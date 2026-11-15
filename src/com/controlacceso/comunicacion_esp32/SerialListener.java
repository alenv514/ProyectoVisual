package com.controlacceso.comunicacion_esp32;

public interface SerialListener {

    void onUidReceived(String uid);

    void onPortStateChanged(boolean conectado, String mensaje);

    void onLogMessage(String log);
}
