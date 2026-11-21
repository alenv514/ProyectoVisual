<?php
class PaginasController {
    public function enlacesPaginaController() {
        if (isset($_GET['accion'])) {
            $enlacesController = $_GET['accion'];
        } else {
            $enlacesController = "escaner";
        }
        $respuesta = enlacesPagina::enlacePaginaModel($enlacesController);
        include __DIR__ . "/../" . $respuesta;
    }

    public function getTemplate() {
        include __DIR__ . "/../views/template.php";
    }
}
?>