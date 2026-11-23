<?php
date_default_timezone_set("America/Guayaquil");

include_once __DIR__ . "/conexion.php";

define('RELOJ_INTERVALO_RESYNC_SEG', 300);
define('RELOJ_COOLDOWN_VERIFICACION_SEG', 45);

function relojConsultarNtp($servidor, $timeoutSec = 0.8) {
    $errno = 0;
    $errstr = '';
    $sock = @stream_socket_client("udp://{$servidor}:123", $errno, $errstr, $timeoutSec);
    if (!$sock) {
        return null;
    }

    $sec = (int) floor($timeoutSec);
    $usec = (int) (($timeoutSec - $sec) * 1000000);
    stream_set_timeout($sock, $sec, $usec);

    $pkt = "\x1b" . str_repeat("\0", 47);
    $written = @fwrite($sock, $pkt);
    $resp = ($written === 48) ? @fread($sock, 48) : '';
    fclose($sock);

    if (!is_string($resp) || strlen($resp) !== 48) {
        return null;
    }

    $stratum = ord($resp[1]);
    if ($stratum < 1 || $stratum > 15) {
        return null;
    }

    $unpacked = @unpack('N12', $resp);
    if (!$unpacked || !isset($unpacked[11], $unpacked[12])) {
        return null;
    }

    $ntpSeconds = $unpacked[11] - 2208988800;
    $fraction = $unpacked[12] / 4294967296.0;
    $unixTs = (int) round($ntpSeconds + $fraction);

    return ($unixTs >= 1700000000) ? $unixTs : null;
}

function relojConsultarHttpDate($url, $timeoutSec = 1.5) {
    if (!function_exists('curl_init')) {
        return null;
    }

    $ch = curl_init($url);
    curl_setopt_array($ch, [
        CURLOPT_NOBODY            => true,
        CURLOPT_HEADER            => true,
        CURLOPT_RETURNTRANSFER    => true,
        CURLOPT_TIMEOUT_MS        => (int) ($timeoutSec * 1000),
        CURLOPT_CONNECTTIMEOUT_MS => (int) ($timeoutSec * 1000),
        CURLOPT_SSL_VERIFYPEER    => true
    ]);
    $headers = curl_exec($ch);
    curl_close($ch);

    if (is_string($headers) && preg_match('/^date:\s*(.+)$/im', $headers, $m)) {
        $ts = strtotime(trim($m[1]));
        if ($ts !== false && $ts >= 1700000000) {
            return (int) $ts;
        }
    }
    return null;
}

function relojObtenerHoraOficialInternet() {
    $servidoresNtp = ['time.google.com', 'pool.ntp.org', 'time.cloudflare.com'];
    foreach ($servidoresNtp as $servidor) {
        $ts = relojConsultarNtp($servidor, 0.8);
        if ($ts !== null) {
            return $ts;
        }
    }

    $urlsHttp = ['https://www.google.com/generate_204', 'https://1.1.1.1/'];
    foreach ($urlsHttp as $url) {
        $ts = relojConsultarHttpDate($url, 1.5);
        if ($ts !== null) {
            return $ts;
        }
    }

    return null;
}

function relojRutaCooldown() {
    return sys_get_temp_dir() . DIRECTORY_SEPARATOR . 'sistema_rdf_ntp_' . md5(__DIR__) . '.txt';
}

function relojPuedeIntentarSync() {
    $archivo = relojRutaCooldown();
    if (!is_file($archivo)) {
        return true;
    }
    $datos = @json_decode((string) @file_get_contents($archivo), true);
    if (!is_array($datos) || !isset($datos['hrtime'])) {
        return true;
    }
    $diff = (hrtime(true) - (int) $datos['hrtime']) / 1000000000;
    return ($diff < 0 || $diff >= RELOJ_COOLDOWN_VERIFICACION_SEG);
}

function relojRegistrarIntentoSync() {
    @file_put_contents(relojRutaCooldown(), json_encode(['hrtime' => hrtime(true)]), LOCK_EX);
}

function inicializarRelojSistema($timestampBase) {
    global $conn;
    $ahoraHrtime = hrtime(true);
    $timestampBase = (int) $timestampBase;
    $ahoraStr = date('Y-m-d H:i:s', $timestampBase);
    $hrtimeStr = (string) $ahoraHrtime;

    $sql = "REPLACE INTO reloj_sistema (id, timestamp_base, hrtime_base, ultimo_timestamp, actualizado_en)
            VALUES (1, $timestampBase, '$hrtimeStr', $timestampBase, '$ahoraStr')";
    $conn->query($sql);

    return $timestampBase;
}

function sincronizarRelojInternet($forzar = false) {
    if (!$forzar && !relojPuedeIntentarSync()) {
        return null;
    }
    relojRegistrarIntentoSync();

    $tsOficial = relojObtenerHoraOficialInternet();
    if ($tsOficial !== null) {
        return inicializarRelojSistema($tsOficial);
    }
    return null;
}

