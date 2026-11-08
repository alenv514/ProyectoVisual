package com.controlacceso.basededatos;

import com.controlacceso.modelos.RegistroAsistencia;

import java.util.List;

/**
 * Interfaz DAO para operaciones de Asistencias y Marcaciones.
 */
public interface AsistenciaDAO {

    boolean registrar(RegistroAsistencia reg);

    List<RegistroAsistencia> listarHoy();

    List<RegistroAsistencia> listarPorFiltros(String fecha, String nombre);

    List<RegistroAsistencia> listarPorFiltros(String fecha, String nombre, String tipo, String estado);

    List<RegistroAsistencia> listarPorUsuarioYFecha(String uid, String fecha);
}
