package com.controlacceso.modelos;

/**
 * Clase que representa a un empleado de la organización.
 * Aplica el principio de Herencia extendiendo de Persona.
 */
public class Empleado extends Persona {

    private String uidTarjeta;
    private String departamento;
    private String cargo;
    private boolean activo;
    private String creadoEn;

    public Empleado() {
        super();
        this.activo = true;
    }

    public Empleado(int id, String uidTarjeta, String nombre, String cedula,
                    String departamento, String cargo, boolean activo, String creadoEn) {
        super(id, nombre, cedula);
        this.uidTarjeta = uidTarjeta;
        this.departamento = departamento;
        this.cargo = cargo;
        this.activo = activo;
        this.creadoEn = creadoEn;
    }

    public Empleado(String uidTarjeta, String nombre, String cedula,
                    String departamento, String cargo) {
        super(nombre, cedula);
        this.uidTarjeta = uidTarjeta;
        this.departamento = departamento;
        this.cargo = cargo;
        this.activo = true;
    }

    public String getUidTarjeta() {
        return uidTarjeta;
    }

    public void setUidTarjeta(String uidTarjeta) {
        this.uidTarjeta = uidTarjeta;
    }

    // Métodos alias para compatibilidad
    public String getUid() {
        return uidTarjeta;
    }

    public void setUid(String uid) {
        this.uidTarjeta = uid;
    }

    public String getDepartamento() {
        return departamento;
    }

    public void setDepartamento(String departamento) {
        this.departamento = departamento;
    }

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public String getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(String creadoEn) {
        this.creadoEn = creadoEn;
    }

    @Override
    public String getDescripcionRol() {
        return "Empleado [" + departamento + " - " + cargo + "]";
    }

    @Override
    public String toString() {
        return "Empleado{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                ", cedula='" + cedula + '\'' +
                ", uidTarjeta='" + uidTarjeta + '\'' +
                ", departamento='" + departamento + '\'' +
                ", cargo='" + cargo + '\'' +
                ", activo=" + activo +
                '}';
    }
}
