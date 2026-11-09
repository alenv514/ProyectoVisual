package com.controlacceso.modelos;

/**
 * Objeto DTO que encapsula el resultado del cálculo de puntualidad y multas.
 */
public class ResultadoCalculoTurno {

    private String tipo;
    private String horarioEsperado;
    private int minutosDiferencia;
    private double multa;
    private String detallePuntualidad;
    private boolean requiereAutorizacionAdmin;

    public ResultadoCalculoTurno() {
        this.tipo = "-";
        this.horarioEsperado = "-";
        this.minutosDiferencia = 0;
        this.multa = 0.0;
        this.detallePuntualidad = "-";
        this.requiereAutorizacionAdmin = false;
    }

    public ResultadoCalculoTurno(String tipo, String horarioEsperado, int minutosDiferencia, double multa, String detallePuntualidad) {
        this(tipo, horarioEsperado, minutosDiferencia, multa, detallePuntualidad, false);
    }

    public ResultadoCalculoTurno(String tipo, String horarioEsperado, int minutosDiferencia, double multa, String detallePuntualidad, boolean requiereAutorizacionAdmin) {
        this.tipo = tipo;
        this.horarioEsperado = horarioEsperado;
        this.minutosDiferencia = minutosDiferencia;
        this.multa = multa;
        this.detallePuntualidad = detallePuntualidad;
        this.requiereAutorizacionAdmin = requiereAutorizacionAdmin;
    }

    public boolean isRequiereAutorizacionAdmin() {
        return requiereAutorizacionAdmin;
    }

    public void setRequiereAutorizacionAdmin(boolean requiereAutorizacionAdmin) {
        this.requiereAutorizacionAdmin = requiereAutorizacionAdmin;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getHorarioEsperado() {
        return horarioEsperado;
    }

    public void setHorarioEsperado(String horarioEsperado) {
        this.horarioEsperado = horarioEsperado;
    }

    public int getMinutosDiferencia() {
        return minutosDiferencia;
    }

    public void setMinutosDiferencia(int minutosDiferencia) {
        this.minutosDiferencia = minutosDiferencia;
    }

    public double getMulta() {
        return multa;
    }

    public void setMulta(double multa) {
        this.multa = multa;
    }

    public String getDetallePuntualidad() {
        return detallePuntualidad;
    }

    public void setDetallePuntualidad(String detallePuntualidad) {
        this.detallePuntualidad = detallePuntualidad;
    }
}
