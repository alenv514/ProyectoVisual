package com.controlacceso.comunicacion_esp32;

/**
 * Interfaz de escucha para los eventos de la comunicación serial con el ESP32.
 */
public interface SerialListener {

    void onUidReceived(String uid);

    void onPortStateChanged(boolean conectado, String mensaje);

    void onLogMessage(String log);
}
