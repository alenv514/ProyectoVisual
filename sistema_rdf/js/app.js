/**
 * JAVASCRIPT NATIVO (VANILLA JS)
 * Sistema de Control de Acceso y Asistencia RFID
 * Implementa llamadas con métodos HTTP nativos: GET, POST, PUT, DELETE
 */

// =============================================================================
// REPRODUCCIÓN DE TONOS AUDITIVOS (WEB AUDIO API NATIVA)
// =============================================================================
function reproducirSonido(exito) {
    try {
        const audioCtx = new (window.AudioContext || window.webkitAudioContext)();
        const osc = audioCtx.createOscillator();
        const gain = audioCtx.createGain();
        osc.connect(gain);
        gain.connect(audioCtx.destination);

        if (exito) {
            // Beep agudo de confirmación (acceso permitido)
            osc.frequency.setValueAtTime(1200, audioCtx.currentTime);
            gain.gain.setValueAtTime(0.2, audioCtx.currentTime);
            osc.start();
            osc.stop(audioCtx.currentTime + 0.15);
        } else {
            // Tono grave de alerta (acceso denegado)
            osc.frequency.setValueAtTime(250, audioCtx.currentTime);
            gain.gain.setValueAtTime(0.3, audioCtx.currentTime);
            osc.start();
            osc.stop(audioCtx.currentTime + 0.35);
        }
    } catch (e) {
        // En navegadores con restricciones de audio previo a interacción
    }
}

// =============================================================================
// RELOJ INDEPENDIENTE EN TIEMPO REAL (VIRTUAL Y MONOTÓNICO)
// =============================================================================
let tiempoVirtualMs = null;

function sincronizarRelojServidor() {
    fetch('models/reloj.php', { method: 'GET' })
    .then(res => res.json())
    .then(data => {
        if (data.success && data.timestamp) {
            tiempoVirtualMs = data.timestamp * 1000;
            actualizarDisplayReloj();
        }
    })
    .catch(() => {});
}

function actualizarDisplayReloj() {
    if (tiempoVirtualMs === null) return;
    // Zona horaria fija de Ecuador (UTC-5) para ser inmune a cambios de zona horaria en Windows
    const ecuador = new Date(tiempoVirtualMs - (5 * 3600 * 1000));
    const horas = String(ecuador.getUTCHours()).padStart(2, '0');
    const minutos = String(ecuador.getUTCMinutes()).padStart(2, '0');
    const segundos = String(ecuador.getUTCSeconds()).padStart(2, '0');
    const el = document.getElementById('relojVivo');
    if (el) {
        el.textContent = `Hora: ${horas}:${minutos}:${segundos}`;
    }
}

function iniciarReloj() {
    sincronizarRelojServidor();

    // El segundero avanza 1 segundo cada 1000 ms independientemente del reloj de Windows
    setInterval(() => {
        if (tiempoVirtualMs !== null) {
            tiempoVirtualMs += 1000;
            actualizarDisplayReloj();
        }
    }, 1000);

    // Resincronizar con el backend periódicamente y al reactivar la pestaña/laptop
    setInterval(sincronizarRelojServidor, 30000);
    document.addEventListener('visibilitychange', () => {
        if (!document.hidden) {
            sincronizarRelojServidor();
        }
    });
}

function ajustarHoraSistema(fechaHora) {
    fetch('models/reloj.php?accion=ajustar', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: 'fechaHora=' + encodeURIComponent(fechaHora)
    })
    .then(res => res.json())
    .then(data => {
        if (data.success) {
            alert(data.mensaje);
            sincronizarRelojServidor();
        } else {
            alert(data.errorMsg || data.error || 'Error al ajustar hora.');
        }
    });
}

function sincronizarHoraRealSistema() {
    fetch('models/reloj.php?accion=sincronizar', { method: 'POST' })
    .then(res => res.json())
    .then(data => {
        if (data.success) {
            alert(data.mensaje);
            sincronizarRelojServidor();
        }
    });
}

// =============================================================================
// ESCÁNER Y SIMULADOR RFID (MÉTODO POST NATIVO)
// =============================================================================
let ultimoIdAsistencia = 0;
let puertoSerial = null;
let lectorSerial = null;
let conectandoSerial = false;
let desconexionManualSerial = false;
let timerReconexionSerial = null;
const STORAGE_KEY_ESP_AUTO = 'esp32AutoConectar';
const STORAGE_KEY_ESP_INFO = 'esp32PuertoInfo';

