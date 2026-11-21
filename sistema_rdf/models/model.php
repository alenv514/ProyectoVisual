<?php
class enlacesPagina {
    static function enlacePaginaModel($enlacesModel) {
        $enlacesModel = trim($enlacesModel);

        if ($enlacesModel == "usuarios" ||
            $enlacesModel == "historial" ||
            $enlacesModel == "escaner") {
            $modulo = "views/" . $enlacesModel . ".php";
        } else {
            $modulo = "views/escaner.php";
        }

        return $modulo;
    }
}
?>