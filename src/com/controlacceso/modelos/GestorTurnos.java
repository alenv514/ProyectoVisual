package com.controlacceso.modelos;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class GestorTurnos {

    private final Turno turnoManana = new TurnoManana();
    private final Turno turnoTarde = new TurnoTarde();

    public ResultadoCalculoTurno calcularMarcacion(LocalDateTime momento, List<RegistroAsistencia> marcasHoy) {
        LocalTime horaActual = momento.toLocalTime();

        boolean tieneEntradaManana = false;
        boolean tieneSalidaManana  = false;
        boolean tieneEntradaTarde  = false;
        boolean tieneSalidaTarde   = false;

        if (marcasHoy != null) {
            for (RegistroAsistencia m : marcasHoy) {
                if ("ACCESO DENEGADO".equalsIgnoreCase(m.getEstado())) {
                    continue;
                }
                String t = m.getTipo();
                if (t == null) continue;

                String norm = Normalizer.normalize(t.toUpperCase(), Normalizer.Form.NFD)
                        .replaceAll("[^A-Z]", " ");

                boolean esEntrada = norm.contains("ENTRADA");
                boolean esSalida  = norm.contains("SALIDA");

                int horaMarca = -1;
                String fh = m.getFechaHora();
                if (fh != null && fh.length() >= 13) {
                    try {
                        String parteHora = fh.substring(11, 13);
                        horaMarca = Integer.parseInt(parteHora);
                    } catch (Exception ignored) {}
                }

                boolean esManana = norm.contains("MA") || (horaMarca >= 0 && horaMarca < 13);
                boolean esTarde  = norm.contains("TARDE") || norm.contains("ANTICIPADA") || (horaMarca >= 13);

                if (esManana) {
                    if (esEntrada) tieneEntradaManana = true;
                    if (esSalida)  tieneSalidaManana  = true;
                } else if (esTarde) {
                    if (esEntrada) tieneEntradaTarde = true;
                    if (esSalida)  tieneSalidaTarde  = true;
                }
            }
        }

        if (horaActual.isBefore(LocalTime.of(13, 0))) {
            if (!tieneEntradaManana) {
                return turnoManana.evaluarEntrada(horaActual);
            } else if (!tieneSalidaManana) {
                return turnoManana.evaluarSalida(horaActual);
            } else {
                return new ResultadoCalculoTurno("REQUIERE AUTORIZACIÓN ADMIN", "-", 0, 0.0, "LÍMITE ALCANZADO: ESPERE AL ADMIN", true);
            }
        }
        else if (horaActual.isBefore(LocalTime.of(14, 0))) {
            if (tieneEntradaManana && !tieneSalidaManana) {
                return turnoManana.evaluarSalida(horaActual);
            } else if (!tieneEntradaTarde) {
                return new ResultadoCalculoTurno("ENTRADA ANTICIPADA", "14:00", 0, 0.0, "A TIEMPO ($0.00)");
            } else {
                return new ResultadoCalculoTurno("REQUIERE AUTORIZACIÓN ADMIN", "-", 0, 0.0, "LÍMITE ALCANZADO: ESPERE AL ADMIN", true);
            }
        }
        else if (horaActual.isBefore(LocalTime.of(20, 0))) {
            if (!tieneEntradaTarde) {
                return turnoTarde.evaluarEntrada(horaActual);
            } else if (!tieneSalidaTarde) {
                return turnoTarde.evaluarSalida(horaActual);
            } else {
                return new ResultadoCalculoTurno("REQUIERE AUTORIZACIÓN ADMIN", "-", 0, 0.0, "LÍMITE ALCANZADO: ESPERE AL ADMIN", true);
            }
        }
        else {
            if (tieneEntradaTarde && !tieneSalidaTarde) {
                return turnoTarde.evaluarSalida(horaActual);
            } else if (!tieneEntradaTarde) {
                return new ResultadoCalculoTurno("ENTRADA ANTICIPADA", "-", 0, 0.0, "A TIEMPO ($0.00)");
            } else {
                return new ResultadoCalculoTurno("REQUIERE AUTORIZACIÓN ADMIN", "-", 0, 0.0, "LÍMITE ALCANZADO: ESPERE AL ADMIN", true);
            }
        }
    }
}
