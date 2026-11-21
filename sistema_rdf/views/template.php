<?php
/**
 * Plantilla Principal (Template MVC) con Bootstrap 5 y JavaScript Nativo
 */
include_once __DIR__ . "/../models/auth.php";
$esAdmin = estaAutenticadoAdmin();
$accionActual = $_GET['accion'] ?? 'escaner';
?>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Sistema de Control de Acceso y Asistencia con RFID</title>

    <!-- Bootstrap 5.3 CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <!-- Bootstrap Icons -->
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
    <!-- Estilos Personalizados del Sistema -->
    <link rel="stylesheet" href="css/estilos.css?v=<?= time() ?>">
</head>
<body>

    <!-- Encabezado Institucional Superior -->
    <header class="header-rfid">
        <div class="container-fluid d-flex flex-wrap align-items-center justify-content-between">
            <div class="d-flex align-items-center">
                <i class="bi bi-shield-check fs-2 text-warning me-2"></i>
                <div>
                    <h5 class="mb-0 fw-bold text-uppercase tracking-wider">
                        Sistema de Control de Acceso y Asistencia RFID
                    </h5>
                </div>
            </div>

            <div class="d-flex align-items-center gap-3 mt-2 mt-md-0">
                <span class="reloj-badge" id="relojVivo">Hora: --:--:--</span>

                <span class="estado-lector" id="indicadorLector">
                    <span id="dotLector" class="spinner-grow spinner-grow-sm text-success" role="status"></span>
                    <span id="textoLector">Lector RFID Sincronizado</span>
                </span>

                <button type="button" class="btn btn-sm btn-outline-light fw-semibold shadow-sm" id="btnConectarSerial" onclick="conectarSerialESP32()" title="Conectar lector ESP32 directamente por USB si Java no está abierto">
                    <i class="bi bi-usb-symbol"></i> Conectar ESP32
                </button>

                <?php if ($esAdmin): ?>
                    <button class="btn btn-sm btn-danger fw-bold shadow-sm" onclick="logoutAdmin()">
                        <i class="bi bi-box-arrow-right"></i> Cerrar Sesión Admin
                    </button>
                <?php else: ?>
                    <button class="btn btn-sm btn-light fw-bold text-dark shadow-sm" data-bs-toggle="modal" data-bs-target="#modalLoginAdmin">
                        <i class="bi bi-lock-fill text-dark"></i> Acceso Admin
                    </button>
                <?php endif; ?>
            </div>
        </div>
    </header>

    <!-- Barra de Pestañas de Navegación Dinámicas -->
    <div class="bg-white border-bottom shadow-sm mb-4">
        <div class="container-fluid">
            <ul class="nav nav-tabs border-0 pt-2">
                <!-- Pestaña 1: Siempre visible (Control de Acceso Público) -->
                <li class="nav-item">
                    <a class="nav-link fw-semibold <?= ($accionActual === 'escaner' || $accionActual === 'inicio') ? 'active' : '' ?>" href="index.php?accion=escaner">
                        <i class="bi bi-upc-scan"></i> Control de Acceso
                    </a>
                </li>

                <!-- Pestañas 2 y 3: Visibles ÚNICAMENTE cuando el Admin está autenticado -->
                <?php if ($esAdmin): ?>
                <li class="nav-item">
                    <a class="nav-link fw-semibold <?= ($accionActual === 'usuarios') ? 'active' : '' ?>" href="index.php?accion=usuarios">
                        <i class="bi bi-people-fill"></i> Gestión de Personal
                    </a>
                </li>
                <li class="nav-item">
                    <a class="nav-link fw-semibold <?= ($accionActual === 'historial') ? 'active' : '' ?>" href="index.php?accion=historial">
                        <i class="bi bi-journal-text"></i> Historial y Reportes
                    </a>
                </li>
                <?php endif; ?>
            </ul>
        </div>
    </div>

    <!-- Contenedor Principal de Vistas -->
    <main class="container-fluid px-4 pb-5">
        <?php
        $mvc = new PaginasController();
        $mvc->enlacesPaginaController();
        ?>
    </main>

    <!-- Pie de Página Institucional -->
    <footer class="footer mt-auto py-3 bg-white border-top text-center text-muted small">
        <div class="container">
            <span>Turnos: Mañana [07:00 a 13:00] | Tarde [14:00 a 20:00] | Tarifa por hora de atraso o salida anticipada: <strong>$3.00 USD</strong></span><br>
        </div>
    </footer>

    <!-- =========================================================================
         MODAL: AUTENTICACIÓN DE ADMINISTRADOR
         ========================================================================= -->
    <div class="modal fade" id="modalLoginAdmin" tabindex="-1" aria-labelledby="modalLoginAdminLabel" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content shadow border-0">
                <div class="modal-header bg-dark text-white py-3">
                    <h5 class="modal-title fw-bold" id="modalLoginAdminLabel">
                        <i class="bi bi-shield-lock-fill text-white"></i> Autenticación de Administrador
                    </h5>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Cerrar"></button>
                </div>
                <div class="modal-body p-4">
                    <p class="text-muted small mb-3">
                        Ingrese sus credenciales administrativas para habilitar las secciones de <strong>Gestión de Personal</strong>, <strong>Historial y Reportes</strong>, y el <strong>Simulador RFID</strong>.
                    </p>
                    <form onsubmit="event.preventDefault(); loginAdmin();">
                        <div class="mb-3">
                            <label for="adminUsuario" class="form-label fw-semibold small">Usuario:</label>
                            <div class="input-group">
                                <span class="input-group-text"><i class="bi bi-person"></i></span>
                                <input type="text" class="form-control" id="adminUsuario" value="admin" required>
                            </div>
                        </div>
                        <div class="mb-3">
                            <label for="adminPassword" class="form-label fw-semibold small">Contraseña:</label>
                            <div class="input-group">
                                <span class="input-group-text"><i class="bi bi-key"></i></span>
                                <input type="password" class="form-control" id="adminPassword" placeholder="admin123" required>
                            </div>
                        </div>
                        <div class="d-flex justify-content-end gap-2 mt-4">
                            <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Cancelar</button>
                            <button type="submit" class="btn btn-dark fw-semibold px-4">Iniciar Sesión</button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </div>

    <!-- =========================================================================
         MODAL: DETALLE 1 A MUCHOS (ASISTENCIAS DE UN EMPLEADO ESPECÍFICO)
         ========================================================================= -->
    <div class="modal fade" id="modalAsistenciasUsuario" tabindex="-1" aria-labelledby="modalAsistenciasUsuarioLabel" aria-hidden="true">
        <div class="modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable">
            <div class="modal-content shadow border-0">
                <div class="modal-header bg-dark text-white py-3">
                    <h5 class="modal-title fw-bold" id="modalAsistenciasUsuarioLabel">
                        <i class="bi bi-diagram-3-fill text-white"></i> Marcaciones de: <span id="modalUsuarioNombre" class="text-white"></span>
                    </h5>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Cerrar"></button>
                </div>
                <div class="modal-body p-0">
                    <div class="table-responsive">
                        <table class="table table-hover align-middle mb-0">
                            <thead class="table-light">
                                <tr>
                                    <th>ID</th>
                                    <th>Fecha y Hora</th>
                                    <th>Turno / Tipo</th>
                                    <th>Horario Esperado</th>
                                    <th>Puntualidad</th>
                                    <th class="text-end">Multa ($)</th>
                                </tr>
                            </thead>
                            <tbody id="modalTablaAsistenciasCuerpo">
                                <tr>
                                    <td colspan="6" class="text-center text-muted py-3">Cargando asistencias...</td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </div>
                <div class="modal-footer py-2 bg-light">
                    <span class="text-muted small me-auto"><i class="bi bi-info-circle"></i> Historial de asistencias</span>
                    <button type="button" class="btn btn-sm btn-secondary" data-bs-dismiss="modal">Cerrar</button>
                </div>
            </div>
        </div>
    </div>

    <!-- =========================================================================
         MODAL: CÁMARA EN VIVO - VERIFICACIÓN FACIAL DEL ADMINISTRADOR (LADO A LADO)
         ========================================================================= -->
    <div class="modal fade" id="modalCamaraAdmin" data-bs-backdrop="static" data-bs-keyboard="false" tabindex="-1" aria-labelledby="modalCamaraAdminLabel" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered modal-xl">
            <div class="modal-content shadow-lg border-0">
                <div class="modal-header py-3 bg-dark text-white">
                    <div class="d-flex align-items-center justify-content-between w-100 me-2">
                        <div>
                            <h5 class="modal-title fw-bold mb-0" id="modalCamaraAdminLabel">
                                <i class="bi bi-camera-video-fill text-naranja me-2"></i> VERIFICACIÓN FACIAL Y COMPARACIÓN DE IDENTIDAD
                            </h5>
                            <small class="text-white-50" id="camaraSubtitulo">Persona: --- | UID: --- | Departamento: ---</small>
                        </div>
                        <span class="badge bg-success px-3 py-2 fw-semibold" id="camaraBadgeEstado">● EN VIVO</span>
                    </div>
                </div>
                <div class="modal-body p-3 bg-light">
                    <div class="row g-3">
                        <!-- Columna Izquierda: Identidad y Foto Registrada en Base de Datos -->
                        <div class="col-md-5">
                            <div class="card h-100 shadow-sm border">
                                <div class="card-header bg-white py-2">
                                    <h6 class="mb-0 fw-bold text-dark">
                                        <i class="bi bi-person-badge-fill me-1"></i> FOTO REGISTRADA (PERFIL)
                                    </h6>
                                    <small class="text-muted">Imagen y datos oficiales del sistema</small>
                                </div>
                                <div class="card-body text-center p-3 d-flex flex-column align-items-center justify-content-center bg-white">
                                    <div class="w-100 mb-3 text-center" style="min-height: 220px; display: flex; align-items: center; justify-content: center;">
                                        <img id="camaraFotoRegistrada" src="" alt="Foto Registrada" class="img-fluid rounded border shadow-sm" style="max-height: 220px; max-width: 100%; object-fit: contain; background: #f8f9fa;">
                                    </div>
                                    <div class="w-100 text-start border-top pt-2 small">
                                        <div class="mb-1"><strong>Nombre:</strong> <span id="camaraNombreEmp" class="text-dark fw-bold">---</span></div>
                                        <div class="mb-1"><strong>UID Tarjeta:</strong> <code id="camaraUidEmp" class="text-dark">---</code></div>
                                        <div class="mb-1"><strong>Departamento:</strong> <span id="camaraDeptoEmp" class="text-muted">---</span></div>
                                        <div class="mt-2">
                                            <span class="badge bg-naranja px-2 py-1">⚠️ 3ra Marcación: Límite Alcanzado</span>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!-- Columna Derecha: Cámara en Vivo -->
                        <div class="col-md-7">
                            <div class="card h-100 shadow-sm border">
                                <div class="card-header bg-white py-2 d-flex justify-content-between align-items-center">
                                    <div>
                                        <h6 class="mb-0 fw-bold text-dark">
                                            <i class="bi bi-camera-video me-1"></i> CÁMARA EN VIVO (PUNTO DE ACCESO)
                                        </h6>
                                        <small class="text-muted">Observe a la persona en tiempo real</small>
                                    </div>
                                    <span class="badge bg-success">● TIEMPO REAL</span>
                                </div>
                                <div class="card-body p-0 bg-dark text-center position-relative d-flex align-items-center justify-content-center" style="min-height: 330px;">
                                    <video id="videoCamaraEnVivo" autoplay playsinline class="w-100 h-100 rounded" style="max-height: 340px; object-fit: cover; transform: scaleX(-1); background: #000;"></video>
                                    <div id="camaraErrorMensaje" class="position-absolute top-50 start-50 translate-middle text-white px-3" style="display: none;">
                                        <i class="bi bi-camera-video-off fs-1 text-danger"></i>
                                        <p class="mt-2 mb-0 fw-semibold">Cámara no disponible o permiso denegado en el navegador.</p>
                                        <small class="text-white-50">Asegúrese de otorgar permisos de cámara en el navegador.</small>
                                    </div>
                                </div>
                                <div class="card-footer py-2 bg-white small text-muted">
                                    <i class="bi bi-info-circle text-naranja"></i> Compare los rasgos de la persona frente a la foto del perfil antes de autorizar.
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="modal-footer py-2 bg-white border-top d-flex justify-content-center gap-3">
                    <button type="button" class="btn btn-success fw-bold px-4 py-2 shadow-sm" id="btnCamaraAutorizar">
                        <i class="bi bi-check-circle-fill"></i> ✔ Autorizar Paso
                    </button>
                    <button type="button" class="btn btn-danger fw-bold px-4 py-2 shadow-sm" id="btnCamaraDenegar">
                        <i class="bi bi-x-circle-fill"></i> ✖ Denegar Paso
                    </button>
                    <button type="button" class="btn btn-secondary px-3 py-2" onclick="cerrarCamaraVerificacion()">
                        Cancelar
                    </button>
                </div>
            </div>
        </div>
    </div>

    <!-- jQuery local para compatibilidad universitaria -->
    <script src="jquery/jquery.min.js"></script>
    <!-- Bootstrap 5.3 Bundle con Popper JS -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
    <!-- JavaScript Nativo del Sistema -->
    <script src="js/app.js?v=<?= time() ?>"></script>
</body>
</html>
