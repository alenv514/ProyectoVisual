package com.controlacceso;

import com.controlacceso.basededatos.AsistenciaDAO;
import com.controlacceso.basededatos.AsistenciaDAOMySql;
import conexion.Conexion;
import com.controlacceso.basededatos.EmpleadoDAO;
import com.controlacceso.basededatos.EmpleadoDAOMySql;
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ControlAccesoTest {

    private final EmpleadoDAO empleadoDAO = new EmpleadoDAOMySql();
    private final AsistenciaDAO asistenciaDAO = new AsistenciaDAOMySql();
    private final ControladorAcceso controlador = new ControladorAcceso();

    private final String UID_PRUEBA = "TEST_POO_123";

    @BeforeEach
    void limpiarPruebasAnteriores() {
        try (Connection conn = Conexion.getConexion()) {
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM usuarios WHERE uid LIKE ?")) {
                pstmt.setString(1, "TEST_%");
                pstmt.executeUpdate();
            }
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM asistencias WHERE uid LIKE ? OR uid LIKE ?")) {
                pstmt.setString(1, "TEST_%");
                pstmt.setString(2, "UID_DESCONOCIDO%");
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
    @DisplayName("DAO: Operaciones CRUD de Empleados en MySQL")
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
    @DisplayName("Reportes: Verificación de plantilla JasperReports")
    void testPlantillaJasperReports() {
        assertNotNull(getClass().getResourceAsStream("/Reportes/ReporteAsistencias.jasper"),
                "El archivo compilado ReporteAsistencias.jasper debe existir en el classpath");
    }

    @Test
    @DisplayName("RelojSistema: Verificación de reloj independiente y monotónico")
    void testRelojSistemaMonotonico() throws InterruptedException {
        java.time.LocalDateTime t1 = com.controlacceso.modelos.RelojSistema.ahora();
        Thread.sleep(50);
        java.time.LocalDateTime t2 = com.controlacceso.modelos.RelojSistema.ahora();
        assertTrue(t2.isAfter(t1) || t2.isEqual(t1), "El tiempo del reloj independiente debe avanzar continuamente");
        assertNotNull(com.controlacceso.modelos.RelojSistema.getHoraFormateada());
    }

    @Test
    @DisplayName("Seguridad: Autenticación de Administrador")
    void testAutenticacionAdmin() {
        assertTrue(com.controlacceso.autenticacion.AdminAuthService.autenticar("admin", "admin123"),
                "Debe autenticar correctamente al admin con credenciales válidas");
        assertTrue(com.controlacceso.autenticacion.AdminAuthService.autenticar("admin", "admin"),
                "Debe aceptar clave de respaldo admin");
        assertFalse(com.controlacceso.autenticacion.AdminAuthService.autenticar("admin", "clave_invalida"),
                "Debe denegar acceso con clave incorrecta");
        assertFalse(com.controlacceso.autenticacion.AdminAuthService.autenticar("hacker", "admin123"),
                "Debe denegar acceso con usuario desconocido");
    }

    @Test
    @DisplayName("DAO / Filtros: Búsqueda de asistencias por Tipo y Estado")
    void testFiltrosAsistenciaTipoYEstado() {
        // Registrar empleado y marcar acceso permitido
        Empleado emp = new Empleado(UID_PRUEBA, "Filtro Test User", "0999999999", "Sistemas", "Tester");
        empleadoDAO.guardar(emp);

        RegistroAsistencia regPermitido = controlador.procesarLecturaRfid(UID_PRUEBA);
        assertNotNull(regPermitido);

        // Generar una marcación denegada con UID temporal
        controlador.procesarLecturaRfid(UID_PRUEBA + "_DENEGADO");

        // 1. Filtrar solo permitidos para este usuario
        List<RegistroAsistencia> permitidos = controlador.listarAsistenciasPorFiltros(null, "Filtro Test User", "TODOS", "ACCESO PERMITIDO");
        assertFalse(permitidos.isEmpty(), "Debe encontrar registros permitidos");
        assertTrue(permitidos.stream().allMatch(r -> "ACCESO PERMITIDO".equalsIgnoreCase(r.getEstado())));

        // 2. Filtrar solo denegados
        List<RegistroAsistencia> denegados = controlador.listarAsistenciasPorFiltros(null, UID_PRUEBA + "_DENEGADO", "TODOS", "ACCESO DENEGADO");
        assertFalse(denegados.isEmpty(), "Debe encontrar registros denegados");
        assertTrue(denegados.stream().allMatch(r -> "ACCESO DENEGADO".equalsIgnoreCase(r.getEstado())));

        // 3. Filtrar por tipo específico que no coincida (debe dar vacío)
        List<RegistroAsistencia> vacio = controlador.listarAsistenciasPorFiltros(null, UID_PRUEBA, "TIPO_INEXISTENTE", "ACCESO PERMITIDO");
        assertTrue(vacio.isEmpty(), "Filtro no coincidente debe retornar lista vacía");

        // Limpieza extra
        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM asistencias WHERE uid LIKE ?")) {
            ps.setString(1, UID_PRUEBA + "%");
            ps.executeUpdate();
        } catch (Exception ignored) {}
    }
}
