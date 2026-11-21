<?php
date_default_timezone_set("America/Guayaquil");

define('TURNO_TARIFA_POR_HORA', 3.00);
define('TURNO_TARIFA_POR_MINUTO', 0.05);

function crearResultadoTurno($tipo, $horarioEsperado, $minutosDiferencia, $multa, $detallePuntualidad, $requiereAutorizacionAdmin = false) {
    return [
        "tipo"                      => $tipo,
        "horarioEsperado"           => $horarioEsperado,
        "minutosDiferencia"         => (int) $minutosDiferencia,
        "multa"                     => round((float) $multa, 2),
        "detallePuntualidad"        => $detallePuntualidad,
        "requiereAutorizacionAdmin" => $requiereAutorizacionAdmin
    ];
}

function evaluarEntradaTurno($nombreTurno, $horaEntradaEsperada, $horaActual) {
    $tiempoMarcacion = strtotime($horaActual);
    $tiempoEsperado = strtotime($horaEntradaEsperada);

    if ($tiempoMarcacion > $tiempoEsperado) {
        $minutos = (int) round(($tiempoMarcacion - $tiempoEsperado) / 60);
        $multa = round($minutos * TURNO_TARIFA_POR_MINUTO, 2);
        $detalle = sprintf("ATRASO: %d min (Multa: $%.2f)", $minutos, $multa);
        return crearResultadoTurno("ENTRADA " . $nombreTurno, $horaEntradaEsperada, $minutos, $multa, $detalle);
    }

    return crearResultadoTurno("ENTRADA " . $nombreTurno, $horaEntradaEsperada, 0, 0.0, "A TIEMPO ($0.00)");
}

function evaluarSalidaTurno($nombreTurno, $horaSalidaEsperada, $horaActual) {
    $tiempoMarcacion = strtotime($horaActual);
    $tiempoEsperado = strtotime($horaSalidaEsperada);

    if ($tiempoMarcacion < $tiempoEsperado) {
        $minutos = (int) round(($tiempoEsperado - $tiempoMarcacion) / 60);
        $multa = round($minutos * TURNO_TARIFA_POR_MINUTO, 2);
        $detalle = sprintf("SALIDA ANTICIPADA: %d min (Multa: $%.2f)", $minutos, $multa);
        return crearResultadoTurno("SALIDA " . $nombreTurno, $horaSalidaEsperada, $minutos, $multa, $detalle);
    }

    return crearResultadoTurno("SALIDA " . $nombreTurno, $horaSalidaEsperada, 0, 0.0, "CUMPLIDO ($0.00)");
}

function calcularMarcacionTurno($horaActual, $marcasHoy = []) {
    $tiempoActual = strtotime($horaActual);
    $hora13 = strtotime("13:00:00");
    $hora14 = strtotime("14:00:00");
    $hora20 = strtotime("20:00:00");

    $tieneEntradaManana = false;
    $tieneSalidaManana = false;
    $tieneEntradaTarde = false;
    $tieneSalidaTarde = false;
    $enReingresoActivo = false;

    if (is_array($marcasHoy)) {
        foreach ($marcasHoy as $m) {
            $estado = strtoupper($m['estado'] ?? '');
            if ($estado !== 'ACCESO PERMITIDO') continue;

            $t = strtoupper($m['tipo'] ?? '');
            $norm = preg_replace('/[^A-Z]/', ' ', $t);

            $horaMarca = -1;
            $fh = $m['fecha_hora'] ?? '';
            if (strlen($fh) >= 13) {
                $horaMarca = (int) substr($fh, 11, 2);
            }

            if (strpos($norm, 'REINGRESO') !== false) {
                $enReingresoActivo = (strpos($norm, 'SALIDA') === false && strpos($norm, 'DENEGADO') === false);
                if ($horaMarca >= 0 && $horaMarca < 13) {
                    $tieneEntradaManana = true;
                    $tieneSalidaManana = true;
                } elseif ($horaMarca >= 13) {
                    $tieneEntradaTarde = true;
                    $tieneSalidaTarde = true;
                }
                continue;
            } else {
                $enReingresoActivo = false;
            }

            $esEntrada = (strpos($norm, 'ENTRADA') !== false);
            $esSalida = (strpos($norm, 'SALIDA') !== false);

            $esManana = (strpos($norm, 'MA') !== false) || ($horaMarca >= 0 && $horaMarca < 13);
            $esTarde = (strpos($norm, 'TARDE') !== false) || (strpos($norm, 'ANTICIPADA') !== false) || ($horaMarca >= 13);

            if ($esManana) {
                if ($esEntrada) $tieneEntradaManana = true;
                if ($esSalida) $tieneSalidaManana = true;
            } elseif ($esTarde) {
                if ($esEntrada) $tieneEntradaTarde = true;
                if ($esSalida) $tieneSalidaTarde = true;
            }
        }
    }

    if ($enReingresoActivo) {
        return crearResultadoTurno("SALIDA DE REINGRESO", "-", 0, 0.0, "SALIDA DE REINGRESO ($0.00)");
    }

    if ($tiempoActual < $hora13) {
        if (!$tieneEntradaManana) {
            return evaluarEntradaTurno("MAÑANA", "07:00", $horaActual);
        } elseif (!$tieneSalidaManana) {
            return evaluarSalidaTurno("MAÑANA", "13:00", $horaActual);
        } else {
            return crearResultadoTurno("REQUIERE AUTORIZACIÓN ADMIN", "-", 0, 0.0, "LÍMITE ALCANZADO: ESPERE AL ADMIN", true);
        }
    } elseif ($tiempoActual < $hora14) {
        if ($tieneEntradaManana && !$tieneSalidaManana) {
            return evaluarSalidaTurno("MAÑANA", "13:00", $horaActual);
        } elseif (!$tieneEntradaTarde) {
            return crearResultadoTurno("ENTRADA ANTICIPADA", "14:00", 0, 0.0, "A TIEMPO ($0.00)");
        } elseif (!$tieneSalidaTarde) {
            return evaluarSalidaTurno("TARDE", "20:00", $horaActual);
        } else {
            return crearResultadoTurno("REQUIERE AUTORIZACIÓN ADMIN", "-", 0, 0.0, "LÍMITE ALCANZADO: ESPERE AL ADMIN", true);
        }
    } elseif ($tiempoActual < $hora20) {
        if (!$tieneEntradaTarde) {
            return evaluarEntradaTurno("TARDE", "14:00", $horaActual);
        } elseif (!$tieneSalidaTarde) {
            return evaluarSalidaTurno("TARDE", "20:00", $horaActual);
        } else {
            return crearResultadoTurno("REQUIERE AUTORIZACIÓN ADMIN", "-", 0, 0.0, "LÍMITE ALCANZADO: ESPERE AL ADMIN", true);
        }
    } else {
        if ($tieneEntradaTarde && !$tieneSalidaTarde) {
            return evaluarSalidaTurno("TARDE", "20:00", $horaActual);
        } elseif (!$tieneEntradaTarde) {
            return crearResultadoTurno("ENTRADA ANTICIPADA", "-", 0, 0.0, "A TIEMPO ($0.00)");
        } else {
            return crearResultadoTurno("REQUIERE AUTORIZACIÓN ADMIN", "-", 0, 0.0, "LÍMITE ALCANZADO: ESPERE AL ADMIN", true);
        }
    }
}
?>