package com.controlacceso.basededatos;

import conexion.Conexion;
import com.controlacceso.modelos.Empleado;
import com.controlacceso.modelos.RelojSistema;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación MySQL (XAMPP) del acceso a datos de Empleados.
 */
public class EmpleadoDAOMySql implements EmpleadoDAO {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public boolean guardar(Empleado empleado) {
        String sql = "INSERT INTO usuarios (uid, nombre, cedula, departamento, cargo, activo, creado_en) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = Conexion.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, empleado.getUidTarjeta().toUpperCase().trim());
            pstmt.setString(2, empleado.getNombre().trim());
            pstmt.setString(3, empleado.getCedula() != null ? empleado.getCedula().trim() : "");
            pstmt.setString(4, empleado.getDepartamento() != null ? empleado.getDepartamento().trim() : "");
            pstmt.setString(5, empleado.getCargo() != null ? empleado.getCargo().trim() : "");
            pstmt.setInt(6, empleado.isActivo() ? 1 : 0);
            pstmt.setString(7, RelojSistema.ahora().format(FORMATTER));

            int filas = pstmt.executeUpdate();
            if (filas > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        empleado.setId(rs.getInt(1));
                    }
                }
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("Error al guardar empleado en MySQL: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean actualizar(Empleado empleado) {
        String sql = "UPDATE usuarios SET uid = ?, nombre = ?, cedula = ?, departamento = ?, cargo = ?, activo = ? WHERE id = ?";
        try (Connection conn = Conexion.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, empleado.getUidTarjeta().toUpperCase().trim());
            pstmt.setString(2, empleado.getNombre().trim());
            pstmt.setString(3, empleado.getCedula() != null ? empleado.getCedula().trim() : "");
            pstmt.setString(4, empleado.getDepartamento() != null ? empleado.getDepartamento().trim() : "");
            pstmt.setString(5, empleado.getCargo() != null ? empleado.getCargo().trim() : "");
            pstmt.setInt(6, empleado.isActivo() ? 1 : 0);
            pstmt.setInt(7, empleado.getId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al actualizar empleado en MySQL: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean eliminar(int id) {
        String sql = "DELETE FROM usuarios WHERE id = ?";
        try (Connection conn = Conexion.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al eliminar empleado en MySQL: " + e.getMessage());
            return false;
        }
    }

    @Override
    public Empleado buscarPorId(int id) {
        String sql = "SELECT * FROM usuarios WHERE id = ?";
        try (Connection conn = Conexion.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapearEmpleado(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al buscar empleado por ID en MySQL: " + e.getMessage());
        }
        return null;
    }

    @Override
    public Empleado buscarPorUid(String uid) {
        if (uid == null) return null;
        String sql = "SELECT * FROM usuarios WHERE UPPER(TRIM(uid)) = UPPER(TRIM(?))";
        try (Connection conn = Conexion.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uid.toUpperCase().trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapearEmpleado(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al buscar empleado por UID en MySQL: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<Empleado> listarTodos() {
        List<Empleado> lista = new ArrayList<>();
        String sql = "SELECT * FROM usuarios ORDER BY nombre ASC";
        try (Connection conn = Conexion.getConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(mapearEmpleado(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error al listar empleados en MySQL: " + e.getMessage());
        }
        return lista;
    }

    private Empleado mapearEmpleado(ResultSet rs) throws SQLException {
        Empleado e = new Empleado();
        e.setId(rs.getInt("id"));
        e.setUidTarjeta(rs.getString("uid"));
        e.setNombre(rs.getString("nombre"));
        e.setCedula(rs.getString("cedula"));
        e.setDepartamento(rs.getString("departamento"));
        e.setCargo(rs.getString("cargo"));
        e.setActivo(rs.getInt("activo") == 1);
        e.setCreadoEn(rs.getString("creado_en"));
        return e;
    }
}
