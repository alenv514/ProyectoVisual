package com.controlacceso.modelos;

import java.time.LocalTime;

/**
 * Subclase de Turno para el turno vespertino (02:00 PM / 14:00 a 08:00 PM / 20:00).
 */
public class TurnoTarde extends Turno {

    public TurnoTarde() {
        super("TARDE", LocalTime.of(14, 0), LocalTime.of(20, 0));
    }
}