function reflejarMarcacionEnUI(reg, emitirSonido = true) {
    if (!reg) return;

    const tarjeta = document.getElementById('tarjetaGrafica');
    const lblUid = document.getElementById('tarjetaUidTexto');
    const lblNombre = document.getElementById('tarjetaNombreTexto');
    const lblEstado = document.getElementById('resultadoEstado');
    const lblMensaje = document.getElementById('resultadoMensaje');
    const panelResultado = document.getElementById('panelResultadoLectura');

    const permitido = (reg.estado === 'ACCESO PERMITIDO');

    if (emitirSonido) {
        reproducirSonido(permitido);
    }

    if (lblUid) lblUid.textContent = reg.uid || 'DESCONOCIDO';
    if (lblNombre) lblNombre.textContent = reg.nombre || 'NO REGISTRADO';

    if (tarjeta) {
        tarjeta.classList.remove('acceso-permitido', 'acceso-denegado', 'en-espera-admin');
        void tarjeta.offsetWidth;
        if (reg.estado === 'EN ESPERA DE ADMIN') {
            tarjeta.classList.add('en-espera-admin');
        } else {
            tarjeta.style.background = '';
            tarjeta.classList.add(permitido ? 'acceso-permitido' : 'acceso-denegado');
        }
    }

    if (panelResultado) {
        panelResultado.style.display = 'block';
        if (reg.estado === 'EN ESPERA DE ADMIN') {
            panelResultado.className = 'alert alert-naranja';
        } else {
            panelResultado.className = 'alert ' + (permitido ? 'alert-success' : 'alert-danger');
        }
    }

    if (lblEstado) {
        lblEstado.textContent = reg.estado;
        if (reg.estado === 'EN ESPERA DE ADMIN') {
            lblEstado.className = 'badge bg-naranja me-2';
        } else {
            lblEstado.className = 'badge bg-' + (permitido ? 'success' : 'danger') + ' me-2';
        }
    }

    if (lblMensaje) {
        if (reg.estado === 'EN ESPERA DE ADMIN') {
            ultimoRegistroEnEspera = reg;
            const esAdminWeb = panelResultado && panelResultado.getAttribute('data-es-admin') === '1';
            let botonesAdmin = '';
            if (esAdminWeb) {
                botonesAdmin = `<div class="mt-2 d-flex flex-wrap gap-2">` +
                    `<button class="btn btn-sm btn-naranja fw-bold shadow-sm" onclick="abrirCamaraVerificacion(ultimoRegistroEnEspera)"><i class="bi bi-camera-video-fill"></i> 📹 Ver Cámara</button>` +
                    `<button class="btn btn-sm btn-success fw-bold shadow-sm" onclick="decidirReingresoWeb(${reg.id}, 'autorizar')"><i class="bi bi-check-circle-fill"></i> ✔ Autorizar Paso</button>` +
                    `<button class="btn btn-sm btn-danger fw-bold shadow-sm" onclick="decidirReingresoWeb(${reg.id}, 'denegar')"><i class="bi bi-x-circle-fill"></i> ✖ Denegar Paso</button>` +
                    `</div>`;
                setTimeout(() => abrirCamaraVerificacion(reg), 350);
            }
            lblMensaje.innerHTML = `<strong>${escapeHtml(reg.nombre)}</strong> (${escapeHtml(reg.departamento)})<br>` +
                `<span class="badge bg-naranja">3ra Marcación del día</span>: Límite de turnos alcanzado.` +
                botonesAdmin;
        } else if (permitido) {
            lblMensaje.innerHTML = `<strong>${escapeHtml(reg.nombre)}</strong> (${escapeHtml(reg.departamento)})<br>` +
                `Turno: <span class="badge bg-secondary">${escapeHtml(reg.tipo)}</span> | Horario: ${escapeHtml(reg.horario_esperado)}<br>` +
                `Puntualidad: <strong>${escapeHtml(reg.detalle_puntualidad)}</strong> | Multa: <strong>$${parseFloat(reg.multa || 0).toFixed(2)}</strong>`;
        } else {
            lblMensaje.innerHTML = `<strong>ACCESO DENEGADO</strong>: Tarjeta no autorizada o empleado inactivo.`;
        }
    }

    cargarMarcacionesRecientes();
}

function escanearTarjeta(uid) {
    const uidLimpio = String(uid).trim().toUpperCase();
    if (!uidLimpio) {
        alert("Por favor ingrese o escanee un UID de tarjeta.");
        return;
    }

    const lblUid = document.getElementById('tarjetaUidTexto');
    const lblNombre = document.getElementById('tarjetaNombreTexto');
    if (lblUid) lblUid.textContent = uidLimpio;
    if (lblNombre) lblNombre.textContent = "VERIFICANDO...";

    fetch('models/guardar_asistencia.php', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: 'uid=' + encodeURIComponent(uidLimpio)
    })
    .then(response => response.json())
    .then(data => {
        if (!data.success && (data.errorMsg || data.error)) {
            alert(data.errorMsg || data.error);
            return;
        }

        const reg = data.registro;
        ultimoIdAsistencia = Math.max(ultimoIdAsistencia, parseInt(reg.id, 10));
        reflejarMarcacionEnUI(reg, true);

        if (puertoSerial && puertoSerial.writable) {
            responderEsp32(data.accesoPermitido);
        }

        const txtSim = document.getElementById('txtSimularUid');
        if (txtSim) txtSim.value = '';
    })
    .catch(err => {
        console.error("Error en la lectura RFID:", err);
    });
}

function iniciarSincronizacionEnVivo() {
    fetch('models/select_asistencias.php?accion=ultima_marcacion&ultimo_id=0')
    .then(r => r.json())
    .then(res => {
        if (res.success && res.registro) {
            ultimoIdAsistencia = parseInt(res.registro.id, 10);
        }
    })
    .catch(() => {});

    setInterval(() => {
        if (!document.getElementById('tarjetaGrafica')) return;
        fetch(`models/select_asistencias.php?accion=ultima_marcacion&ultimo_id=${ultimoIdAsistencia}`)
        .then(r => r.json())
        .then(res => {
            if (res.success && res.hayNueva && res.registro) {
                const reg = res.registro;
                ultimoIdAsistencia = Math.max(ultimoIdAsistencia, parseInt(reg.id, 10));
                reflejarMarcacionEnUI(reg, true);
            }
        })
        .catch(() => {});
    }, 1000);
}

function guardarPreferenciaSerial(activo, port = null) {
    try {
        localStorage.setItem(STORAGE_KEY_ESP_AUTO, activo ? '1' : '0');
        if (activo && port && typeof port.getInfo === 'function') {
            const info = port.getInfo();
            localStorage.setItem(STORAGE_KEY_ESP_INFO, JSON.stringify({
                usbVendorId: info.usbVendorId ?? null,
                usbProductId: info.usbProductId ?? null
            }));
        } else if (!activo) {
            localStorage.removeItem(STORAGE_KEY_ESP_INFO);
        }
    } catch (e) {}
}

async function buscarPuertoAutorizado() {
    if (!('serial' in navigator)) return null;
    const ports = await navigator.serial.getPorts();
    if (!ports || ports.length === 0) return null;

    try {
        const rawInfo = localStorage.getItem(STORAGE_KEY_ESP_INFO);
        if (rawInfo) {
            const saved = JSON.parse(rawInfo);
            if (saved && saved.usbVendorId != null && saved.usbProductId != null) {
                const match = ports.find(p => {
                    const info = p.getInfo ? p.getInfo() : {};
                    return info.usbVendorId === saved.usbVendorId && info.usbProductId === saved.usbProductId;
                });
                if (match) return match;
            }
        }
    } catch (e) {}

    return ports[0];
}

