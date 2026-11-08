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

    public void iniciarAutoConexion() {
        if (autoConnectEnabled) return;
        autoConnectEnabled = true;

        autoConnectThread = new Thread(() -> {
            while (autoConnectEnabled) {
                try {
                    if (activePort != null && activePort.isOpen()) {
                        // Verificar si el puerto sigue conectado físicamente a Windows
                        boolean siguePresente = false;
                        for (String p : obtenerPuertosDisponibles()) {
                            if (p.equalsIgnoreCase(activePort.getSystemPortName())) {
                                siguePresente = true;
                                break;
                            }
                        }
                        if (!siguePresente) {
                            desconectar();
                        }
                    } else {
                        List<String> puertos = obtenerPuertosDisponibles();
                        boolean conectado = false;
                        for (String puerto : puertos) {
                            if (conectar(puerto, 115200)) {
                                conectado = true;
                                break;
                            }
                        }
                        if (!conectado) {
                            notificarEstado(false, "Buscando lector RFID...");
                        }
                    }

                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    break;
                } catch (Throwable t) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ignored) {
                        break;
                    }
                }
            }
        }, "SerialAutoConnectThread");

        autoConnectThread.setDaemon(true);
        autoConnectThread.start();
    }

    public void detenerAutoConexion() {
        autoConnectEnabled = false;
        if (autoConnectThread != null) {
            autoConnectThread.interrupt();
            autoConnectThread = null;
        }
        desconectar();
    }

    public synchronized boolean conectar(String portName, int baudRate) {
        if (activePort != null && activePort.isOpen()) {
            if (portName.equalsIgnoreCase(activePort.getSystemPortName())) {
                return true; // Ya conectado a este puerto
            }
            desconectar();
        }

        try {
            activePort = SerialPort.getCommPort(portName);
            activePort.setBaudRate(baudRate);
            activePort.setNumDataBits(8);
            activePort.setNumStopBits(SerialPort.ONE_STOP_BIT);
            activePort.setParity(SerialPort.NO_PARITY);
            activePort.clearDTR();
            activePort.clearRTS();

            if (!activePort.openPort()) {
                activePort = null;
                return false;
            }

            // Usar listener de eventos con delimitador de fin de línea (\n)
            activePort.addDataListener(new SerialPortMessageListener() {
                @Override
                public int getListeningEvents() {
                    return SerialPort.LISTENING_EVENT_DATA_RECEIVED;
                }

                @Override
                public byte[] getMessageDelimiter() {
                    return new byte[]{'\n'};
                }

                @Override
                public boolean delimiterIndicatesEndOfMessage() {
                    return true;
                }

                @Override
                public void serialEvent(SerialPortEvent event) {
                    byte[] data = event.getReceivedData();
                    if (data != null && data.length > 0) {
                        String linea = new String(data, StandardCharsets.UTF_8).trim();
                        if (!linea.isEmpty()) {
                            procesarLinea(linea);
                        }
                    }
                }
            });

            notificarEstado(true, "Lector RFID Conectado (" + portName + ")");
            return true;
        } catch (Exception e) {
            activePort = null;
            return false;
        }
    }

    public synchronized void desconectar() {
        if (activePort != null) {
            try {
                activePort.removeDataListener();
            } catch (Throwable ignored) {}

            if (activePort.isOpen()) {
                try {
                    activePort.closePort();
                } catch (Throwable ignored) {}
            }
            activePort = null;
            notificarEstado(false, "Buscando lector RFID...");
        }
    }

    public boolean estaConectado() {
        return activePort != null && activePort.isOpen();
    }

    public synchronized boolean enviarComando(String comando) {
        if (!estaConectado()) {
            return false;
        }
        try {
            OutputStream out = activePort.getOutputStream();
            String linea = comando.endsWith("\n") ? comando : comando + "\n";
            byte[] bytes = linea.getBytes(StandardCharsets.UTF_8);
            out.write(bytes);
            out.flush();
            notificarLog("TX -> " + comando.trim());
            return true;
        } catch (Exception e) {
            notificarLog("Error al enviar comando: " + e.getMessage());
            return false;
        }
    }

    public void enviarRespuesta(boolean permitido, String nombre) {
        String msg = (permitido ? "OK:" : "ERR:") + (nombre != null ? nombre : "DESCONOCIDO");
        enviarComando(msg);
    }

    private void procesarLinea(String linea) {
        notificarLog("RX <- " + linea);

        String uidExtraido = null;
        String upper = linea.toUpperCase();

        if (upper.startsWith("UID:")) {
            uidExtraido = upper.substring(4).replaceAll("[^A-F0-9]", "");
        } else if (upper.matches("([0-9A-F]{2}\\s*)+") && upper.replaceAll("\\s+", "").length() >= 4) {
            uidExtraido = upper.replaceAll("\\s+", "");
        }

        if (uidExtraido != null && !uidExtraido.isEmpty()) {
            final String finalUid = uidExtraido;
            SwingUtilities.invokeLater(() -> {
                if (listener != null) {
                    listener.onUidReceived(finalUid);
                }
            });
        }
    }

    private void notificarEstado(boolean conectado, String mensaje) {
        SwingUtilities.invokeLater(() -> {
            if (listener != null) {
                listener.onPortStateChanged(conectado, mensaje);
            }
        });
    }

    private void notificarLog(String log) {
        SwingUtilities.invokeLater(() -> {
            if (listener != null) {
                listener.onLogMessage(log);
            }
        });
    }
}
