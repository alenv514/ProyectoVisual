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
        int cantidadMarcas = marcasHoy != null ? marcasHoy.size() : 0;

        if (cantidadMarcas == 0) {
            // Primera marca del día: Entrada
            if (horaActual.isBefore(LocalTime.of(13, 30))) {
                return turnoManana.evaluarEntrada(horaActual);
            } else {
                return turnoTarde.evaluarEntrada(horaActual);
            }
        } else if (cantidadMarcas == 1) {
            // Segunda marca del día: Salida
            RegistroAsistencia primera = marcasHoy.get(0);
            if (primera.getTipo() != null && primera.getTipo().contains("TARDE")) {
                return turnoTarde.evaluarSalida(horaActual);
            } else {
                return turnoManana.evaluarSalida(horaActual);
            }
        } else if (cantidadMarcas == 2) {
            // Tercera marca del día: Entrada de la tarde
            return turnoTarde.evaluarEntrada(horaActual);
        } else if (cantidadMarcas == 3) {
            // Cuarta marca del día: Salida de la tarde
            return turnoTarde.evaluarSalida(horaActual);
        } else {
            // Marcas adicionales
            RegistroAsistencia ultima = marcasHoy.get(marcasHoy.size() - 1);
            if (ultima.getTipo() != null && ultima.getTipo().startsWith("ENTRADA")) {
                return turnoTarde.evaluarSalida(horaActual);
            } else {
                return new ResultadoCalculoTurno("ENTRADA ADICIONAL", "-", 0, 0.0, "A TIEMPO ($0.00)");
            }
        }
    }
}