async function cerrarPuertoSerial() {
    if (lectorSerial) {
        try {
            await lectorSerial.cancel();
        } catch (e) {}
        try {
            lectorSerial.releaseLock();
        } catch (e) {}
        lectorSerial = null;
    }

    const port = puertoSerial;
    puertoSerial = null;
    if (port) {
        try {
            await port.close();
        } catch (e) {}
    }
}

function programarReconexionSerial(delayMs = 1500) {
    clearTimeout(timerReconexionSerial);
    if (desconexionManualSerial) return;
    if (localStorage.getItem(STORAGE_KEY_ESP_AUTO) !== '1') return;

    timerReconexionSerial = setTimeout(async () => {
        if (puertoSerial || conectandoSerial || desconexionManualSerial) return;
        if (localStorage.getItem(STORAGE_KEY_ESP_AUTO) !== '1') return;

        const port = await buscarPuertoAutorizado();
        if (port) {
            const ok = await abrirPuertoSerial(port, { silencioso: true });
            if (!ok && !desconexionManualSerial) {
                programarReconexionSerial(2000);
            }
        } else {
            programarReconexionSerial(2500);
        }
    }, delayMs);
}

async function abrirPuertoSerial(port, opciones = { silencioso: false }) {
    if (!port || conectandoSerial) return false;
    conectandoSerial = true;

    try {
        if (puertoSerial && puertoSerial !== port) {
            await cerrarPuertoSerial();
        }
        if (!port.readable) {
            await port.open({ baudRate: 115200 });
        }
        puertoSerial = port;
        desconexionManualSerial = false;
        guardarPreferenciaSerial(true, port);
        actualizarIndicadorSerial(true, "ESP32 Conectado (USB)", false);
        leerFlujoSerial(port);
        return true;
    } catch (e) {
        if (puertoSerial === port) {
            puertoSerial = null;
        }
        if (!opciones.silencioso) {
            if (e.name === 'NetworkError' || (e.message && e.message.toLowerCase().includes('busy'))) {
                alert("El puerto COM está ocupado por Java u otra app. Como Java está abierto, el sistema web se sincroniza automáticamente en vivo mediante la base de datos.");
            } else if (e.name !== 'NotFoundError') {
                console.error(e);
            }
        }
        return false;
    } finally {
        conectandoSerial = false;
    }
}

async function conectarSerialESP32() {
    if (!('serial' in navigator)) {
        alert("Tu navegador no soporta Web Serial API. Para conexión directa USB usa Google Chrome, Brave o Edge. De todas formas, la sincronización por base de datos está activa.");
        return;
    }

    if (puertoSerial) {
        desconexionManualSerial = true;
        clearTimeout(timerReconexionSerial);
        guardarPreferenciaSerial(false);
        await cerrarPuertoSerial();
        actualizarIndicadorSerial(false, "Lector RFID Sincronizado", false);
        return;
    }

    try {
        clearTimeout(timerReconexionSerial);
        desconexionManualSerial = false;
        const port = await navigator.serial.requestPort();
        await abrirPuertoSerial(port, { silencioso: false });
    } catch (e) {
        if (e.name !== 'NotFoundError') {
            console.error(e);
        }
    }
}

async function leerFlujoSerial(port) {
    const decoder = new TextDecoder('utf-8', { fatal: false });
    let acumulador = "";

    try {
        while (puertoSerial === port && port.readable && !desconexionManualSerial) {
            const reader = port.readable.getReader();
            lectorSerial = reader;
            try {
                while (true) {
                    const { value, done } = await reader.read();
                    if (done) break;
                    if (value) {
                        acumulador += decoder.decode(value, { stream: true });
                        if (acumulador.length > 4096) {
                            acumulador = acumulador.slice(-1024);
                        }
                        const lineas = acumulador.split(/\r?\n/);
                        acumulador = lineas.pop() || "";
                        for (const linea of lineas) {
                            const l = linea.trim();
                            if (l.startsWith("UID:")) {
                                const uid = l.substring(4).trim();
                                if (uid) {
                                    escanearTarjeta(uid);
                                }
                            }
                        }
                    }
                }
            } finally {
                try {
                    reader.releaseLock();
                } catch (e) {}
                if (lectorSerial === reader) {
                    lectorSerial = null;
                }
            }
            break;
        }
    } catch (err) {
        console.warn("Interrupción en lectura serial ESP32:", err);
    } finally {
        if (puertoSerial === port) {
            await cerrarPuertoSerial();
            if (!desconexionManualSerial && localStorage.getItem(STORAGE_KEY_ESP_AUTO) === '1') {
                actualizarIndicadorSerial(false, "Reconectando ESP32...", true);
                programarReconexionSerial(1000);
            } else {
                actualizarIndicadorSerial(false, "Lector RFID Sincronizado", false);
            }
        }
    }
}

async function responderEsp32(permitido) {
    if (!puertoSerial || !puertoSerial.writable) return;
    let writer = null;
    try {
        writer = puertoSerial.writable.getWriter();
        const texto = (permitido ? "ACCESO_PERMITIDO\n" : "ACCESO_DENEGADO\n");
        const encoder = new TextEncoder();
        await writer.write(encoder.encode(texto));
    } catch (e) {
        console.warn("No se pudo enviar respuesta al ESP32:", e);
    } finally {
        if (writer) {
            try {
                writer.releaseLock();
            } catch (e) {}
        }
    }
}

