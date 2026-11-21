<?php
/**
 * Vista: Gestión de Personal (Entidad Padre en la relación 1 a Muchos)
 * Protegida para Administradores
 */
include_once __DIR__ . "/../models/auth.php";
if (!estaAutenticadoAdmin()) {
    echo '<div class="alert alert-warning text-center my-4"><i class="bi bi-shield-lock-fill"></i> Debe iniciar sesión como Administrador para acceder a la Gestión de Personal.</div>';
    return;
}
?>

<div class="row">
    <!-- Formulario de Registro / Edición -->
    <div class="col-lg-4 mb-4">
        <div class="card shadow-sm border-0">
            <div class="card-header bg-white border-bottom py-3">
                <h5 class="card-title mb-0 fw-bold text-dark">
                    <i class="bi bi-person-badge"></i> Datos del Empleado y Tarjeta
                </h5>
            </div>
            <div class="card-body">
                <form id="formUsuario" onsubmit="event.preventDefault(); guardarUsuario();">
                    <input type="hidden" id="usuarioId" value="0">

                    <div class="mb-3">
                        <label for="txtRegUid" class="form-label fw-semibold small">UID Tarjeta: <span class="text-danger">*</span></label>
                        <div class="input-group">
                            <input type="text" class="form-control font-monospace" id="txtRegUid" placeholder="Ej: C31CF528" required>
                            <button class="btn btn-outline-secondary btn-sm" type="button" onclick="document.getElementById('txtRegUid').value = 'TARJETA_' + Math.floor(Math.random()*9000+1000)">
                                Generar
                            </button>
                        </div>
                        <div class="form-text small">Código hexadecimal grabado en el chip RFID.</div>
                    </div>

                    <div class="mb-3">
                        <label for="txtRegNombre" class="form-label fw-semibold small">Nombre Completo: <span class="text-danger">*</span></label>
                        <input type="text" class="form-control" id="txtRegNombre" placeholder="Ej: Carlos Mendoza" required>
                    </div>

                    <div class="mb-3">
                        <label for="txtRegCedula" class="form-label fw-semibold small">Cédula / DNI:</label>
                        <input type="text" class="form-control" id="txtRegCedula" placeholder="Ej: 1720304050">
                    </div>

                    <div class="mb-3">
                        <label for="txtRegDepto" class="form-label fw-semibold small">Departamento:</label>
                        <input type="text" class="form-control" id="txtRegDepto" placeholder="Ej: Sistemas, Contabilidad">
                    </div>

                    <div class="mb-3">
                        <label for="txtRegCargo" class="form-label fw-semibold small">Cargo:</label>
                        <input type="text" class="form-control" id="txtRegCargo" placeholder="Ej: Desarrollador, Analista">
                    </div>

                    <div class="mb-3">
                        <label for="txtRegFoto" class="form-label fw-semibold small">Foto de Perfil:</label>
                        <div class="d-flex align-items-center gap-2 mb-2">
                            <img id="previewFotoUsuario" src="" alt="Vista previa" class="rounded border bg-light" style="width: 56px; height: 56px; object-fit: cover; display: none;">
                            <div class="flex-grow-1">
                                <input type="file" class="form-control form-control-sm" id="txtRegFoto" accept="image/jpeg,image/png,image/webp" onchange="previsualizarFotoUsuario(this)">
                            </div>
                            <button type="button" id="btnQuitarFoto" class="btn btn-sm btn-outline-secondary" style="display: none;" onclick="quitarFotoFormulario()" title="Quitar foto">
                                <i class="bi bi-x-lg"></i>
                            </button>
                        </div>
                        <input type="hidden" id="fotoActual" value="">
                        <input type="hidden" id="quitarFotoFlag" value="0">
                        <div class="form-text small">Opcional (JPG, PNG o WEBP). Se usa en el perfil y verificación facial.</div>
                    </div>

                    <div class="mb-3 form-check">
                        <input type="checkbox" class="form-check-input" id="chkRegActivo" checked>
                        <label class="form-check-label fw-semibold small" for="chkRegActivo">Usuario Activo en el Sistema</label>
                    </div>

                    <div class="d-grid gap-2">
                        <button type="submit" id="btnGuardarUsuario" class="btn btn-guardar">
                            Guardar Empleado
                        </button>
                        <button type="button" class="btn btn-outline-secondary" onclick="limpiarFormUsuario()">
                            Limpiar Formulario
                        </button>
                        <button type="button" id="btnCancelarEdicion" class="btn btn-outline-danger" style="display: none;" onclick="limpiarFormUsuario()">
                            Cancelar Edición
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <!-- Tabla de Empleados Registrados con Relación 1 a Muchos -->
    <div class="col-lg-8 mb-4">
        <div class="card shadow-sm border-0">
            <div class="card-header bg-white border-bottom py-3">
                <div class="row g-2 align-items-center">
                    <div class="col-sm-6">
                        <h5 class="card-title mb-0 fw-bold text-dark">
                            <i class="bi bi-people-fill"></i> Personal Registrado
                        </h5>
                    </div>
                    <div class="col-sm-6">
                        <div class="input-group">
                            <input type="text" id="txtBuscarUsuario" class="form-control form-control-sm" placeholder="Buscar por ID, Nombre, UID, Cédula...">
                            <button class="btn btn-sm btn-dark" type="button" onclick="cargarUsuarios(document.getElementById('txtBuscarUsuario').value)">
                                Buscar
                            </button>
                            <button class="btn btn-sm btn-outline-secondary" type="button" onclick="document.getElementById('txtBuscarUsuario').value=''; cargarUsuarios();">
                                Todos
                            </button>
                        </div>
                    </div>
                </div>
            </div>
            <div class="card-body p-0">
                <div class="table-responsive">
                    <table class="table table-hover align-middle mb-0">
                        <thead class="table-light">
                            <tr>
                                <th>ID</th>
                                <th>Foto</th>
                                <th>UID</th>
                                <th>Nombre</th>
                                <th>Cédula</th>
                                <th>Depto</th>
                                <th>Cargo</th>
                                <th>Estado</th>
                                <th class="text-end">Acciones</th>
                            </tr>
                        </thead>
                        <tbody id="tablaUsuariosCuerpo">
                            <tr>
                                <td colspan="9" class="text-center text-muted py-3">Cargando personal...</td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
</div>
