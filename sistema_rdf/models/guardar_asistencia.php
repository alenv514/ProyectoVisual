<?php
include "conexion.php";
include_once __DIR__ . "/reloj.php";
include_once __DIR__ . "/turnos.php";

$uid = strtoupper(trim($_POST['uid'] ?? ''));

if ($uid === '') {
    echo json_encode([
        "success"  => false,
        "error"    => "UID de tarjeta vacío.",
        "errorMsg" => "Debe proporcionar el UID de la tarjeta."
    ]);
    exit;
}

$ahoraMomento = obtenerFechaHoraSistema();
$hoyFecha = obtenerFechaSistema();
$horaActual = obtenerHoraSistema();

$sqlUser = "SELECT * FROM usuarios WHERE UPPER(TRIM(uid)) = '$uid'";
$resUser = $conn->query($sqlUser);
$usuario = ($resUser && $resUser->num_rows > 0) ? $resUser->fetch_assoc() : null;

$accesoPermitido = ($usuario !== null && (int)$usuario['activo'] === 1);

$usuarioId = null;
$nombre = "NO REGISTRADO";
$departamento = "-";
$tipo = "ACCESO DENEGADO";
$estado = "ACCESO DENEGADO";
$horarioEsperado = "-";
$minutosDiferencia = 0;
$multa = 0.00;
$detallePuntualidad = "TARJETA NO VALIDA";

if ($accesoPermitido) {
    $usuarioId = (int)$usuario['id'];
    $nombre = $usuario['nombre'];
    $departamento = $usuario['departamento'];
    $estado = "ACCESO PERMITIDO";

    $sqlMarcas = "SELECT * FROM asistencias WHERE uid = '$uid' AND fecha_hora LIKE '$hoyFecha%' ORDER BY id ASC";
    $resMarcas = $conn->query($sqlMarcas);
    $marcasHoy = array();
    if ($resMarcas && $resMarcas->num_rows > 0) {
        while ($fila = $resMarcas->fetch_assoc()) {
            array_push($marcasHoy, $fila);
        }
    }

    $calc = calcularMarcacionTurno($horaActual, $marcasHoy);

    $tipo = $calc['tipo'];
    $horarioEsperado = $calc['horarioEsperado'];
    $minutosDiferencia = $calc['minutosDiferencia'];
    $multa = $calc['multa'];
    $detallePuntualidad = $calc['detallePuntualidad'];

    if (!empty($calc['requiereAutorizacionAdmin'])) {
        $estado = "EN ESPERA DE ADMIN";
        $accesoPermitido = false;
    }
} elseif ($usuario !== null && (int)$usuario['activo'] === 0) {
    $nombre = $usuario['nombre'];
    $departamento = $usuario['departamento'];
    $detallePuntualidad = "USUARIO INACTIVO";
}

$usuarioIdSql = ($usuarioId !== null) ? $usuarioId : "NULL";

$sql = "INSERT INTO asistencias (usuario_id, uid, nombre, departamento, tipo, estado, fecha_hora, horario_esperado, minutos_diferencia, multa, detalle_puntualidad)
        VALUES ($usuarioIdSql, '$uid', '$nombre', '$departamento', '$tipo', '$estado', '$ahoraMomento', '$horarioEsperado', $minutosDiferencia, $multa, '$detallePuntualidad')";

try {
    if ($conn->query($sql)) {
        $insertId = $conn->insert_id;
        echo json_encode([
            "success"         => true,
            "accesoPermitido" => $accesoPermitido,
            "registro"        => [
                "id"                  => $insertId,
                "usuario_id"          => $usuarioId,
                "uid"                 => $uid,
                "nombre"              => $nombre,
                "departamento"        => $departamento,
                "tipo"                => $tipo,
                "estado"              => $estado,
                "fecha_hora"          => $ahoraMomento,
                "horario_esperado"    => $horarioEsperado,
                "minutos_diferencia"  => $minutosDiferencia,
                "multa"               => $multa,
                "detalle_puntualidad" => $detallePuntualidad,
                "foto"                => $usuario['foto'] ?? null
            ]
        ]);
    } else {
        $msg = "Error al guardar marcación en MySQL: " . $conn->error;
        echo json_encode(["success" => false, "error" => $msg, "errorMsg" => $msg]);
    }
} catch (Exception $e) {
    $msg = "Error al guardar marcación en MySQL: " . $e->getMessage();
    echo json_encode(["success" => false, "error" => $msg, "errorMsg" => $msg]);
}
?>