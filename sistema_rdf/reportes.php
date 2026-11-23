<?php
include __DIR__ . "/fpdf19/fpdf.php";
include __DIR__ . "/models/conexion.php";

$buscar = isset($_GET['buscar']) ? trim($_GET['buscar']) : '';
$tipo = isset($_GET['tipo']) ? trim($_GET['tipo']) : 'TODOS';
$estado = isset($_GET['estado']) ? trim($_GET['estado']) : 'TODOS';

$sql = "SELECT * FROM asistencias WHERE 1=1";

if ($buscar !== '') {
    $termino = strtolower($buscar);
    if (ctype_digit($buscar)) {
        $sql .= " AND (id = $buscar OR LOWER(nombre) LIKE '%$termino%' OR LOWER(uid) LIKE '%$termino%')";
    } else {
        $sql .= " AND (LOWER(nombre) LIKE '%$termino%' OR LOWER(uid) LIKE '%$termino%')";
    }
}

if ($tipo !== '' && strtoupper($tipo) !== 'TODOS') {
    $tipoUpper = strtoupper($tipo);
    $sql .= " AND UPPER(TRIM(tipo)) = '$tipoUpper'";
}

if ($estado !== '' && strtoupper($estado) !== 'TODOS') {
    $estadoUpper = strtoupper($estado);
    $sql .= " AND UPPER(TRIM(estado)) = '$estadoUpper'";
}

$sql .= " ORDER BY id DESC";

$respuesta = $conn->query($sql);
$registros = array();
if ($respuesta && $respuesta->num_rows > 0) {
    while ($fila = $respuesta->fetch_assoc()) {
        array_push($registros, $fila);
    }
}

function textoPdf($texto) {
    $texto = (string) $texto;
    if (function_exists('iconv')) {
        $conv = @iconv('UTF-8', 'ISO-8859-1//TRANSLIT', $texto);
        if ($conv !== false) {
            return $conv;
        }
    }
    if (function_exists('mb_convert_encoding')) {
        return mb_convert_encoding($texto, 'ISO-8859-1', 'UTF-8');
    }
    return $texto;
}

class PDFReporte extends FPDF {
    public $filtroTexto;
    public $filtroTipo;
    public $filtroEstado;

    function Header() {
        $this->SetFillColor(15, 32, 67);
        $this->Rect(0, 0, 297, 8, 'F');

        $this->SetFont('Arial', 'B', 16);
        $this->SetTextColor(15, 32, 67);
        $this->SetXY(14, 12);
        $this->Cell(200, 8, textoPdf("SISTEMA DE CONTROL DE ACCESO Y ASISTENCIA RFID"), 0, 1, 'L');

        $this->SetFont('Arial', 'I', 8);
        $this->SetTextColor(120, 120, 120);
        $this->SetXY(210, 12);
        $this->Cell(73, 5, textoPdf("Emisión: " . date('Y-m-d H:i:s')), 0, 1, 'R');

        $this->SetDrawColor(200, 205, 215);
        $this->SetLineWidth(0.4);
        $this->Line(14, 26, 283, 26);

        $this->SetFont('Arial', 'B', 8);
        $this->SetTextColor(50, 50, 50);
        $this->SetXY(14, 28);
        $infoFiltro = "Filtros: ";
        $infoFiltro .= (!empty($this->filtroTexto)) ? "[Búsqueda: '" . $this->filtroTexto . "'] " : "[Búsqueda: Todos] ";
        $infoFiltro .= "[Tipo: " . $this->filtroTipo . "] ";
        $infoFiltro .= "[Estado: " . $this->filtroEstado . "]";
        $this->Cell(269, 5, textoPdf($infoFiltro), 0, 1, 'L');

        $this->Ln(2);

        $this->SetFillColor(235, 240, 248);
        $this->SetTextColor(15, 32, 67);
        $this->SetFont('Arial', 'B', 8);
        $this->SetDrawColor(180, 195, 215);

        $this->Cell(12, 7, "ID", 1, 0, 'C', true);
        $this->Cell(32, 7, "Fecha y Hora", 1, 0, 'C', true);
        $this->Cell(25, 7, "UID", 1, 0, 'C', true);
        $this->Cell(45, 7, "Empleado", 1, 0, 'L', true);
        $this->Cell(28, 7, "Departamento", 1, 0, 'L', true);
        $this->Cell(34, 7, "Turno / Tipo", 1, 0, 'C', true);
        $this->Cell(22, 7, "Horario", 1, 0, 'C', true);
        $this->Cell(50, 7, "Puntualidad / Detalle", 1, 0, 'L', true);
        $this->Cell(21, 7, "Multa ($)", 1, 1, 'R', true);
    }

