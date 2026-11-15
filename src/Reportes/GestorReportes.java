package Reportes;

import conexion.Conexion;
import java.awt.Component;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JOptionPane;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRResultSetDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.view.JasperViewer;

public class GestorReportes {

    private static final String RUTA_JASPER = "/Reportes/ReporteAsistencias.jasper";
    private static final String RUTA_JRXML = "/Reportes/ReporteAsistencias.jrxml";

    public static void mostrarReporteAsistencias(Component padre) {
        mostrarReporteAsistencias(padre, null, null, null);
    }

    public static void mostrarReporteAsistencias(Component padre, String buscar, String tipo, String estado) {
        try {
            JasperPrint print = generarReporteAsistencias(buscar, tipo, estado);

            if (print == null || print.getPages().isEmpty()) {
                JOptionPane.showMessageDialog(padre,
                        "No existen registros de asistencias con los filtros seleccionados para mostrar.",
                        "Reporte Vacío",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            JasperViewer visor = new JasperViewer(print, false);
            visor.setTitle("Reporte Oficial de Asistencias y Accesos RFID");
            visor.setVisible(true);

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(padre,
                    "Error de base de datos al generar el reporte:\n" + ex.getMessage(),
                    "Error SQL",
                    JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        } catch (JRException ex) {
            JOptionPane.showMessageDialog(padre,
                    "Error en el motor JasperReports:\n" + ex.getMessage(),
                    "Error JasperReports",
                    JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(padre,
                    "Ocurrió un error inesperado al generar el reporte:\n" + ex.getMessage(),
                    "Error General",
                    JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    public static JasperPrint generarReporteAsistencias(String buscar, String tipo, String estado) throws Exception {
        StringBuilder sql = new StringBuilder("""
            SELECT id, fecha_hora, uid, nombre, departamento, tipo, estado,
                   horario_esperado, minutos_diferencia, multa, detalle_puntualidad
            FROM asistencias
            WHERE 1=1
        """);

        List<Object> paramsList = new ArrayList<>();

        if (buscar != null && !buscar.isBlank()) {
            sql.append(" AND (LOWER(nombre) LIKE ? OR LOWER(uid) LIKE ?)");
            String term = "%" + buscar.trim().toLowerCase() + "%";
            paramsList.add(term);
            paramsList.add(term);
        }

        if (tipo != null && !tipo.isBlank() && !tipo.equalsIgnoreCase("TODOS")) {
            sql.append(" AND UPPER(TRIM(tipo)) = UPPER(TRIM(?))");
            paramsList.add(tipo.trim());
        }

        if (estado != null && !estado.isBlank() && !estado.equalsIgnoreCase("TODOS")) {
            sql.append(" AND UPPER(TRIM(estado)) = UPPER(TRIM(?))");
            paramsList.add(estado.trim());
        }

        sql.append(" ORDER BY id DESC");

        try (Connection con = Conexion.getConexion()) {
            if (con == null || con.isClosed()) {
                throw new SQLException("No se pudo establecer conexión con la base de datos MySQL.");
            }

            try (PreparedStatement ps = con.prepareStatement(sql.toString())) {
                for (int i = 0; i < paramsList.size(); i++) {
                    ps.setObject(i + 1, paramsList.get(i));
                }

                try (ResultSet rs = ps.executeQuery()) {
                    JRResultSetDataSource dataSource = new JRResultSetDataSource(rs);
                    Map<String, Object> params = new HashMap<>();

                    InputStream streamJasper = GestorReportes.class.getResourceAsStream(RUTA_JASPER);
                    if (streamJasper != null) {
                        return JasperFillManager.fillReport(streamJasper, params, dataSource);
                    } else {
                        InputStream streamJrxml = GestorReportes.class.getResourceAsStream(RUTA_JRXML);
                        if (streamJrxml == null) {
                            throw new JRException("No se encontró la plantilla del reporte (" + RUTA_JRXML + ").");
                        }
                        JasperReport reporteCompilado = JasperCompileManager.compileReport(streamJrxml);
                        return JasperFillManager.fillReport(reporteCompilado, params, dataSource);
                    }
                }
            }
        }
    }
}