function obtenerTimestampSistema() {
    global $conn;
    $res = $conn->query("SELECT * FROM reloj_sistema WHERE id = 1");
    $ahoraHrtime = hrtime(true);

    if ($res && $res->num_rows > 0) {
        $fila = $res->fetch_assoc();
        $timestampBase = (int) $fila['timestamp_base'];
        $hrtimeBase = (int) $fila['hrtime_base'];
        $ultimoTimestamp = (int) $fila['ultimo_timestamp'];

        $segundosTranscurridos = intdiv($ahoraHrtime - $hrtimeBase, 1000000000);

        if ($segundosTranscurridos < 0) {
            $tsOficial = sincronizarRelojInternet(true);
            if ($tsOficial !== null) {
                return $tsOficial;
            }
            return inicializarRelojSistema(max($ultimoTimestamp, time()));
        }

        $timestampCalculado = $timestampBase + $segundosTranscurridos;

        $necesitaResyncPeriodica = ($segundosTranscurridos >= RELOJ_INTERVALO_RESYNC_SEG);
        $posibleDesfaseOSuspension = (abs(time() - $timestampCalculado) > 60);

        if (($necesitaResyncPeriodica || $posibleDesfaseOSuspension) && relojPuedeIntentarSync()) {
            $tsOficial = sincronizarRelojInternet(false);
            if ($tsOficial !== null) {
                return $tsOficial;
            }
        }

        if ($timestampCalculado < $ultimoTimestamp) {
            $timestampCalculado = $ultimoTimestamp;
        }

        if ($timestampCalculado > $ultimoTimestamp) {
            $ahoraStr = date('Y-m-d H:i:s', $timestampCalculado);
            $conn->query("UPDATE reloj_sistema SET ultimo_timestamp = $timestampCalculado, actualizado_en = '$ahoraStr' WHERE id = 1");
        }

        return $timestampCalculado;
    }

    $tsOficial = sincronizarRelojInternet(true);
    if ($tsOficial !== null) {
        return $tsOficial;
    }
    return inicializarRelojSistema(time());
}

function obtenerFechaHoraSistema() {
    return date('Y-m-d H:i:s', obtenerTimestampSistema());
}

function obtenerFechaSistema() {
    return date('Y-m-d', obtenerTimestampSistema());
}

function obtenerHoraSistema() {
    return date('H:i:s', obtenerTimestampSistema());
}

if (basename($_SERVER['SCRIPT_FILENAME'] ?? '') === 'reloj.php') {
    include_once __DIR__ . "/auth.php";
    header('Content-Type: application/json; charset=utf-8');

    $method = $_SERVER['REQUEST_METHOD'] ?? 'GET';
    $accion = $_REQUEST['accion'] ?? 'hora';

    if ($method === 'GET') {
        $ts = obtenerTimestampSistema();
        echo json_encode([
            "success"   => true,
            "ahora"     => date('Y-m-d H:i:s', $ts),
            "fecha"     => date('Y-m-d', $ts),
            "hora"      => date('H:i:s', $ts),
            "timestamp" => $ts
        ]);
        exit;
    } elseif ($method === 'POST') {
        if (!estaAutenticadoAdmin()) {
            echo json_encode([
                "success"  => false,
                "error"    => "Acción reservada para el Administrador.",
                "errorMsg" => "Acción reservada para el Administrador."
            ]);
            exit;
        }

        if ($accion === 'ajustar') {
            echo json_encode([
                "success"  => false,
                "error"    => "El reloj del sistema es automático e inmodificable.",
                "errorMsg" => "El reloj del sistema es automático e inmodificable (sincronizado con hora oficial)."
            ]);
            exit;
        } elseif ($accion === 'sincronizar') {
            $ts = sincronizarRelojInternet(true);
            if ($ts !== null) {
                echo json_encode([
                    "success"   => true,
                    "mensaje"   => "Reloj del sistema resincronizado con la hora oficial de Ecuador.",
                    "ahora"     => date('Y-m-d H:i:s', $ts),
                    "timestamp" => $ts
                ]);
            } else {
                $tsActual = obtenerTimestampSistema();
                echo json_encode([
                    "success"   => false,
                    "error"     => "No se pudo contactar al servidor NTP/Internet.",
                    "errorMsg"  => "No se pudo contactar al servidor NTP/Internet; continúa el reloj monotónico local.",
                    "ahora"     => date('Y-m-d H:i:s', $tsActual),
                    "timestamp" => $tsActual
                ]);
            }
            exit;
        }
    }

    echo json_encode(["success" => false, "error" => "Acción no soportada.", "errorMsg" => "Acción no soportada."]);
    exit;
}
?>