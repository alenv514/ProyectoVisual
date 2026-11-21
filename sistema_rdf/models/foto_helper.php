<?php
function limpiarFotosAnterioresUid($uidLimpio, $carpetaAbs) {
    $extensiones = ['jpg', 'jpeg', 'png', 'webp'];
    foreach ($extensiones as $ext) {
        $ruta = $carpetaAbs . DIRECTORY_SEPARATOR . $uidLimpio . '.' . $ext;
        if (is_file($ruta)) {
            @unlink($ruta);
        }
    }
}

function guardarFotoEmpleado($uid, $archivo, &$errorMsg) {
    if (!isset($archivo) || !isset($archivo['error']) || $archivo['error'] === UPLOAD_ERR_NO_FILE) {
        return null;
    }

    if ($archivo['error'] !== UPLOAD_ERR_OK) {
        $errorMsg = "Error al subir la fotografía del empleado.";
        return false;
    }

    if (!is_uploaded_file($archivo['tmp_name'])) {
        $errorMsg = "Archivo de imagen inválido.";
        return false;
    }

    $infoImg = @getimagesize($archivo['tmp_name']);
    if ($infoImg === false) {
        $errorMsg = "El archivo seleccionado no es una imagen válida.";
        return false;
    }

    $mapaTipos = [
        IMAGETYPE_JPEG => 'jpg',
        IMAGETYPE_PNG  => 'png',
        IMAGETYPE_WEBP => 'webp'
    ];

    if (!isset($mapaTipos[$infoImg[2]])) {
        $errorMsg = "Formato de imagen no permitido. Use JPG, PNG o WEBP.";
        return false;
    }

    $ext = $mapaTipos[$infoImg[2]];
    $uidLimpio = preg_replace('/[^A-Za-z0-9_-]/', '', strtoupper(trim($uid)));
    if ($uidLimpio === '') {
        $uidLimpio = uniqid('emp_');
    }

    $carpetaAbs = __DIR__ . "/../fotos_empleados";
    if (!is_dir($carpetaAbs)) {
        mkdir($carpetaAbs, 0777, true);
    }

    limpiarFotosAnterioresUid($uidLimpio, $carpetaAbs);

    $nombreArchivo = $uidLimpio . '.' . $ext;
    $destinoAbs = $carpetaAbs . DIRECTORY_SEPARATOR . $nombreArchivo;

    if (!move_uploaded_file($archivo['tmp_name'], $destinoAbs)) {
        $errorMsg = "No se pudo guardar la fotografía en el servidor.";
        return false;
    }

    return "fotos_empleados/" . $nombreArchivo;
}
?>