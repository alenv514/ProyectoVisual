package com.controlacceso.comunicacion_esp32;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortMessageListener;

import javax.swing.SwingUtilities;
import java.io.File;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestor de comunicación con la placa ESP32 por puerto COM serial.
 * Implementa auto-detección de puerto y escucha por eventos delimitados por '\n'.
 */
public class SerialManager {

    static {
        try {
            File dllDir = new File("lib/native");
            if (dllDir.exists()) {
                System.setProperty("jSerialComm.library.path", dllDir.getAbsolutePath());
            }
        } catch (Throwable ignored) {
        }
    }

    private SerialPort activePort;
    private Thread autoConnectThread;
    private volatile boolean autoConnectEnabled = false;
    private SerialListener listener;

    public void setListener(SerialListener listener) {
        this.listener = listener;
    }

    /**
     * Obtiene la lista de puertos COM disponibles, priorizando los adaptadores USB
     * (Silicon Labs CP210x, CH340, FTDI, Arduino) y descartando enlaces Bluetooth virtuales.
     */
    public static List<String> obtenerPuertosDisponibles() {
        List<String> puertosUsb = new ArrayList<>();
        List<String> otrosPuertos = new ArrayList<>();

        try {
            SerialPort[] comPorts = SerialPort.getCommPorts();
            if (comPorts != null) {
                for (SerialPort port : comPorts) {
                    if (port == null || port.getSystemPortName() == null) {
                        continue;
                    }
                    String desc = (port.getPortDescription() + " " + port.getDescriptivePortName()).toLowerCase();
                    if (desc.contains("bluetooth") || desc.contains("bth")) {
                        continue; // Ignorar enlaces Bluetooth virtuales
                    }
                    if (desc.contains("cp210") || desc.contains("uart") || desc.contains("usb") || desc.contains("silicon")) {
                        puertosUsb.add(port.getSystemPortName());
                    } else {
                        otrosPuertos.add(port.getSystemPortName());
                    }
                }
            }
        } catch (Throwable t) {
            System.err.println("Advertencia al escanear puertos seriales: " + t.getMessage());
        }

        List<String> resultado = new ArrayList<>(puertosUsb);
        resultado.addAll(otrosPuertos);
        return resultado;
    }