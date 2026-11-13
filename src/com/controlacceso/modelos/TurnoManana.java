package com.controlacceso.modelos;

import java.time.LocalTime;

/**
 * Subclase de Turno para el turno matutino (07:00 AM a 01:00 PM / 13:00).
 */
public class TurnoManana extends Turno {

    public TurnoManana() {
        super("MAÑANA", LocalTime.of(7, 0), LocalTime.of(13, 0));
    }
}
