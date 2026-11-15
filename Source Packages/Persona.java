/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author Lenovo
 */
public class Persona {

    protected int id;
    protected String nombre;
    protected String cedula;

    public Persona() {
    }

    public Persona(int id, String nombre, String cedula) {
        this.id = id;
        this.nombre = nombre;
        this.cedula = cedula;
    }

    public Persona(String nombre, String cedula) {
        this.nombre = nombre;
        this.cedula = cedula;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCedula() {
        return cedula;
    }

    public void setCedula(String cedula) {
        this.cedula = cedula;
    }

    /**
     * Método polimórfico para obtener la descripción general de la persona.
     */
    public abstract String getDescripcionRol();

    @Override
    public String toString() {
        return "Persona{"
                + "id=" + id
                + ", nombre='" + nombre + '\''
                + ", cedula='" + cedula + '\''
                + '}';
    }
}
