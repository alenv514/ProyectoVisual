<?php
include "conexion.php";

if (isset($_GET['asistencias_usuario'])) {
    $usuarioId = $_GET['asistencias_usuario'];

    $sqlUsuario = "SELECT * FROM usuarios WHERE id = '$usuarioId'";
    $resUsuario = $conn->query($sqlUsuario);
    $usuario = ($resUsuario && $resUsuario->num_rows > 0) ? $resUsuario->fetch_assoc() : null;

    $sqlAsist = "SELECT * FROM asistencias WHERE usuario_id = '$usuarioId' ORDER BY id DESC";
    $resAsist = $conn->query($sqlAsist);
    $asistencias = [];
    if ($resAsist && $resAsist->num_rows > 0) {
        while ($fila = $resAsist->fetch_assoc()) {
            array_push($asistencias, $fila);
        }
    }

    echo json_encode([
        "success"     => true,
        "usuario"     => $usuario,
        "asistencias" => $asistencias
    ]);
    exit;
}

if (isset($_GET['id']) && $_GET['id'] !== '') {
    $id = $_GET['id'];
    $sql = "SELECT * FROM usuarios WHERE id = '$id'";
    $respuesta = $conn->query($sql);

    if ($respuesta && $respuesta->num_rows > 0) {
        $fila = $respuesta->fetch_assoc();
        echo json_encode(["success" => true, "data" => $fila]);
    } else {
        echo json_encode(["success" => false, "error" => "Usuario no encontrado.", "errorMsg" => "Usuario no encontrado."]);
    }
    exit;
}

if (isset($_GET['uid']) && $_GET['uid'] !== '') {
    $uid = strtoupper(trim($_GET['uid']));
    $sql = "SELECT * FROM usuarios WHERE UPPER(TRIM(uid)) = '$uid'";
    $respuesta = $conn->query($sql);

    if ($respuesta && $respuesta->num_rows > 0) {
        $fila = $respuesta->fetch_assoc();
        echo json_encode(["success" => true, "data" => $fila]);
    } else {
        echo json_encode(["success" => false, "error" => "Tarjeta RFID no asignada.", "errorMsg" => "Tarjeta RFID no asignada."]);
    }
    exit;
}

$buscar = trim($_GET['buscar'] ?? '');

if ($buscar !== '') {
    $condicionId = ctype_digit($buscar) ? "u.id = $buscar OR " : "";
    $sql = "SELECT u.*, COUNT(a.id) AS total_asistencias
            FROM usuarios u
            LEFT JOIN asistencias a ON u.id = a.usuario_id
            WHERE {$condicionId}u.nombre LIKE '%$buscar%'
               OR u.uid LIKE '%$buscar%'
               OR u.cedula LIKE '%$buscar%'
               OR u.departamento LIKE '%$buscar%'
            GROUP BY u.id
            ORDER BY u.id DESC";
} else {
    $sql = "SELECT u.*, COUNT(a.id) AS total_asistencias
            FROM usuarios u
            LEFT JOIN asistencias a ON u.id = a.usuario_id
            GROUP BY u.id
            ORDER BY u.id DESC";
}

$respuesta = $conn->query($sql);
$resultado = array();

if ($respuesta && $respuesta->num_rows > 0) {
    while ($fila = $respuesta->fetch_assoc()) {
        array_push($resultado, $fila);
    }
}

echo json_encode([
    "success" => true,
    "total"   => count($resultado),
    "data"    => $resultado
]);
?>