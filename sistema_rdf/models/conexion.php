<?php
date_default_timezone_set("America/Guayaquil");

$servername = "localhost";
$username = "root";
$password = "";
$database = "control_acceso_rfid";

try {
    $conn = mysqli_connect($servername, $username, $password, $database);
    if ($conn) {
        $conn->set_charset("utf8mb4");
        $conn->query("SET time_zone = '-05:00'");
    }
} catch (Exception $e) {
    die($e->getMessage());
}
?>