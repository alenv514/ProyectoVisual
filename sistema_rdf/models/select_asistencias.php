<?php
include "conexion.php";

$accion = $_GET['accion'] ?? 'listar';

if ($accion === 'estadisticas') {
    $sql = "SELECT COUNT(*) as total,
                   SUM(CASE WHEN estado = 'ACCESO PERMITIDO' THEN 1 ELSE 0 END) as permitidos,
                   SUM(CASE WHEN estado = 'ACCESO DENEGADO' THEN 1 ELSE 0 END) as denegados,
                   SUM(multa) as total_multas
            FROM asistencias";
    $res = $conn->query($sql);
    $stats = $res ? $res->fetch_assoc() : [
        "total" => 0,
        "permitidos" => 0,
        "denegados" => 0,
        "total_multas" => 0.0
    ];
    $stats['total'] = (int)$stats['total'];
    $stats['permitidos'] = (int)$stats['permitidos'];
    $stats['denegados'] = (int)$stats['denegados'];
    $stats['total_multas'] = round((float)$stats['total_multas'], 2);

    echo json_encode(["success" => true, "data" => $stats]);
    exit;
}

if ($accion === 'ultima_marcacion') {
    $ultimoId = (int)($_GET['ultimo_id'] ?? 0);
    if ($ultimoId > 0) {
        $sql = "SELECT a.*, u.foto FROM asistencias a LEFT JOIN usuarios u ON a.usuario_id = u.id WHERE a.id > $ultimoId ORDER BY a.id DESC LIMIT 1";
    } else {
        $sql = "SELECT a.*, u.foto FROM asistencias a LEFT JOIN usuarios u ON a.usuario_id = u.id ORDER BY a.id DESC LIMIT 1";
    }
    $res = $conn->query($sql);
    $reg = ($res && $res->num_rows > 0) ? $res->fetch_assoc() : null;

    echo json_encode([
        "success"  => true,
        "hayNueva" => ($reg !== null),
        "registro" => $reg
    ]);
    exit;
}

$fecha = trim($_GET['fecha'] ?? '');
$texto = trim($_GET['buscar'] ?? '');
$tipo = trim($_GET['tipo'] ?? '');
$estado = trim($_GET['estado'] ?? '');

if (strtolower($fecha) === 'hoy') {
    include_once __DIR__ . "/reloj.php";
    $fecha = obtenerFechaSistema();
}

$sql = "SELECT a.*, u.foto FROM asistencias a LEFT JOIN usuarios u ON a.usuario_id = u.id WHERE 1=1";

if ($fecha !== '') {
    $sql .= " AND a.fecha_hora LIKE '$fecha%'";
}

if ($texto !== '') {
    $termino = strtolower($texto);
    if (ctype_digit($texto)) {
        $sql .= " AND (a.id = $texto OR LOWER(a.nombre) LIKE '%$termino%' OR LOWER(a.uid) LIKE '%$termino%')";
    } else {
        $sql .= " AND (LOWER(a.nombre) LIKE '%$termino%' OR LOWER(a.uid) LIKE '%$termino%')";
    }
}

if ($tipo !== '' && strtoupper($tipo) !== "TODOS") {
    $tipoUpper = strtoupper($tipo);
    $sql .= " AND UPPER(TRIM(a.tipo)) = '$tipoUpper'";
}

if ($estado !== '' && strtoupper($estado) !== "TODOS") {
    $estadoUpper = strtoupper($estado);
    $sql .= " AND UPPER(TRIM(a.estado)) = '$estadoUpper'";
}

$sql .= " ORDER BY a.id DESC";

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