function iniciarAutoReconexionSerial() {
    if (!('serial' in navigator)) return;

    navigator.serial.addEventListener('connect', async () => {
        if (desconexionManualSerial || puertoSerial || conectandoSerial) return;
        if (localStorage.getItem(STORAGE_KEY_ESP_AUTO) !== '1') return;

        clearTimeout(timerReconexionSerial);
        const port = await buscarPuertoAutorizado();
        if (port) {
            const ok = await abrirPuertoSerial(port, { silencioso: true });
            if (!ok) programarReconexionSerial(1200);
        }
    });

    navigator.serial.addEventListener('disconnect', async (event) => {
        if (puertoSerial && (event.target === puertoSerial || !event.target)) {
            await cerrarPuertoSerial();
            if (!desconexionManualSerial && localStorage.getItem(STORAGE_KEY_ESP_AUTO) === '1') {
                actualizarIndicadorSerial(false, "Reconectando ESP32...", true);
                programarReconexionSerial(1500);
            } else {
                actualizarIndicadorSerial(false, "Lector RFID Sincronizado", false);
            }
        }
    });

    window.addEventListener('pagehide', () => {
        desconexionManualSerial = true;
        clearTimeout(timerReconexionSerial);
        cerrarPuertoSerial();
    });

    window.addEventListener('pageshow', (event) => {
        if (event.persisted && localStorage.getItem(STORAGE_KEY_ESP_AUTO) === '1' && !puertoSerial) {
            desconexionManualSerial = false;
            actualizarIndicadorSerial(false, "Reconectando ESP32...", true);
            programarReconexionSerial(300);
        }
    });

    if (localStorage.getItem(STORAGE_KEY_ESP_AUTO) === '1') {
        desconexionManualSerial = false;
        actualizarIndicadorSerial(false, "Reconectando ESP32...", true);
        programarReconexionSerial(250);
    }
}

function actualizarIndicadorSerial(conectado, texto, reconectando = false) {
    const elTexto = document.getElementById('textoLector');
    const elDot = document.getElementById('dotLector');
    const btn = document.getElementById('btnConectarSerial');
    if (elTexto) elTexto.textContent = texto;
    if (elDot) {
        elDot.className = reconectando
            ? "spinner-grow spinner-grow-sm text-warning"
            : "spinner-grow spinner-grow-sm text-success";
    }
    if (btn) {
        if (conectado) {
            btn.className = "btn btn-sm btn-success fw-bold";
            btn.innerHTML = `<i class="bi bi-check-circle-fill"></i> Conectado`;
            btn.title = "ESP32 conectado por USB (clic para desconectar)";
        } else if (reconectando) {
            btn.className = "btn btn-sm btn-warning fw-semibold text-dark shadow-sm";
            btn.innerHTML = `<i class="bi bi-arrow-repeat"></i> Reconectando...`;
            btn.title = "Intentando reconectar ESP32 automáticamente";
        } else {
            btn.className = "btn btn-sm btn-outline-light fw-semibold shadow-sm";
            btn.innerHTML = `<i class="bi bi-usb-symbol"></i> Conectar ESP32`;
            btn.title = "Conectar lector ESP32 directamente por USB si Java no está abierto";
        }
    }
}

let bufferRfid = '';
let timerRfid = null;
window.addEventListener('keydown', function(e) {
    if (e.target && (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA')) {
        return;
    }
    if (e.key === 'Enter') {
        if (bufferRfid.trim().length >= 4) {
            escanearTarjeta(bufferRfid.trim());
        }
        bufferRfid = '';
        return;
    }
    if (e.key && e.key.length === 1) {
        bufferRfid += e.key;
        clearTimeout(timerRfid);
        timerRfid = setTimeout(() => { bufferRfid = ''; }, 250);
    }
});

let streamCamaraActivo = null;
let ultimoRegistroEnEspera = null;

function abrirCamaraVerificacion(reg) {
    if (!reg) reg = ultimoRegistroEnEspera;
    if (!reg) return;
    ultimoRegistroEnEspera = reg;

    const modalEl = document.getElementById('modalCamaraAdmin');
    if (!modalEl) return;

    const subtitulo = document.getElementById('camaraSubtitulo');
    if (subtitulo) {
        subtitulo.textContent = `Persona: ${reg.nombre || '---'} | UID: ${reg.uid || '---'} | Departamento: ${reg.departamento || '-'}`;
    }

    // Cargar datos y fotografía registrada de la persona
    const imgFoto = document.getElementById('camaraFotoRegistrada');
    if (imgFoto) {
        const uidLimpio = (reg.uid || '').trim();
        const rutaFoto = (reg.foto && String(reg.foto).trim() !== '')
            ? String(reg.foto).trim()
            : 'fotos_empleados/' + encodeURIComponent(uidLimpio) + '.jpg';
        imgFoto.src = rutaFoto + '?v=' + Date.now();
        imgFoto.onerror = function() {
            this.onerror = null;
            this.src = 'https://ui-avatars.com/api/?name=' + encodeURIComponent(reg.nombre || 'Usuario') + '&size=200&background=495057&color=ffffff&bold=true';
        };
    }

    const lblNom = document.getElementById('camaraNombreEmp');
    if (lblNom) lblNom.textContent = reg.nombre || '---';

    const lblUid = document.getElementById('camaraUidEmp');
    if (lblUid) lblUid.textContent = reg.uid || '---';

    const lblDep = document.getElementById('camaraDeptoEmp');
    if (lblDep) lblDep.textContent = reg.departamento || '-';

    const btnAutorizar = document.getElementById('btnCamaraAutorizar');
    const btnDenegar = document.getElementById('btnCamaraDenegar');

    if (btnAutorizar) {
        btnAutorizar.onclick = () => decidirReingresoWeb(reg.id, 'autorizar');
    }

    if (btnDenegar) {
        btnDenegar.onclick = () => decidirReingresoWeb(reg.id, 'denegar');
    }

    const video = document.getElementById('videoCamaraEnVivo');
    const errorMsg = document.getElementById('camaraErrorMensaje');
    const badgeEstado = document.getElementById('camaraBadgeEstado');

    if (errorMsg) errorMsg.style.display = 'none';
    if (badgeEstado) {
        badgeEstado.className = 'badge bg-naranja px-3 py-2 fw-semibold';
        badgeEstado.textContent = '● Iniciando cámara...';
    }

    const modal = bootstrap.Modal.getOrCreateInstance(modalEl);
    modal.show();

    if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
        navigator.mediaDevices.getUserMedia({ video: { width: { ideal: 1280 }, height: { ideal: 720 } } })
            .then(stream => {
                streamCamaraActivo = stream;
                if (video) {
                    video.srcObject = stream;
                    video.play();
                }
                if (badgeEstado) {
                    badgeEstado.className = 'badge bg-success px-3 py-2 fw-semibold';
                    badgeEstado.textContent = '● EN VIVO';
                }
            })
            .catch(err => {
                console.warn("Cámara no disponible:", err);
                if (errorMsg) errorMsg.style.display = 'block';
                if (badgeEstado) {
                    badgeEstado.className = 'badge bg-danger px-3 py-2 fw-semibold';
                    badgeEstado.textContent = '● Cámara no disponible';
                }
            });
    } else {
        if (errorMsg) errorMsg.style.display = 'block';
    }
}

