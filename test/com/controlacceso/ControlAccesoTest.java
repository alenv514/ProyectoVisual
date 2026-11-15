/*package com.controlacceso;

import com.controlacceso.basededatos.AsistenciaDAO;
import com.controlacceso.basededatos.AsistenciaDAOSqlite;
import com.controlacceso.basededatos.ConexionBD;
import com.controlacceso.basededatos.EmpleadoDAO;
import com.controlacceso.basededatos.EmpleadoDAOSqlite;
import com.controlacceso.controlador.ControladorAcceso;
import com.controlacceso.modelos.Empleado;
import com.controlacceso.modelos.Persona;
import com.controlacceso.modelos.RegistroAsistencia;
import com.controlacceso.modelos.ResultadoCalculoTurno;
import com.controlacceso.modelos.Turno;
import com.controlacceso.modelos.TurnoManana;
import com.controlacceso.modelos.TurnoTarde;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ControlAccesoTest {

    private final EmpleadoDAO empleadoDAO = new EmpleadoDAOSqlite();
    private final AsistenciaDAO asistenciaDAO = new AsistenciaDAOSqlite();
    private final ControladorAcceso controlador = new ControladorAcceso();

    private final String UID_PRUEBA = "TEST_POO_123";

    @BeforeEach
    void limpiarPruebasAnteriores() {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM usuarios WHERE uid = ?")) {
                pstmt.setString(1, UID_PRUEBA);
                pstmt.executeUpdate();
            }
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM asistencias WHERE uid = ?")) {
                pstmt.setString(1, UID_PRUEBA);
                pstmt.executeUpdate();
            }
        } catch (Exception e) {
            fail("Error al limpiar base de datos de pruebas: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("POO: Verificar principio de Herencia (Empleado es una Persona)")
    void testHerenciaPersonaEmpleado() {
        Empleado emp = new Empleado(UID_PRUEBA, "Carlos Mendoza", "1720304050", "Tecnología", "Ingeniero");

        // Polimorfismo y herencia
        assertTrue(emp instanceof Persona, "Empleado debe ser una instancia de la clase abstracta Persona");
        assertEquals("Carlos Mendoza", emp.getNombre());
        assertEquals("1720304050", emp.getCedula());
        assertEquals("Tecnología", emp.getDepartamento());
        assertEquals(UID_PRUEBA, emp.getUidTarjeta());
        assertTrue(emp.getDescripcionRol().contains("Tecnología"));
    }

    @Test
    @DisplayName("POO: Verificar Polimorfismo en cálculo de Turnos y Multas ($3/hora)")
    void testPolimorfismoTurnos() {
        Turno turnoManana = new TurnoManana();
        Turno turnoTarde = new TurnoTarde();

        // 1. Entrada a las 07:20 AM (20 minutos de atraso -> $1.00 de multa)
        ResultadoCalculoTurno r1 = turnoManana.evaluarEntrada(LocalTime.of(7, 20));
        assertEquals("ENTRADA MAÑANA", r1.getTipo());
        assertEquals(20, r1.getMinutosDiferencia());
        assertEquals(1.00, r1.getMulta(), 0.01);
        assertTrue(r1.getDetallePuntualidad().contains("ATRASO"));

        // 2. Salida a las 12:30 PM (30 minutos antes de las 13:00 -> $1.50 de multa)
        ResultadoCalculoTurno r2 = turnoManana.evaluarSalida(LocalTime.of(12, 30));
        assertEquals("SALIDA MAÑANA", r2.getTipo());
        assertEquals(30, r2.getMinutosDiferencia());
        assertEquals(1.50, r2.getMulta(), 0.01);
        assertTrue(r2.getDetallePuntualidad().contains("SALIDA ANTICIPADA"));

        // 3. Entrada a las 13:55 (Antes de las 14:00 -> A tiempo, $0.00)
        ResultadoCalculoTurno r3 = turnoTarde.evaluarEntrada(LocalTime.of(13, 55));
        assertEquals("ENTRADA TARDE", r3.getTipo());
        assertEquals(0, r3.getMinutosDiferencia());
        assertEquals(0.00, r3.getMulta(), 0.01);
        assertTrue(r3.getDetallePuntualidad().contains("A TIEMPO"));
    }

    @Test
    @DisplayName("DAO: Operaciones CRUD de Empleados en SQLite")
    void testCrudEmpleadoDAO() {
        Empleado nuevo = new Empleado(UID_PRUEBA, "Ana Gomez", "0987654321", "Contabilidad", "Analista");
        boolean guardado = empleadoDAO.guardar(nuevo);
        assertTrue(guardado, "Debe guardar al empleado en la base de datos");
        assertTrue(nuevo.getId() > 0, "Debe generar un ID autonumérico");

        Empleado recuperado = empleadoDAO.buscarPorUid(UID_PRUEBA);
        assertNotNull(recuperado, "Debe encontrar al empleado por UID");
        assertEquals("Ana Gomez", recuperado.getNombre());

        recuperado.setNombre("Ana Gomez Modificada");
        boolean actualizado = empleadoDAO.actualizar(recuperado);
        assertTrue(actualizado, "Debe actualizar los datos del empleado");

        Empleado modificado = empleadoDAO.buscarPorId(recuperado.getId());
        assertEquals("Ana Gomez Modificada", modificado.getNombre());

        boolean eliminado = empleadoDAO.eliminar(modificado.getId());
        assertTrue(eliminado, "Debe eliminar al empleado");
        assertNull(empleadoDAO.buscarPorUid(UID_PRUEBA), "No debe existir tras eliminarlo");
    }

    @Test
    @DisplayName("Controlador: Procesamiento de lecturas RFID (Permitido vs Denegado)")
    void testControladorAccesoRfid() {
        // 1. Tarjeta no registrada -> Acceso Denegado
        RegistroAsistencia regDenegado = controlador.procesarLecturaRfid("UID_DESCONOCIDO_999");
        assertNotNull(regDenegado);
        assertEquals("ACCESO DENEGADO", regDenegado.getEstado());

        // 2. Registrar empleado y pasar tarjeta -> Acceso Permitido
        Empleado emp = new Empleado(UID_PRUEBA, "David Torres", "1102203300", "Operaciones", "Supervisor");
        empleadoDAO.guardar(emp);

        RegistroAsistencia regPermitido = controlador.procesarLecturaRfid(UID_PRUEBA);
        assertNotNull(regPermitido);
        assertEquals("ACCESO PERMITIDO", regPermitido.getEstado());
        assertEquals("David Torres", regPermitido.getNombre());
    }

    @Test
    @DisplayName("Reporte: Exportación a archivo CSV")
    void testExportacionCsv() {
        RegistroAsistencia r = new RegistroAsistencia();
        r.setUid(UID_PRUEBA);
        r.setNombre("Empleado CSV");
        r.setDepartamento("Ventas");
        r.setTipo("ENTRADA MAÑANA");
        r.setEstado("ACCESO PERMITIDO");
        r.setFechaHora("2026-11-05 07:15:00");
        r.setHorarioEsperado("07:00");
        r.setMinutosDiferencia(15);
        r.setMulta(0.75);
        r.setDetallePuntualidad("ATRASO: 15 min");

        asistenciaDAO.registrar(r);

        File tempFile = new File(System.getProperty("java.io.tmpdir"), "test_reporte_poo.csv");
        List<RegistroAsistencia> lista = asistenciaDAO.listarPorFiltros(null, "Empleado CSV");
        boolean exportado = asistenciaDAO.exportarCsv(tempFile, lista);

        assertTrue(exportado, "Debe exportar el archivo CSV correctamente");
        assertTrue(tempFile.exists() && tempFile.length() > 0, "El archivo CSV no debe estar vacío");
        tempFile.delete();
    }
}
*/