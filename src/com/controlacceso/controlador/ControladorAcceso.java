package com.controlacceso.controlador;

import com.controlacceso.basededatos.AsistenciaDAO;
import com.controlacceso.basededatos.AsistenciaDAOMySql;
import com.controlacceso.basededatos.EmpleadoDAO;
import com.controlacceso.basededatos.EmpleadoDAOMySql;
import com.controlacceso.comunicacion_esp32.SerialListener;
import com.controlacceso.comunicacion_esp32.SerialManager;
import com.controlacceso.modelos.Empleado;
import com.controlacceso.modelos.GestorTurnos;
import com.controlacceso.modelos.RegistroAsistencia;
import com.controlacceso.modelos.RelojSistema;
import com.controlacceso.modelos.ResultadoCalculoTurno;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controlador principal del sistema (Patrón MVC - Modelo Vista Controlador).
 * Orquesta la lógica entre la interfaz gráfica, el hardware ESP32 y la base de datos.
 */
public class ControladorAcceso {

    private final EmpleadoDAO empleadoDAO;
    private final AsistenciaDAO asistenciaDAO;
    private final GestorTurnos gestorTurnos;
    private final SerialManager serialManager;

    private static final DateTimeFormatter FORMATTER_COMPLETO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FORMATTER_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public ControladorAcceso() {
        this(new EmpleadoDAOMySql(), new AsistenciaDAOMySql(), new GestorTurnos(), new SerialManager());
    }

    public ControladorAcceso(EmpleadoDAO empleadoDAO, AsistenciaDAO asistenciaDAO,
                             GestorTurnos gestorTurnos, SerialManager serialManager) {
        this.empleadoDAO = empleadoDAO;
        this.asistenciaDAO = asistenciaDAO;
        this.gestorTurnos = gestorTurnos;
        this.serialManager = serialManager;
    }

    public void configurarListenerSerial(SerialListener listener) {
        this.serialManager.setListener(listener);
    }

    public void iniciarComunicacionSerial() {
        this.serialManager.iniciarAutoConexion();
    }

    public void detenerComunicacionSerial() {
        this.serialManager.detenerAutoConexion();
    }

    public boolean estaConectadoSerial() {
        return this.serialManager.estaConectado();
    }

    /**
     * Procesa la lectura de una tarjeta RFID enviada por el ESP32.
     * Retorna el objeto RegistroAsistencia generado y listo para mostrar en pantalla.
     */
    public RegistroAsistencia procesarLecturaRfid(String uid) {
        if (uid == null || uid.trim().isEmpty()) {
            return null;
        }

        String uidLimpio = uid.trim().toUpperCase();
        LocalDateTime ahora = RelojSistema.ahora();
        String fechaHora = ahora.format(FORMATTER_COMPLETO);
        String fechaHoy = ahora.format(FORMATTER_FECHA);

        Empleado empleado = empleadoDAO.buscarPorUid(uidLimpio);
        boolean accesoPermitido = (empleado != null && empleado.isActivo());

        RegistroAsistencia registro = new RegistroAsistencia();
        registro.setUid(uidLimpio);
        registro.setFechaHora(fechaHora);

        if (accesoPermitido) {
            registro.setUsuarioId(empleado.getId());
            registro.setNombre(empleado.getNombre());
            registro.setDepartamento(empleado.getDepartamento());
            registro.setEstado("ACCESO PERMITIDO");

            // Consultar las marcas anteriores de hoy para este empleado
            List<RegistroAsistencia> marcasHoy = asistenciaDAO.listarPorUsuarioYFecha(uidLimpio, fechaHoy);
            ResultadoCalculoTurno calc = gestorTurnos.calcularMarcacion(ahora, marcasHoy);

            registro.setTipo(calc.getTipo());
            registro.setHorarioEsperado(calc.getHorarioEsperado());
            registro.setMinutosDiferencia(calc.getMinutosDiferencia());
            registro.setMulta(calc.getMulta());
            registro.setDetallePuntualidad(calc.getDetallePuntualidad());

            if (calc.isRequiereAutorizacionAdmin()) {
                registro.setEstado("EN ESPERA DE ADMIN");
            } else {
                registro.setEstado("ACCESO PERMITIDO");
            }
        } else {
            registro.setUsuarioId(null);
            registro.setNombre("NO REGISTRADO");
            registro.setDepartamento("-");
            registro.setTipo("ACCESO DENEGADO");
            registro.setEstado("ACCESO DENEGADO");
            registro.setHorarioEsperado("-");
            registro.setMinutosDiferencia(0);
            registro.setMulta(0.0);
            registro.setDetallePuntualidad("TARJETA NO VALIDA");
        }

        // Si no está en espera de admin, guardar y responder al ESP32 normalmente
        if (!"EN ESPERA DE ADMIN".equalsIgnoreCase(registro.getEstado())) {
            asistenciaDAO.registrar(registro);
            if (serialManager != null && serialManager.estaConectado()) {
                serialManager.enviarRespuesta(accesoPermitido, registro.getNombre());
            }
        } else {
            // No se abre la puerta todavía: se envía aviso de advertencia al ESP32
            if (serialManager != null && serialManager.estaConectado()) {
                serialManager.enviarComando("WARN:" + registro.getNombre());
            }
        }

        return registro;
    }

    public void autorizarAccesoExcepcional(RegistroAsistencia reg) {
        if (reg == null) return;
        reg.setEstado("ACCESO PERMITIDO");
        reg.setTipo("REINGRESO AUTORIZADO");
        reg.setDetallePuntualidad("AUTORIZADO POR ADMINISTRADOR");
        reg.setMulta(0.0);
        asistenciaDAO.registrar(reg);
        if (serialManager != null && serialManager.estaConectado()) {
            serialManager.enviarRespuesta(true, reg.getNombre());
        }
    }

    public void denegarAccesoExcepcional(RegistroAsistencia reg) {
        if (reg == null) return;
        reg.setEstado("ACCESO DENEGADO");
        reg.setTipo("REINGRESO DENEGADO");
        reg.setDetallePuntualidad("DENEGADO POR ADMINISTRADOR");
        reg.setMulta(0.0);
        asistenciaDAO.registrar(reg);
        if (serialManager != null && serialManager.estaConectado()) {
            serialManager.enviarRespuesta(false, reg.getNombre());
        }
    }

    // =========================================================================
    // SERVICIOS PARA GESTIÓN DE PERSONAL (CRUD)
    // =========================================================================

    public boolean guardarEmpleado(Empleado empleado) {
        return empleadoDAO.guardar(empleado);
    }

    public boolean actualizarEmpleado(Empleado empleado) {
        return empleadoDAO.actualizar(empleado);
    }

    public boolean eliminarEmpleado(int id) {
        return empleadoDAO.eliminar(id);
    }

    public Empleado buscarEmpleadoPorUid(String uid) {
        return empleadoDAO.buscarPorUid(uid);
    }

    public List<Empleado> listarEmpleados() {
        return empleadoDAO.listarTodos();
    }

    // =========================================================================
    // SERVICIOS PARA HISTORIAL Y CONSULTAS
    // =========================================================================

    public List<RegistroAsistencia> listarAsistenciasHoy() {
        return asistenciaDAO.listarHoy();
    }

    public List<RegistroAsistencia> listarAsistenciasPorFiltros(String fecha, String nombre) {
        return asistenciaDAO.listarPorFiltros(fecha, nombre);
    }

    public List<RegistroAsistencia> listarAsistenciasPorFiltros(String fecha, String nombre, String tipo, String estado) {
        return asistenciaDAO.listarPorFiltros(fecha, nombre, tipo, estado);
    }
}