function cerrarCamaraVerificacion() {
    if (streamCamaraActivo) {
        streamCamaraActivo.getTracks().forEach(track => track.stop());
        streamCamaraActivo = null;
    }
    const video = document.getElementById('videoCamaraEnVivo');
    if (video) {
        video.srcObject = null;
    }
    const modalEl = document.getElementById('modalCamaraAdmin');
    if (modalEl) {
        const modal = bootstrap.Modal.getInstance(modalEl);
        if (modal) modal.hide();
    }
}

function decidirReingresoWeb(id, decision) {
    cerrarCamaraVerificacion();

    fetch('models/actualizar_asistencia.php?id=' + encodeURIComponent(id), {
        method: 'PUT',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: 'id=' + encodeURIComponent(id) + '&decision=' + encodeURIComponent(decision)
    })
    .then(res => res.json())
    .then(data => {
        if (data.success) {
            const fueAutorizado = (decision === 'autorizar');
            reproducirSonido(fueAutorizado);

            const tarjeta = document.getElementById('tarjetaGrafica');
            if (tarjeta) {
                tarjeta.classList.remove('acceso-permitido', 'acceso-denegado', 'en-espera-admin');
                tarjeta.style.background = '';
                tarjeta.classList.add(fueAutorizado ? 'acceso-permitido' : 'acceso-denegado');
            }

            const panelResultado = document.getElementById('panelResultadoLectura');
            if (panelResultado) {
                panelResultado.className = 'alert ' + (fueAutorizado ? 'alert-success' : 'alert-danger');
                document.getElementById('resultadoEstado').className = 'badge bg-' + (fueAutorizado ? 'success' : 'danger') + ' me-2';
                document.getElementById('resultadoEstado').textContent = fueAutorizado ? 'ACCESO PERMITIDO' : 'ACCESO DENEGADO';
                document.getElementById('resultadoMensaje').innerHTML = fueAutorizado
                    ? '<strong>✔ REINGRESO AUTORIZADO POR ADMINISTRADOR</strong><br><small class="text-success fw-semibold">Paso concedido con éxito.</small>'
                    : '<strong>✖ REINGRESO DENEGADO POR ADMINISTRADOR</strong><br><small class="text-danger fw-semibold">Acceso bloqueado.</small>';
            }
            cargarMarcacionesRecientes();
        } else {
            alert(data.errorMsg || data.error || 'Error al procesar decisión.');
        }
    });
}

function cargarMarcacionesRecientes() {
    const tabla = document.getElementById('tablaMarcacionesRecientes');
    if (!tabla) return;

    fetch('models/select_asistencias.php?accion=listar&fecha=hoy', { method: 'GET' })
    .then(res => res.json())
    .then(res => {
        if (!res.success) return;
        const lista = res.data.slice(0, 5); // Últimas 5 del día de hoy
        let html = '';
        if (lista.length === 0) {
            html = `<tr><td colspan="6" class="text-center text-muted py-3">No hay marcaciones registradas el día de hoy.</td></tr>`;
        }
        lista.forEach(r => {
            const esEnEspera = (r.estado === 'EN ESPERA DE ADMIN');
            const esDenegado = r.estado.includes('DENEGADO');
            const multa = parseFloat(r.multa);
            let badgeClass = 'bg-success';
            if (esEnEspera) badgeClass = 'bg-naranja';
            else if (esDenegado) badgeClass = 'bg-danger';
            else if (multa > 0) badgeClass = 'bg-danger';

            let accionExtra = '';
            const panelResultado = document.getElementById('panelResultadoLectura');
            const esAdminWeb = panelResultado && panelResultado.getAttribute('data-es-admin') === '1';
            if (esEnEspera && esAdminWeb) {
                accionExtra = ` <button class="btn btn-xs btn-outline-dark py-0 px-1 fw-bold ms-1" onclick="abrirCamaraVerificacion(${JSON.stringify(r).replace(/"/g, '&quot;')})" title="Ver cámara y autorizar"><i class="bi bi-camera-video-fill text-naranja"></i> Cámara</button>`;
            }

            html += `<tr>
                <td><small class="text-muted">${r.fecha_hora}</small></td>
                <td><code class="text-dark">${r.uid}</code></td>
                <td><strong>${r.nombre}</strong></td>
                <td><span class="badge ${badgeClass}">${r.tipo}</span>${accionExtra}</td>
                <td><span class="${multa > 0 ? 'text-danger fw-semibold' : 'text-muted'}">${r.detalle_puntualidad}</span></td>
                <td class="text-end font-monospace ${multa > 0 ? 'text-danger fw-bold' : 'text-muted'}">$${multa.toFixed(2)}</td>
            </tr>`;
        });
        tabla.innerHTML = html;
    });
}

