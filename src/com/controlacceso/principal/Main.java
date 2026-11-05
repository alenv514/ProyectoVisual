package com.controlacceso.principal;

import com.controlacceso.vistas.VentanaPrincipal;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.io.File;

/**
 * Punto de entrada principal de la aplicación de escritorio.
 * Paquete: com.controlacceso.principal
 */
public class Main {

    static {
        // Configurar la ruta nativa fija para que jSerialComm cargue directamente
        // el DLL x86_64 correcto y no intente extraer versiones en Temp.
        try {
            File dllDir = new File("lib/native");
            if (dllDir.exists()) {
                System.setProperty("jSerialComm.library.path", dllDir.getAbsolutePath());
            }
        } catch (Throwable t) {
            System.err.println("Aviso al configurar ruta serial: " + t.getMessage());
        }
    }

    public static void main(String[] args) {
        // Establecer apariencia visual nativa del sistema operativo (Windows)
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("No se pudo establecer el Look and Feel del sistema: " + e.getMessage());
        }

        // Ejecutar la interfaz gráfica en el Event Dispatch Thread (EDT) de Swing
        SwingUtilities.invokeLater(() -> {
            VentanaPrincipal ventana = new VentanaPrincipal();
            ventana.setVisible(true);
        });
    }
}
