package Reportes;

import conexion.Conexion;
import java.awt.Component;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JOptionPane;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.view.JasperViewer;

/**
 * Gestor encargado de la compilación, llenado y visualización de reportes JasperReports.
 */
public class GestorReportes {

    private static final String RUTA_JASPER = "/Reportes/ReporteAsistencias.jasper";
    private static final String RUTA_JRXML = "/Reportes/ReporteAsistencias.jrxml";

    /**
     * Genera y visualiza en pantalla el reporte oficial de asistencias.
     *
     * @param padre Componente visual padre para modales y diálogos.
     */
    public static void mostrarReporteAsistencias(Component padre) {
        Connection con = null;
        try {
            con = Conexion.getConexion();
            if (con == null || con.isClosed()) {
                JOptionPane.showMessageDialog(padre,
                        "No se pudo establecer conexión con la base de datos MySQL.",
                        "Error de Conexión",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            JasperPrint print = null;
            Map<String, Object> params = new HashMap<>();

            // 1. Intentar cargar el archivo .jasper precompilado
            InputStream streamJasper = GestorReportes.class.getResourceAsStream(RUTA_JASPER);
            if (streamJasper != null) {
                print = JasperFillManager.fillReport(streamJasper, params, con);
            } else {
                // 2. Si no existe .jasper, compilar el .jrxml en tiempo de ejecución
                InputStream streamJrxml = GestorReportes.class.getResourceAsStream(RUTA_JRXML);
                if (streamJrxml == null) {
                    JOptionPane.showMessageDialog(padre,
                            "No se encontró la plantilla del reporte (" + RUTA_JRXML + ").",
                            "Plantilla no encontrada",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                JasperReport reporteCompilado = JasperCompileManager.compileReport(streamJrxml);
                print = JasperFillManager.fillReport(reporteCompilado, params, con);
            }

            if (print == null || print.getPages().isEmpty()) {
                JOptionPane.showMessageDialog(padre,
                        "No existen registros de asistencias en la base de datos para mostrar.",
                        "Reporte Vacío",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // Visualizar el reporte con JasperViewer sin cerrar el programa al salir
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
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }
}