function limpiarIngresosHoy() {
    if (!confirm('¿Borrar todas las marcaciones del día de hoy para reiniciar el flujo de pruebas?')) {
        return;
    }

    fetch('models/delete_asistencia.php?accion=limpiar_hoy', {
        method: 'DELETE'
    })
    .then(res => res.json())
    .then(data => {
        if (!data.success) {
            alert(data.errorMsg || data.error || 'No se pudieron borrar las marcaciones de hoy.');
            return;
        }

        ultimoRegistroEnEspera = null;

        const tarjeta = document.getElementById('tarjetaGrafica');
        if (tarjeta) {
            tarjeta.classList.remove('acceso-permitido', 'acceso-denegado', 'en-espera-admin');
            tarjeta.style.background = '';
        }
        const lblUid = document.getElementById('tarjetaUidTexto');
        const lblNombre = document.getElementById('tarjetaNombreTexto');
        if (lblUid) lblUid.textContent = 'APROXIME TARJETA';
        if (lblNombre) lblNombre.textContent = 'SISTEMA LISTO';

        const panelResultado = document.getElementById('panelResultadoLectura');
        if (panelResultado) {
            panelResultado.style.display = 'none';
        }

        cargarMarcacionesRecientes();
        alert(data.mensaje);
    })
    .catch(err => {
        console.error('Error al limpiar marcaciones de hoy:', err);
    });
}

// =============================================================================
// GESTIÓN DE PERSONAL (MÉTODOS GET, POST, PUT, DELETE)
// =============================================================================
function cargarUsuarios(buscar = '') {
    const tabla = document.getElementById('tablaUsuariosCuerpo');
    if (!tabla) return;

    fetch('models/select.php?buscar=' + encodeURIComponent(buscar), {
        method: 'GET'
    })
    .then(res => res.json())
    .then(res => {
        if (!res.success) return;
        let html = '';
        if (res.data.length === 0) {
            html = `<tr><td colspan="9" class="text-center text-muted py-3">No hay empleados registrados.</td></tr>`;
        } else {
            const cacheBust = Date.now();
            res.data.forEach(u => {
                const activoBadge = (parseInt(u.activo) === 1)
                    ? `<span class="badge bg-success">Activo</span>`
                    : `<span class="badge bg-secondary">Inactivo</span>`;
                const fotoHtml = u.foto
                    ? `<img src="${escapeHtml(u.foto)}?v=${cacheBust}" alt="${escapeHtml(u.nombre)}" class="rounded-circle border" style="width: 36px; height: 36px; object-fit: cover;" onerror="this.outerHTML='<i class=\\'bi bi-person-circle fs-4 text-secondary\\'></i>'">`
                    : `<i class="bi bi-person-circle fs-4 text-secondary"></i>`;

                html += `<tr>
                    <td>${u.id}</td>
                    <td class="text-center">${fotoHtml}</td>
                    <td><code>${u.uid}</code></td>
                    <td><strong>${u.nombre}</strong></td>
                    <td>${u.cedula || '-'}</td>
                    <td>${u.departamento || '-'}</td>
                    <td>${u.cargo || '-'}</td>
                    <td>${activoBadge}</td>
                    <td class="text-end">
                        <button class="btn btn-sm btn-outline-primary me-1" onclick="seleccionarUsuario(${u.id})">Editar</button>
                        <button class="btn btn-sm btn-outline-danger" onclick="eliminarUsuario(${u.id}, '${escapeHtml(u.nombre)}')">Eliminar</button>
                    </td>
                </tr>`;
            });
        }
        tabla.innerHTML = html;
    })
    .catch(err => console.error("Error al cargar usuarios:", err));
}

function previsualizarFotoUsuario(input) {
    const preview = document.getElementById('previewFotoUsuario');
    const btnQuitar = document.getElementById('btnQuitarFoto');
    const flagQuitar = document.getElementById('quitarFotoFlag');

    if (input && input.files && input.files[0]) {
        const reader = new FileReader();
        reader.onload = function(e) {
            if (preview) {
                preview.src = e.target.result;
                preview.style.display = 'block';
            }
            if (btnQuitar) btnQuitar.style.display = 'inline-block';
            if (flagQuitar) flagQuitar.value = '0';
        };
        reader.readAsDataURL(input.files[0]);
    }
}

function quitarFotoFormulario() {
    const inputFoto = document.getElementById('txtRegFoto');
    const preview = document.getElementById('previewFotoUsuario');
    const btnQuitar = document.getElementById('btnQuitarFoto');
    const fotoActual = document.getElementById('fotoActual');
    const flagQuitar = document.getElementById('quitarFotoFlag');

    if (inputFoto) inputFoto.value = '';
    if (fotoActual) fotoActual.value = '';
    if (flagQuitar) flagQuitar.value = '1';
    if (preview) {
        preview.src = '';
        preview.style.display = 'none';
    }
    if (btnQuitar) btnQuitar.style.display = 'none';
}

function seleccionarUsuario(id) {
    fetch('models/select.php?id=' + id, { method: 'GET' })
    .then(res => res.json())
    .then(res => {
        if (!res.success) return;
        const u = res.data;
        document.getElementById('usuarioId').value = u.id;
        document.getElementById('txtRegUid').value = u.uid;
        document.getElementById('txtRegNombre').value = u.nombre;
        document.getElementById('txtRegCedula').value = u.cedula;
        document.getElementById('txtRegDepto').value = u.departamento;
        document.getElementById('txtRegCargo').value = u.cargo;
        document.getElementById('chkRegActivo').checked = (parseInt(u.activo) === 1);

        const inputFoto = document.getElementById('txtRegFoto');
        const preview = document.getElementById('previewFotoUsuario');
        const btnQuitar = document.getElementById('btnQuitarFoto');
        const fotoActual = document.getElementById('fotoActual');
        const flagQuitar = document.getElementById('quitarFotoFlag');

        if (inputFoto) inputFoto.value = '';
        if (fotoActual) fotoActual.value = u.foto || '';
        if (flagQuitar) flagQuitar.value = '0';

        if (u.foto && preview) {
            preview.src = u.foto + '?v=' + Date.now();
            preview.style.display = 'block';
            if (btnQuitar) btnQuitar.style.display = 'inline-block';
        } else if (preview) {
            preview.src = '';
            preview.style.display = 'none';
            if (btnQuitar) btnQuitar.style.display = 'none';
        }

        document.getElementById('btnGuardarUsuario').textContent = "Actualizar Empleado";
        document.getElementById('btnGuardarUsuario').className = "btn btn-warning fw-bold";
        document.getElementById('btnCancelarEdicion').style.display = "inline-block";
    });
}

