<?php
include "conexion.php";

$_PUT = array();
$raw = file_get_contents("php://input");
if (!empty($raw)) {
    $json = json_decode($raw, true);
    if (is_array($json)) {
        $_PUT = $json;
    } else {
        parse_str($raw, $_PUT);
    }
}
$_PUT = array_merge($_POST, $_PUT);

$id = (int)($_GET['id'] ?? ($_PUT['id'] ?? 0));
$decision = $_PUT['decision'] ?? ($_GET['decision'] ?? 'autorizar');

if ($id <= 0) {
    echo json_encode([
        "success"  => false,
        "error"    => "ID no válido.",
        "errorMsg" => "ID no válido."
    ]);
    exit;
}

if ($decision === 'autorizar') {
    $sql = "UPDATE asistencias SET tipo = 'REINGRESO AUTORIZADO', estado = 'ACCESO PERMITIDO', detalle_puntualidad = 'AUTORIZADO POR ADMINISTRADOR', multa = 0.0 WHERE id = '$id'";
} else {
    $sql = "UPDATE asistencias SET tipo = 'REINGRESO DENEGADO', estado = 'ACCESO DENEGADO', detalle_puntualidad = 'DENEGADO POR ADMINISTRADOR', multa = 0.0 WHERE id = '$id'";
}

if ($conn->query($sql) == true) {
    echo json_encode(["success" => true]);
} else {
    $msg = "Error al actualizar asistencia: " . $conn->error;
    echo json_encode(["success" => false, "error" => $msg, "errorMsg" => $msg]);
}
?>