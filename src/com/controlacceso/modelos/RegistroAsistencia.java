package com.controlacceso.modelos;

/**
 * Representa una marcación de asistencia individual en el sistema.
 */
public class RegistroAsistencia {

    private int id;
    private Integer usuarioId;
    private String uid;
    private String nombre;
    private String departamento;
    private String tipo;               // ENTRADA MAÑANA, SALIDA MAÑANA, ENTRADA TARDE, SALIDA TARDE
    private String estado;             // ACCESO PERMITIDO, ACCESO DENEGADO
    private String fechaHora;          // yyyy-MM-dd HH:mm:ss
    private String horarioEsperado;    // 07:00, 13:00, 14:00, 20:00
    private int minutosDiferencia;     // Minutos de retraso o de anticipación
    private double multa;              // Valor en USD ($3.00/hora = $0.05/min)
    private String detallePuntualidad; // "A TIEMPO", "ATRASO: 20 min (Multa: $1.00)"

    public RegistroAsistencia() {
        this.horarioEsperado = "-";
        this.minutosDiferencia = 0;
        this.multa = 0.0;
        this.detallePuntualidad = "-";
    }

    public RegistroAsistencia(int id, Integer usuarioId, String uid, String nombre, String departamento,
                              String tipo, String estado, String fechaHora,
                              String horarioEsperado, int minutosDiferencia, double multa, String detallePuntualidad) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.uid = uid;
        this.nombre = nombre;
        this.departamento = departamento;
        this.tipo = tipo;
        this.estado = estado;
        this.fechaHora = fechaHora;
        this.horarioEsperado = horarioEsperado;
        this.minutosDiferencia = minutosDiferencia;
        this.multa = multa;
        this.detallePuntualidad = detallePuntualidad;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Integer usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDepartamento() {
        return departamento;
    }

    public void setDepartamento(String departamento) {
        this.departamento = departamento;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(String fechaHora) {
        this.fechaHora = fechaHora;
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
