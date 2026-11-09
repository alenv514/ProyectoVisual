package com.controlacceso.modelos;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Gestor encargado de seleccionar el Turno y calcular la puntualidad:
 * - Mañana (2 principales): Entrada Mañana (07:00) y Salida Mañana (13:00)
 * - Intermedio (13:00 a 14:00 o tras Salida Mañana): Entrada Anticipada (14:00, $0.00)
 * - Tarde (2 principales): Entrada Tarde (14:00) y Salida Tarde (20:00)
 * - Noche (20:00 en adelante tras Salida Tarde): Entrada Anticipada ($0.00)
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
                    if (t.contains("ENTRADA TARDE") || t.contains("ENTRADA ANTICIPADA")) tieneEntradaTarde = true;
                    if (t.contains("SALIDA TARDE"))   tieneSalidaTarde   = true;
                }
            }
        }

        // 1. Horario de la Mañana (antes de las 13:00 / 1:00 PM)
        if (horaActual.isBefore(LocalTime.of(13, 0))) {
            if (!tieneEntradaManana) {
                return turnoManana.evaluarEntrada(horaActual);
            } else if (!tieneSalidaManana) {
                return turnoManana.evaluarSalida(horaActual);
            } else {
                // Ya completó sus 2 marcas de la mañana y vuelve a ingresar
                return new ResultadoCalculoTurno("ENTRADA ANTICIPADA", "14:00", 0, 0.0, "A TIEMPO ($0.00)");
            }
        }
        // 2. Horario de 1:00 PM a 2:00 PM (13:00 a 14:00)
        else if (horaActual.isBefore(LocalTime.of(14, 0))) {
            if (tieneEntradaManana && !tieneSalidaManana) {
                return turnoManana.evaluarSalida(horaActual);
            } else {
                return new ResultadoCalculoTurno("ENTRADA ANTICIPADA", "14:00", 0, 0.0, "A TIEMPO ($0.00)");
            }
        }
        // 3. Horario de la Tarde (14:00 a 20:00 / 2:00 PM a 8:00 PM)
        else if (horaActual.isBefore(LocalTime.of(20, 0))) {
            if (!tieneEntradaTarde) {
                return turnoTarde.evaluarEntrada(horaActual);
            } else if (!tieneSalidaTarde) {
                return turnoTarde.evaluarSalida(horaActual);
            } else {
                return new ResultadoCalculoTurno("ENTRADA ANTICIPADA", "20:00", 0, 0.0, "A TIEMPO ($0.00)");
            }
        }
        // 4. De las 8:00 PM en adelante (20:00 en adelante)
        else {
            if (tieneEntradaTarde && !tieneSalidaTarde) {
                return turnoTarde.evaluarSalida(horaActual);
            } else {
                return new ResultadoCalculoTurno("ENTRADA ANTICIPADA", "-", 0, 0.0, "A TIEMPO ($0.00)");
            }
        }
    }
}
