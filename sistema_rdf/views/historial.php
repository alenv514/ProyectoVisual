<?php
/**
 * Vista: Historial y Reportes con Filtros Dinámicos (Tipo y Estado)
 * Protegida para Administradores
 */
include_once __DIR__ . "/../models/auth.php";
if (!estaAutenticadoAdmin()) {
    echo '<div class="alert alert-warning text-center my-4"><i class="bi bi-shield-lock-fill"></i> Debe iniciar sesión como Administrador para consultar el Historial y generar Reportes.</div>';
    return;
}
?>

<div class="card shadow-sm border-0 mb-4">
    <!-- Barra Superior de Filtros Dinámicos -->
    <div class="card-header bg-white border-bottom py-3">
        <div class="row g-2 align-items-center">
            <div class="col-md-3">
                <label class="form-label small fw-bold text-muted mb-1">Buscar:</label>
                <input type="text" id="txtBuscarHistorial" class="form-control form-control-sm" placeholder="ID, Nombre o UID...">
            </div>

            <div class="col-md-3">
                <label class="form-label small fw-bold text-muted mb-1">Tipo:</label>
                <select id="cbFiltroTipo" class="form-select form-select-sm">
                    <option value="TODOS">TODOS</option>
                    <option value="ENTRADA MAÑANA">ENTRADA MAÑANA</option>
                    <option value="SALIDA MAÑANA">SALIDA MAÑANA</option>
                    <option value="ENTRADA TARDE">ENTRADA TARDE</option>
                    <option value="SALIDA TARDE">SALIDA TARDE</option>
                    <option value="ENTRADA ANTICIPADA">ENTRADA ANTICIPADA</option>
                    <option value="REQUIERE AUTORIZACIÓN ADMIN">REQUIERE AUTORIZACIÓN ADMIN</option>
                    <option value="REINGRESO AUTORIZADO">REINGRESO AUTORIZADO</option>
                    <option value="REINGRESO DENEGADO">REINGRESO DENEGADO</option>
                    <option value="SALIDA DE REINGRESO">SALIDA DE REINGRESO</option>
                    <option value="ACCESO DENEGADO">ACCESO DENEGADO</option>
                </select>
            </div>

            <div class="col-md-2">
                <label class="form-label small fw-bold text-muted mb-1">Estado:</label>
                <select id="cbFiltroEstado" class="form-select form-select-sm">
                    <option value="TODOS">TODOS</option>
                    <option value="ACCESO PERMITIDO">ACCESO PERMITIDO</option>
                    <option value="ACCESO DENEGADO">ACCESO DENEGADO</option>
                    <option value="EN ESPERA DE ADMIN">EN ESPERA DE ADMIN</option>
                </select>
            </div>

            <div class="col-md-4 d-flex align-items-end gap-1 pt-3">
                <button class="btn btn-sm btn-dark fw-semibold" type="button" onclick="cargarHistorial()">
                    <i class="bi bi-funnel-fill"></i> Aplicar Filtro
                </button>
                <button class="btn btn-sm btn-outline-secondary" type="button" onclick="limpiarFiltrosHistorial()">
                    <i class="bi bi-x-circle"></i> Limpiar
                </button>
                <button class="btn btn-sm btn-jasper ms-auto" type="button" onclick="generarReportePDF()">
                    <i class="bi bi-file-earmark-pdf-fill"></i> Generar Reporte FPDF (PDF)
                </button>
            </div>
        </div>
    </div>

    <!-- Tabla de Marcaciones e Infracciones -->
    <div class="card-body p-0">
        <div class="table-responsive">
            <table class="table table-hover align-middle mb-0">
                <thead class="table-light">
                    <tr>
                        <th>ID</th>
                        <th>Fecha y Hora</th>
                        <th>UID</th>
                        <th>Empleado</th>
                        <th>Departamento</th>
                        <th>Turno / Tipo</th>
                        <th>Horario Oficial</th>
                        <th>Puntualidad</th>
                        <th class="text-end">Multa ($)</th>
                    </tr>
                </thead>
                <tbody id="tablaHistorialCuerpo">
                    <tr>
                        <td colspan="9" class="text-center text-muted py-4">Cargando historial de asistencias...</td>
                    </tr>
                </tbody>
            </table>
        </div>
    </div>

    <div class="card-footer bg-light py-2 text-muted small d-flex justify-content-between align-items-center">
        <span><i class="bi bi-info-circle"></i> Las filas resaltadas en rojo suave indican <strong>accesos denegados</strong> o <strong>infracciones con multas acumuladas</strong>.</span>
        <span>Tarifa oficial: <strong>$3.00 USD/hora ($0.05/min)</strong></span>
    </div>
</div>
