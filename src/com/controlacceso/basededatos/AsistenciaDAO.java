package com.controlacceso.basededatos;

import com.controlacceso.modelos.RegistroAsistencia;
import java.io.File;
import java.util.List;

public interface AsistenciaDAO {

    boolean registrar(RegistroAsistencia reg);

    List<RegistroAsistencia> listarHoy();

    List<RegistroAsistencia> listarPorFiltros(String fecha, String nombre);

    default List<RegistroAsistencia> listarPorFiltros(String fecha, String nombre, String tipo, String estado) {
        return listarPorFiltros(fecha, nombre);
    }

    List<RegistroAsistencia> listarPorUsuarioYFecha(String uid, String fecha);

    default boolean exportarCsv(File archivo, List<RegistroAsistencia> registros) {
        return false;
    }
}
