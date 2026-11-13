package com.controlacceso.modelos;

import java.time.Duration;
import java.time.LocalTime;

/**
 * Clase base abstracta que modela un turno laboral.
 * Aplica Herencia y Polimorfismo para calcular puntualidad y multas.
 */
public abstract class Turno {

    public static final double TARIFA_POR_HORA = 3.00; // $3.00 USD por hora ($0.05/minuto)

    protected String nombreTurno;
    protected LocalTime horaEntradaEsperada;
    protected LocalTime horaSalidaEsperada;

    public Turno(String nombreTurno, LocalTime horaEntradaEsperada, LocalTime horaSalidaEsperada) {
        this.nombreTurno = nombreTurno;
        this.horaEntradaEsperada = horaEntradaEsperada;
        this.horaSalidaEsperada = horaSalidaEsperada;
    }

    public String getNombreTurno() {
        return nombreTurno;
    }

    public LocalTime getHoraEntradaEsperada() {
        return horaEntradaEsperada;
    }

    public LocalTime getHoraSalidaEsperada() {
        return horaSalidaEsperada;
    }

    /**
     * Evalúa una marcación de entrada (Polimorfismo).
     */
    public ResultadoCalculoTurno evaluarEntrada(LocalTime horaMarcacion) {
        ResultadoCalculoTurno res = new ResultadoCalculoTurno();
        res.setTipo("ENTRADA " + nombreTurno);
        res.setHorarioEsperado(horaEntradaEsperada.toString());

        if (horaMarcacion.isAfter(horaEntradaEsperada)) {
            long minutos = Duration.between(horaEntradaEsperada, horaMarcacion).toMinutes();
            double multa = calcularMulta(minutos);
            res.setMinutosDiferencia((int) minutos);
            res.setMulta(multa);
            res.setDetallePuntualidad(String.format("ATRASO: %d min (Multa: $%.2f)", minutos, multa));
        } else {
            res.setMinutosDiferencia(0);
            res.setMulta(0.0);
            res.setDetallePuntualidad("A TIEMPO ($0.00)");
        }
        return res;
    }

    /**
     * Evalúa una marcación de salida (Polimorfismo).
     */
    public ResultadoCalculoTurno evaluarSalida(LocalTime horaMarcacion) {
        ResultadoCalculoTurno res = new ResultadoCalculoTurno();
        res.setTipo("SALIDA " + nombreTurno);
        res.setHorarioEsperado(horaSalidaEsperada.toString());

        if (horaMarcacion.isBefore(horaSalidaEsperada)) {
            long minutos = Duration.between(horaMarcacion, horaSalidaEsperada).toMinutes();
            double multa = calcularMulta(minutos);
            res.setMinutosDiferencia((int) minutos);
            res.setMulta(multa);
            res.setDetallePuntualidad(String.format("SALIDA ANTICIPADA: %d min (Multa: $%.2f)", minutos, multa));
        } else {
            res.setMinutosDiferencia(0);
            res.setMulta(0.0);
            res.setDetallePuntualidad("A TIEMPO ($0.00)");
        }
        return res;
    }

    /**
     * Calcula la multa proporcional a $3.00 USD por hora.
     */
    protected double calcularMulta(long minutos) {
        double valor = (minutos / 60.0) * TARIFA_POR_HORA;
        return Math.round(valor * 100.0) / 100.0;
    }
}
