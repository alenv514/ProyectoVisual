<?php
date_default_timezone_set("America/Guayaquil");

require_once __DIR__ . "/models/model.php";
require_once __DIR__ . "/controllers/controller.php";

$mvc = new PaginasController();
$mvc->getTemplate();
?>