<?php
include "conexion.php";
include_once __DIR__ . "/foto_helper.php";

$_PUT = array();
$raw = file_get_contents("php://input");
if (!empty($raw) && empty($_POST)) {
    $json = json_decode($raw, true);
    if (is_array($json)) {
        $_PUT = $json;
    } else {
        parse_str($raw, $_PUT);
    }
}
$_PUT = array_merge($_POST, $_PUT);

$id = $_GET['id'] ?? ($_PUT['id'] ?? 0);
$uid = strtoupper(trim($_PUT['uid'] ?? ''));
$nombre = trim($_PUT['nombre'] ?? '');
$cedula = trim($_PUT['cedula'] ?? '');
$departamento = trim($_PUT['departamento'] ?? '');
$cargo = trim($_PUT['cargo'] ?? '');
$activo = isset($_PUT['activo']) ? (int)$_PUT['activo'] : 1;

if ((int)$id <= 0 || $uid === '' || $nombre === '') {
    echo json_encode([
        "success"  => false,
        "error"    => "ID, UID y Nombre son campos obligatorios.",
        "errorMsg" => "ID, UID y Nombre son campos obligatorios."
    ]);
    exit;
}

$sqlCheck = "SELECT * FROM usuarios WHERE UPPER(TRIM(uid)) = '$uid' AND id != '$id'";
$resCheck = $conn->query($sqlCheck);
if ($resCheck && $resCheck->num_rows > 0) {
    $existente = $resCheck->fetch_assoc();
    $msg = "El UID '$uid' ya pertenece a otro usuario registrado (" . $existente['nombre'] . ").";
    echo json_encode(["success" => false, "error" => $msg, "errorMsg" => $msg]);
    exit;
}

$resActual = $conn->query("SELECT foto FROM usuarios WHERE id = '$id'");
$filaActual = ($resActual && $resActual->num_rows > 0) ? $resActual->fetch_assoc() : null;
$fotoActualBd = $filaActual['foto'] ?? null;

$errorFoto = '';
$fotoSubida = guardarFotoEmpleado($uid, $_FILES['foto'] ?? null, $errorFoto);
if ($fotoSubida === false) {
    echo json_encode(["success" => false, "error" => $errorFoto, "errorMsg" => $errorFoto]);
    exit;
}

$quitarFoto = isset($_PUT['quitar_foto']) && $_PUT['quitar_foto'] === '1';

if ($fotoSubida !== null) {
    $fotoValor = $fotoSubida;
} elseif ($quitarFoto) {
    $uidLimpio = preg_replace('/[^A-Za-z0-9_-]/', '', $uid);
    if ($uidLimpio !== '') {
        limpiarFotosAnterioresUid($uidLimpio, __DIR__ . "/../fotos_empleados");
    }
    $fotoValor = null;
} elseif (isset($_PUT['foto']) && trim($_PUT['foto']) !== '') {
    $fotoValor = trim($_PUT['foto']);
} elseif (isset($_PUT['foto_actual']) && trim($_PUT['foto_actual']) !== '') {
    $fotoValor = trim($_PUT['foto_actual']);
} else {
    $fotoValor = $fotoActualBd;
}

$foto = ($fotoValor !== null && $fotoValor !== '') ? "'$fotoValor'" : "NULL";

$sql = "UPDATE usuarios
        SET uid = '$uid', nombre = '$nombre', cedula = '$cedula', departamento = '$departamento', cargo = '$cargo', activo = $activo, foto = $foto
        WHERE id = '$id'";

try {
    if ($conn->query($sql) == true) {
        $sqlUpd = "UPDATE asistencias SET nombre = '$nombre', departamento = '$departamento', uid = '$uid' WHERE usuario_id = '$id'";
        $conn->query($sqlUpd);

        echo json_encode([
            "success" => true,
            "mensaje" => "Empleado actualizado exitosamente."
        ]);
    } else {
        $msg = "Error al actualizar empleado: " . $conn->error;
        echo json_encode(["success" => false, "error" => $msg, "errorMsg" => $msg]);
    }
} catch (Exception $e) {
    $msg = "Error al actualizar empleado: " . $e->getMessage();
    echo json_encode(["success" => false, "error" => $msg, "errorMsg" => $msg]);
}
?>