    function Footer() {
        $this->SetY(-15);
        $this->SetDrawColor(210, 210, 210);
        $this->Line(14, $this->GetY(), 283, $this->GetY());

        $this->SetFont('Arial', 'I', 8);
        $this->SetTextColor(120, 120, 120);
        $this->Cell(140, 10, textoPdf("Sistema RFID Universitario | Tarifa de atraso: $3.00/hora ($0.05/min)"), 0, 0, 'L');
        $this->Cell(129, 10, textoPdf("Página " . $this->PageNo() . " de {nb}"), 0, 0, 'R');
    }
}

$pdf = new PDFReporte('L', 'mm', 'A4');
$pdf->AliasNbPages();
$pdf->filtroTexto = $buscar;
$pdf->filtroTipo = $tipo;
$pdf->filtroEstado = $estado;
$pdf->SetMargins(14, 10, 14);
$pdf->SetAutoPageBreak(true, 18);
$pdf->AddPage();

$totalPermitidos = 0;
$totalDenegados = 0;
$totalMultas = 0.0;

if (!empty($registros)) {
    $pdf->SetFont('Arial', '', 7.5);

    foreach ($registros as $r) {
        $esDenegado = (strpos($r['estado'], 'DENEGADO') !== false || strpos($r['tipo'], 'DENEGADO') !== false);
        $multaVal = (float)$r['multa'];
        $tieneMulta = ($multaVal > 0);

        if ($esDenegado) {
            $totalDenegados++;
        } else {
            $totalPermitidos++;
        }
        $totalMultas += $multaVal;

        if ($esDenegado || $tieneMulta) {
            $pdf->SetFillColor(255, 238, 238);
            $pdf->SetTextColor(170, 20, 20);
            $fill = true;
        } else {
            $pdf->SetFillColor(255, 255, 255);
            $pdf->SetTextColor(40, 40, 40);
            $fill = false;
        }

        $pdf->SetDrawColor(220, 225, 230);

        $pdf->Cell(12, 6, $r['id'], 1, 0, 'C', $fill);
        $pdf->Cell(32, 6, $r['fecha_hora'], 1, 0, 'C', $fill);
        $pdf->Cell(25, 6, textoPdf($r['uid']), 1, 0, 'C', $fill);
        $pdf->Cell(45, 6, textoPdf(substr($r['nombre'], 0, 26)), 1, 0, 'L', $fill);
        $pdf->Cell(28, 6, textoPdf(substr($r['departamento'], 0, 16)), 1, 0, 'L', $fill);
        $pdf->Cell(34, 6, textoPdf($r['tipo']), 1, 0, 'C', $fill);
        $pdf->Cell(22, 6, textoPdf($r['horario_esperado']), 1, 0, 'C', $fill);
        $pdf->Cell(50, 6, textoPdf(substr($r['detalle_puntualidad'], 0, 32)), 1, 0, 'L', $fill);

        $multaStr = ($multaVal > 0) ? sprintf("$%.2f", $multaVal) : "$0.00";
        $pdf->Cell(21, 6, $multaStr, 1, 1, 'R', $fill);
    }

    $pdf->Ln(2);
    $pdf->SetFont('Arial', 'B', 8.5);
    $pdf->SetFillColor(240, 244, 250);
    $pdf->SetTextColor(15, 32, 67);
    $pdf->SetDrawColor(180, 195, 215);

    $pdf->Cell(176, 7, textoPdf("TOTALES GENERALES (" . count($registros) . " Registros: " . $totalPermitidos . " Permitidos | " . $totalDenegados . " Denegados)"), 1, 0, 'L', true);
    $pdf->Cell(72, 7, textoPdf("Total Multas Acumuladas:"), 1, 0, 'R', true);
    $pdf->Cell(21, 7, sprintf("$%.2f", $totalMultas), 1, 1, 'R', true);
} else {
    $pdf->SetFont('Arial', 'I', 10);
    $pdf->SetTextColor(120, 120, 120);
    $pdf->Cell(269, 12, textoPdf("No se encontraron registros con los filtros seleccionados."), 1, 1, 'C');
}

$pdf->Output('I', 'Reporte_Asistencias_RFID_' . date('Ymd_His') . '.pdf');
?>