function guardarUsuario() {
    const id = parseInt(document.getElementById('usuarioId').value) || 0;
    const uid = document.getElementById('txtRegUid').value.trim();
    const nombre = document.getElementById('txtRegNombre').value.trim();
    const cedula = document.getElementById('txtRegCedula').value.trim();
    const departamento = document.getElementById('txtRegDepto').value.trim();
    const cargo = document.getElementById('txtRegCargo').value.trim();
    const activo = document.getElementById('chkRegActivo').checked ? 1 : 0;
    const fotoActual = document.getElementById('fotoActual') ? document.getElementById('fotoActual').value : '';
    const quitarFoto = document.getElementById('quitarFotoFlag') ? document.getElementById('quitarFotoFlag').value : '0';

    if (!uid || !nombre) {
        alert("El UID de la tarjeta y el Nombre Completo son obligatorios.");
        return;
    }

    const formData = new FormData();
    formData.append('id', id);
    formData.append('uid', uid);
    formData.append('nombre', nombre);
    formData.append('cedula', cedula);
    formData.append('departamento', departamento);
    formData.append('cargo', cargo);
    formData.append('activo', activo);
    formData.append('foto_actual', fotoActual);
    formData.append('quitar_foto', quitarFoto);

    const inputFoto = document.getElementById('txtRegFoto');
    if (inputFoto && inputFoto.files && inputFoto.files[0]) {
        formData.append('foto', inputFoto.files[0]);
    }

    if (id > 0) {
        fetch('models/actualizar.php?id=' + encodeURIComponent(id), {
            method: 'POST',
            body: formData
        })
        .then(res => res.json())
        .then(res => {
            if (res.success) {
                alert(res.mensaje);
                limpiarFormUsuario();
                cargarUsuarios();
            } else {
                alert("Error: " + (res.errorMsg || res.error));
            }
        });
    } else {
        fetch('models/guardar.php', {
            method: 'POST',
            body: formData
        })
        .then(res => res.json())
        .then(res => {
            if (res.success) {
                alert(res.mensaje);
                limpiarFormUsuario();
                cargarUsuarios();
            } else {
                alert("Error: " + (res.errorMsg || res.error));
            }
        });
    }
}

function eliminarUsuario(id, nombre) {
    if (!confirm(`¿Está seguro de eliminar al empleado "${nombre}"?\n(Esta acción es permanente en la base de datos).`)) {
        return;
    }

    // ELIMINAR (MÉTODO HTTP DELETE NATIVO)
    fetch('models/delete.php?id=' + encodeURIComponent(id), {
        method: 'DELETE'
    })
    .then(res => res.json())
    .then(res => {
        if (res.success) {
            alert(res.mensaje);
            limpiarFormUsuario();
            cargarUsuarios();
        } else {
            alert("Error: " + (res.errorMsg || res.error));
        }
    });
}

function limpiarFormUsuario() {
    document.getElementById('usuarioId').value = 0;
    document.getElementById('txtRegUid').value = '';
    document.getElementById('txtRegNombre').value = '';
    document.getElementById('txtRegCedula').value = '';
    document.getElementById('txtRegDepto').value = '';
    document.getElementById('txtRegCargo').value = '';
    document.getElementById('chkRegActivo').checked = true;

    const inputFoto = document.getElementById('txtRegFoto');
    const preview = document.getElementById('previewFotoUsuario');
    const btnQuitar = document.getElementById('btnQuitarFoto');
    const fotoActual = document.getElementById('fotoActual');
    const flagQuitar = document.getElementById('quitarFotoFlag');

    if (inputFoto) inputFoto.value = '';
    if (fotoActual) fotoActual.value = '';
    if (flagQuitar) flagQuitar.value = '0';
    if (preview) {
        preview.src = '';
        preview.style.display = 'none';
    }
    if (btnQuitar) btnQuitar.style.display = 'none';

    const btn = document.getElementById('btnGuardarUsuario');
    if (btn) {
        btn.textContent = "Guardar Empleado";
        btn.className = "btn btn-guardar";
    }
    const btnCancel = document.getElementById('btnCancelarEdicion');
    if (btnCancel) btnCancel.style.display = "none";
}

// =============================================================================
// DETALLE DE LA RELACIÓN 1 A MUCHOS (MODAL DE ASISTENCIAS POR USUARIO)
// =============================================================================
function verAsistenciasUsuario(usuarioId, nombre) {
    const modalEl = document.getElementById('modalAsistenciasUsuario');
    if (!modalEl) return;

    document.getElementById('modalUsuarioNombre').textContent = nombre;
    const cuerpoTabla = document.getElementById('modalTablaAsistenciasCuerpo');
    cuerpoTabla.innerHTML = '<tr><td colspan="6" class="text-center text-muted">Cargando marcaciones...</td></tr>';

    const bsModal = new bootstrap.Modal(modalEl);
    bsModal.show();

    fetch('models/select.php?asistencias_usuario=' + encodeURIComponent(usuarioId), { method: 'GET' })
    .then(res => res.json())
    .then(res => {
        if (!res.success || res.asistencias.length === 0) {
            cuerpoTabla.innerHTML = '<tr><td colspan="6" class="text-center text-muted py-3">Este empleado aún no registra marcaciones de asistencia.</td></tr>';
            return;
        }

        let html = '';
        res.asistencias.forEach(a => {
            const multa = parseFloat(a.multa);
            const esDenegado = a.estado.includes('DENEGADO');
            const esEnEspera = (a.estado === 'EN ESPERA DE ADMIN');
            const rowClass = (esDenegado || multa > 0) ? 'fila-infraccion' : '';

            let bClass = 'bg-success';
            if (esEnEspera) bClass = 'bg-naranja';
            else if (esDenegado || multa > 0) bClass = 'bg-danger';

            html += `<tr class="${rowClass}">
                <td>${a.id}</td>
                <td>${a.fecha_hora}</td>
                <td><span class="badge ${bClass}">${a.tipo}</span></td>
                <td>${a.horario_esperado}</td>
                <td>${a.detalle_puntualidad}</td>
                <td class="text-end font-monospace">$${multa.toFixed(2)}</td>
            </tr>`;
        });
        cuerpoTabla.innerHTML = html;
    });
}

