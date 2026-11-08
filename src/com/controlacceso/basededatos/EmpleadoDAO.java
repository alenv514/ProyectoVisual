package com.controlacceso.basededatos;

import com.controlacceso.modelos.Empleado;
import java.util.List;

/**
 * Interfaz DAO (Data Access Object) para operaciones con Empleados.
 * Aplica Abstracción y Polimorfismo.
 */
public interface EmpleadoDAO {

    boolean guardar(Empleado empleado);

    boolean actualizar(Empleado empleado);

    boolean eliminar(int id);

    Empleado buscarPorId(int id);

    Empleado buscarPorUid(String uid);

    List<Empleado> listarTodos();
}
