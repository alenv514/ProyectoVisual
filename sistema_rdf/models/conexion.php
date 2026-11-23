<?php
date_default_timezone_set("America/Guayaquil");

$servername = getenv('DB_HOST') ?: "localhost";
$username   = getenv('DB_USER') ?: "root";
$password   = getenv('DB_PASS') !== false ? getenv('DB_PASS') : "";
$database   = getenv('DB_NAME') ?: "control_acceso_rfid";
$port       = getenv('DB_PORT') ? (int)getenv('DB_PORT') : 3306;

try {
    $conn = mysqli_connect($servername, $username, $password, $database, $port);
    if ($conn) {
        $conn->set_charset("utf8mb4");
        $conn->query("SET time_zone = '-05:00'");
    }
} catch (Exception $e) {
    die($e->getMessage());
}
?>