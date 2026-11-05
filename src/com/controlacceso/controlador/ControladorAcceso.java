package com.controlacceso.controlador;

import com.controlacceso.basededatos.AsistenciaDAO;
import com.controlacceso.basededatos.AsistenciaDAOSqlite;
import com.controlacceso.basededatos.EmpleadoDAO;
import com.controlacceso.basededatos.EmpleadoDAOSqlite;
import com.controlacceso.comunicacion_esp32.SerialListener;
import com.controlacceso.comunicacion_esp32.SerialManager;
import com.controlacceso.modelos.Empleado;
import com.controlacceso.modelos.GestorTurnos;
import com.controlacceso.modelos.RegistroAsistencia;
import com.controlacceso.modelos.ResultadoCalculoTurno;

import java.io.File;
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
        this(new EmpleadoDAOSqlite(), new AsistenciaDAOSqlite(), new GestorTurnos(), new SerialManager());
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
        LocalDateTime ahora = LocalDateTime.now();
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

        // Guardar en la base de datos
        asistenciaDAO.registrar(registro);

        // Responder al ESP32 para activar LEDs o Buzzer de confirmación
        if (serialManager.estaConectado()) {
            serialManager.enviarRespuesta(accesoPermitido, registro.getNombre());
        }

        return registro;
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
    // SERVICIOS PARA HISTORIAL Y REPORTES
    // =========================================================================

    public List<RegistroAsistencia> listarAsistenciasPorFiltros(String fecha, String nombre) {
        return asistenciaDAO.listarPorFiltros(fecha, nombre);
    }

    public boolean exportarReporteCsv(File archivo, List<RegistroAsistencia> registros) {
        return asistenciaDAO.exportarCsv(archivo, registros);
    }
}
