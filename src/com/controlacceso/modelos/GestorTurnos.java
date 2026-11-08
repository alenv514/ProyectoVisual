package com.controlacceso.modelos;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Gestor encargado de seleccionar el Turno y calcular la puntualidad
 * según la secuencia de marcaciones del empleado en el día.
 */
public class GestorTurnos {

    private final Turno turnoManana = new TurnoManana();
    private final Turno turnoTarde = new TurnoTarde();

    /**
     * Calcula el resultado de la marcación aplicando el turno correspondiente.
     */
    public ResultadoCalculoTurno calcularMarcacion(LocalDateTime momento, List<RegistroAsistencia> marcasHoy) {
        LocalTime horaActual = momento.toLocalTime();

        boolean tieneEntradaManana = false;
        boolean tieneSalidaManana  = false;
        boolean tieneEntradaTarde  = false;
        boolean tieneSalidaTarde   = false;

        if (marcasHoy != null) {
            for (RegistroAsistencia m : marcasHoy) {
                String t = m.getTipo();
                if (t != null) {
                    if (t.contains("ENTRADA MAÑANA")) tieneEntradaManana = true;
                    if (t.contains("SALIDA MAÑANA"))  tieneSalidaManana  = true;
                    if (t.contains("ENTRADA TARDE"))  tieneEntradaTarde  = true;
                    if (t.contains("SALIDA TARDE"))   tieneSalidaTarde   = true;
                }
            }
        }

        if (horaActual.isBefore(LocalTime.of(13, 30))) {
            // Horario Matutino (07:00 a 13:00)
            if (!tieneEntradaManana) {
                return turnoManana.evaluarEntrada(horaActual);
            } else if (!tieneSalidaManana) {
                return turnoManana.evaluarSalida(horaActual);
            } else {
                return new ResultadoCalculoTurno("ENTRADA ADICIONAL", "-", 0, 0.0, "A TIEMPO ($0.00)");
            }
        } else {
            // Horario Vespertino (14:00 a 20:00)
            if (!tieneEntradaTarde) {
                return turnoTarde.evaluarEntrada(horaActual);
            } else if (!tieneSalidaTarde) {
                return turnoTarde.evaluarSalida(horaActual);
            } else {
                return new ResultadoCalculoTurno("ENTRADA ADICIONAL", "-", 0, 0.0, "A TIEMPO ($0.00)");
            }
        }
    }
}
