package com.controlacceso.basededatos;

import com.controlacceso.modelos.RegistroAsistencia;

import java.io.File;
import java.util.List;

/**
 * Interfaz DAO para operaciones de Asistencias y Reportes.
 */
public interface AsistenciaDAO {

    boolean registrar(RegistroAsistencia reg);

    List<RegistroAsistencia> listarHoy();

    List<RegistroAsistencia> listarPorFiltros(String fecha, String nombre);

    List<RegistroAsistencia> listarPorUsuarioYFecha(String uid, String fecha);

    boolean exportarCsv(File archivo, List<RegistroAsistencia> registros);
}
