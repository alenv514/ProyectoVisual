<?php
/**
 * Vista: Control de Acceso y Escáner RFID (Vista Principal Pública)
 */
include_once __DIR__ . "/../models/auth.php";
$esAdmin = estaAutenticadoAdmin();
?>

<div class="row">
    <!-- Panel Izquierdo: Lector Visual RFID -->
    <div class="col-lg-5 mb-4">
        <div class="card shadow-sm h-100">
            <div class="card-header py-3">
                <h5 class="card-title mb-0 fw-bold text-dark">
                    <i class="bi bi-broadcast"></i> Escáner de Tarjetas RFID
                </h5>
            </div>
            <div class="card-body text-center d-flex flex-column justify-content-center bg-white">
                <p class="text-muted small mb-3">
                    Aproxime la tarjeta o llavero RFID al lector USB/ESP32 o utilice el simulador interactivo inferior.
                </p>

                <!-- Tarjeta RFID Gráfica Interactiva -->
                <div class="tarjeta-rfid-contenedor">
                    <div class="tarjeta-rfid" id="tarjetaGrafica">
                        <div class="tarjeta-chip"></div>
                        <div class="tarjeta-rfid-logo">
                            <i class="bi bi-wifi"></i>
                        </div>
                        <div class="small text-uppercase opacity-75">Control de Acceso RFID</div>
                        <div class="tarjeta-uid" id="tarjetaUidTexto">APROXIME TARJETA</div>
                        <div class="tarjeta-nombre" id="tarjetaNombreTexto">SISTEMA LISTO</div>
                    </div>
                </div>

                <!-- Panel de Resultado Dinámico -->
                <div id="panelResultadoLectura" class="alert alert-secondary mt-3 text-start" data-es-admin="<?php echo $esAdmin ? '1' : '0'; ?>" style="display: none;">
                    <div class="d-flex align-items-center mb-1">
                        <span id="resultadoEstado" class="badge bg-secondary me-2">ESTADO</span>
                        <span class="small text-muted">Resultado de la marcación</span>
                    </div>
                    <div id="resultadoMensaje" class="small"></div>
                </div>

                <div class="mt-4 pt-3 border-top text-start">
                    <label class="form-label fw-bold small text-muted">
                        <i class="bi bi-cpu"></i> Simulador y Prueba Rápida de Tarjetas
                    </label>
                    <div class="input-group mb-2">
                        <input type="text" id="txtSimularUid" class="form-control font-monospace" placeholder="Ej: C31CF528, 5555, 5, 7, 8" autofocus>
                        <button class="btn btn-dark fw-semibold" type="button" onclick="escanearTarjeta(document.getElementById('txtSimularUid').value)">
                            Simular Lectura
                        </button>
                    </div>
                    <div class="d-flex flex-wrap gap-1">
                        <button type="button" class="btn btn-sm btn-outline-secondary" onclick="escanearTarjeta('C31CF528')">Alen (C31CF528)</button>
                        <button type="button" class="btn btn-sm btn-outline-secondary" onclick="escanearTarjeta('8')">Jose (8)</button>
                        <button type="button" class="btn btn-sm btn-outline-secondary" onclick="escanearTarjeta('5555')">5555</button>
                        <button type="button" class="btn btn-sm btn-outline-secondary" onclick="escanearTarjeta('9999')">Inválida (9999)</button>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- Panel Derecho: Marcaciones Recientes y Horarios -->
    <div class="col-lg-7 mb-4">
        <div class="card shadow-sm mb-4">
            <div class="card-header py-3 d-flex justify-content-between align-items-center">
                <h5 class="card-title mb-0 fw-bold text-dark">
                    <i class="bi bi-clock-history"></i> Marcaciones Recientes en Tiempo Real
                </h5>
                <div class="d-flex align-items-center gap-1">
                    <?php if ($esAdmin): ?>
                    <button type="button" class="btn btn-sm btn-outline-secondary text-muted border-0" onclick="limpiarIngresosHoy()" title="Borrar marcaciones de hoy para reiniciar pruebas">
                        <i class="bi bi-eraser"></i> Limpiar hoy
                    </button>
                    <?php endif; ?>
                    <button class="btn btn-sm btn-outline-secondary" onclick="cargarMarcacionesRecientes()">
                        <i class="bi bi-arrow-clockwise"></i> Actualizar
                    </button>
                </div>
            </div>
            <div class="card-body p-0 bg-white">
                <div class="table-responsive">
                    <table class="table table-hover align-middle mb-0">
                        <thead class="table-light">
                            <tr>
                                <th>Fecha y Hora</th>
                                <th>UID</th>
                                <th>Empleado</th>
                                <th>Turno / Tipo</th>
                                <th>Puntualidad</th>
                                <th class="text-end">Multa</th>
                            </tr>
                        </thead>
                        <tbody id="tablaMarcacionesRecientes">
                            <tr>
                                <td colspan="6" class="text-center text-muted py-3">Cargando marcaciones recientes...</td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>

        <!-- Tarjeta Informativa de Turnos y Políticas de Puntualidad -->
        <div class="card shadow-sm bg-light">
            <div class="card-body py-3">
                <h6 class="fw-bold text-dark mb-2">
                    <i class="bi bi-info-circle-fill"></i> Políticas Universitarias de Turnos y Puntualidad
                </h6>
                <div class="row g-2 small text-muted">
                    <div class="col-md-6">
                        <strong>Turno Mañana:</strong> 07:00 AM a 01:00 PM (13:00)<br>
                        Entrada esperada: 07:00 | Salida esperada: 13:00
                    </div>
                    <div class="col-md-6">
                        <strong>Turno Tarde:</strong> 02:00 PM a 08:00 PM (20:00)<br>
                        Entrada esperada: 14:00 | Salida esperada: 20:00
                    </div>
                    <div class="col-12 mt-2 pt-2 border-top">
                        <span class="badge bg-danger">Tarifa de Penalización:</span> $3.00 USD por hora de retraso o salida anticipada ($0.05 por minuto).
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