// =============================================================================
// HISTORIAL Y REPORTES CON FILTROS POR TIPO Y ESTADO (MÉTODO GET NATIVO)
// =============================================================================
function cargarHistorial() {
    const tabla = document.getElementById('tablaHistorialCuerpo');
    if (!tabla) return;

    const texto  = document.getElementById('txtBuscarHistorial') ? document.getElementById('txtBuscarHistorial').value.trim() : '';
    const tipo   = document.getElementById('cbFiltroTipo') ? document.getElementById('cbFiltroTipo').value : 'TODOS';
    const estado = document.getElementById('cbFiltroEstado') ? document.getElementById('cbFiltroEstado').value : 'TODOS';

    const url = `models/select_asistencias.php?buscar=${encodeURIComponent(texto)}&tipo=${encodeURIComponent(tipo)}&estado=${encodeURIComponent(estado)}`;

    fetch(url, { method: 'GET' })
    .then(res => res.json())
    .then(res => {
        if (!res.success) return;
        let html = '';

        if (res.data.length === 0) {
            html = `<tr><td colspan="9" class="text-center text-muted py-4">No se encontraron marcaciones con los filtros seleccionados.</td></tr>`;
        } else {
            res.data.forEach(r => {
                const multa = parseFloat(r.multa);
                const esDenegado = r.estado.includes('DENEGADO');
                const esEnEspera = (r.estado === 'EN ESPERA DE ADMIN');
                const rowClass = (esDenegado || multa > 0) ? 'fila-infraccion' : '';

                let bClass = 'bg-success';
                if (esEnEspera) bClass = 'bg-naranja';
                else if (esDenegado || multa > 0) bClass = 'bg-danger';

                html += `<tr class="${rowClass}">
                    <td>${r.id}</td>
                    <td>${r.fecha_hora}</td>
                    <td><code class="text-dark">${r.uid}</code></td>
                    <td><strong>${r.nombre}</strong></td>
                    <td>${r.departamento}</td>
                    <td><span class="badge ${bClass}">${r.tipo}</span></td>
                    <td>${r.horario_esperado}</td>
                    <td>${r.detalle_puntualidad}</td>
                    <td class="text-end font-monospace">$${multa.toFixed(2)}</td>
                </tr>`;
            });
        }
        tabla.innerHTML = html;
    });
}

function limpiarFiltrosHistorial() {
    if (document.getElementById('txtBuscarHistorial')) document.getElementById('txtBuscarHistorial').value = '';
    if (document.getElementById('cbFiltroTipo')) document.getElementById('cbFiltroTipo').selectedIndex = 0;
    if (document.getElementById('cbFiltroEstado')) document.getElementById('cbFiltroEstado').selectedIndex = 0;
    cargarHistorial();
}

function generarReportePDF() {
    const texto  = document.getElementById('txtBuscarHistorial') ? document.getElementById('txtBuscarHistorial').value.trim() : '';
    const tipo   = document.getElementById('cbFiltroTipo') ? document.getElementById('cbFiltroTipo').value : 'TODOS';
    const estado = document.getElementById('cbFiltroEstado') ? document.getElementById('cbFiltroEstado').value : 'TODOS';

    const url = `reportes.php?buscar=${encodeURIComponent(texto)}&tipo=${encodeURIComponent(tipo)}&estado=${encodeURIComponent(estado)}`;
    window.open(url, '_blank');
}

// =============================================================================
// AUTENTICACIÓN ADMINISTRADOR (MÉTODO POST NATIVO)
// =============================================================================
function loginAdmin() {
    const usuario = document.getElementById('adminUsuario').value.trim();
    const password = document.getElementById('adminPassword').value.trim();

    if (!usuario || !password) {
        alert("Ingrese usuario y contraseña.");
        return;
    }

    fetch('models/auth.php?accion=login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: `usuario=${encodeURIComponent(usuario)}&password=${encodeURIComponent(password)}`
    })
    .then(res => res.json())
    .then(res => {
        if (res.success) {
            // Recargar para habilitar las pestañas protegidas
            window.location.reload();
        } else {
            alert(res.errorMsg || res.error || "Error de autenticación.");
        }
    });
}

function logoutAdmin() {
    fetch('models/auth.php?accion=logout', { method: 'POST' })
    .then(res => res.json())
    .then(res => {
        window.location.href = 'index.php?accion=escaner';
    });
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/[&<>"']/g, function(m) {
        return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' }[m];
    });
}

// Inicialización de componentes al cargar la ventana
document.addEventListener('DOMContentLoaded', function() {
    iniciarReloj();
    iniciarSincronizacionEnVivo();
    iniciarAutoReconexionSerial();

    if (document.getElementById('tablaMarcacionesRecientes')) {
        cargarMarcacionesRecientes();
    }
    if (document.getElementById('tablaUsuariosCuerpo')) {
        cargarUsuarios();
    }
    if (document.getElementById('tablaHistorialCuerpo')) {
        cargarHistorial();
    }

    // Permitir presionar Enter en campos de búsqueda
    const txtBuscarUser = document.getElementById('txtBuscarUsuario');
    if (txtBuscarUser) {
        txtBuscarUser.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') cargarUsuarios(this.value);
        });
    }

    const txtBuscarHist = document.getElementById('txtBuscarHistorial');
    if (txtBuscarHist) {
        txtBuscarHist.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') cargarHistorial();
        });
    }

    const txtSim = document.getElementById('txtSimularUid');
    if (txtSim) {
        txtSim.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') escanearTarjeta(this.value);
        });
    }
});
