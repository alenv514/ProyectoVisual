<?php
include "conexion.php";
include_once __DIR__ . "/foto_helper.php";

$uid = strtoupper(trim($_POST['uid'] ?? ''));
$nombre = trim($_POST['nombre'] ?? '');
$cedula = trim($_POST['cedula'] ?? '');
$departamento = trim($_POST['departamento'] ?? '');
$cargo = trim($_POST['cargo'] ?? '');
$activo = isset($_POST['activo']) ? (int)$_POST['activo'] : 1;
$creado_en = date('Y-m-d H:i:s');
$fotoValor = isset($_POST['foto']) && trim($_POST['foto']) !== '' ? trim($_POST['foto']) : null;

if ($uid === '' || $nombre === '') {
    echo json_encode([
        "success"  => false,
        "error"    => "El UID de la tarjeta y el Nombre son obligatorios.",
        "errorMsg" => "El UID de la tarjeta y el Nombre son obligatorios."
    ]);
    exit;
}

$sqlCheck = "SELECT * FROM usuarios WHERE UPPER(TRIM(uid)) = '$uid'";
$resCheck = $conn->query($sqlCheck);
if ($resCheck && $resCheck->num_rows > 0) {
    $existente = $resCheck->fetch_assoc();
    $msg = "La tarjeta con UID '$uid' ya está asignada al usuario: " . $existente['nombre'];
    echo json_encode(["success" => false, "error" => $msg, "errorMsg" => $msg]);
    exit;
}

$errorFoto = '';
$fotoSubida = guardarFotoEmpleado($uid, $_FILES['foto'] ?? null, $errorFoto);
if ($fotoSubida === false) {
    echo json_encode(["success" => false, "error" => $errorFoto, "errorMsg" => $errorFoto]);
    exit;
}
if ($fotoSubida !== null) {
    $fotoValor = $fotoSubida;
}

$foto = ($fotoValor !== null) ? "'$fotoValor'" : "NULL";

$sql = "INSERT INTO usuarios (uid, nombre, cedula, departamento, cargo, activo, creado_en, foto)
        VALUES ('$uid', '$nombre', '$cedula', '$departamento', '$cargo', $activo, '$creado_en', $foto)";

try {
    if ($conn->query($sql)) {
        echo json_encode([
            "success" => true,
            "mensaje" => "Empleado registrado correctamente con ID: " . $conn->insert_id,
            "id"      => $conn->insert_id
        ]);
    } else {
        $msg = "Error al registrar en la base de datos: " . $conn->error;
        echo json_encode(["success" => false, "error" => $msg, "errorMsg" => $msg]);
    }
} catch (Exception $e) {
    $msg = "Error al registrar en la base de datos: " . $e->getMessage();
    echo json_encode(["success" => false, "error" => $msg, "errorMsg" => $msg]);
}
?>