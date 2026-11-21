<?php
include "conexion.php";
include_once __DIR__ . "/reloj.php";
include_once __DIR__ . "/auth.php";

$_DELETE = array();
$raw = file_get_contents("php://input");
if (!empty($raw)) {
    $json = json_decode($raw, true);
    if (is_array($json)) {
        $_DELETE = $json;
    } else {
        parse_str($raw, $_DELETE);
    }
}

$accion = $_GET['accion'] ?? ($_DELETE['accion'] ?? '');

if ($accion === 'limpiar_hoy') {
    if (!estaAutenticadoAdmin()) {
        echo json_encode([
            "success"  => false,
            "error"    => "Acción reservada para el Administrador.",
            "errorMsg" => "Acción reservada para el Administrador."
        ]);
        exit;
    }

    $hoyFecha = obtenerFechaSistema();
    $sql = "DELETE FROM asistencias WHERE fecha_hora LIKE '$hoyFecha%'";
    if ($conn->query($sql) == true) {
        $eliminados = $conn->affected_rows;
        echo json_encode([
            "success"    => true,
            "fecha"      => $hoyFecha,
            "eliminados" => $eliminados,
            "mensaje"    => "Se eliminaron {$eliminados} marcación(es) del día {$hoyFecha}."
        ]);
    } else {
        $msg = "Error al eliminar marcaciones de hoy: " . $conn->error;
        echo json_encode(["success" => false, "error" => $msg, "errorMsg" => $msg]);
    }
    exit;
}

$id = (int)($_GET['id'] ?? ($_DELETE['id'] ?? 0));

if ($id <= 0) {
    echo json_encode([
        "success"  => false,
        "error"    => "ID de marcación no válido.",
        "errorMsg" => "ID de marcación no válido."
    ]);
    exit;
}

$sql = "DELETE FROM asistencias WHERE id = '$id'";

if ($conn->query($sql) == true) {
    echo json_encode(["success" => true, "mensaje" => "Registro eliminado del historial."]);
} else {
    $msg = "Error al eliminar registro: " . $conn->error;
    echo json_encode(["success" => false, "error" => $msg, "errorMsg" => $msg]);
}
?>