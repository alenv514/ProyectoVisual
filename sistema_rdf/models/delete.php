<?php
include "conexion.php";

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

$id = $_GET['id'] ?? ($_DELETE['id'] ?? 0);

if ((int)$id <= 0) {
    echo json_encode([
        "success"  => false,
        "error"    => "ID de usuario inválido.",
        "errorMsg" => "ID de usuario inválido."
    ]);
    exit;
}

$sql = "DELETE FROM usuarios WHERE id = '$id'";

try {
    if ($conn->query($sql) == true) {
        echo json_encode([
            "success" => true,
            "mensaje" => "Empleado eliminado de la base de datos."
        ]);
    } else {
        $msg = "Error al eliminar empleado: " . $conn->error;
        echo json_encode(["success" => false, "error" => $msg, "errorMsg" => $msg]);
    }
} catch (Exception $e) {
    $msg = "Error al eliminar empleado: " . $e->getMessage();
    echo json_encode(["success" => false, "error" => $msg, "errorMsg" => $msg]);
}
?>