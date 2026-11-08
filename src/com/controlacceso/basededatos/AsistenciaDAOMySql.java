package com.controlacceso.basededatos;

import conexion.Conexion;
import com.controlacceso.modelos.RegistroAsistencia;
import com.controlacceso.modelos.RelojSistema;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación MySQL (XAMPP) del acceso a datos para Registros de Asistencia.
 */
public class AsistenciaDAOMySql implements AsistenciaDAO {

    @Override
    public boolean registrar(RegistroAsistencia reg) {
        String sql = """
            INSERT INTO asistencias (
                usuario_id, uid, nombre, departamento, tipo, estado,
                fecha_hora, horario_esperado, minutos_diferencia, multa, detalle_puntualidad
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            if (reg.getUsuarioId() != null) {
                ps.setInt(1, reg.getUsuarioId());
            } else {
                ps.setNull(1, java.sql.Types.INTEGER);
            }
            ps.setString(2, reg.getUid());
            ps.setString(3, reg.getNombre());
            ps.setString(4, reg.getDepartamento());
            ps.setString(5, reg.getTipo());
            ps.setString(6, reg.getEstado());
            ps.setString(7, reg.getFechaHora());
            ps.setString(8, reg.getHorarioEsperado());
            ps.setInt(9, reg.getMinutosDiferencia());
            ps.setDouble(10, reg.getMulta());
            ps.setString(11, reg.getDetallePuntualidad());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        reg.setId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error al registrar asistencia en MySQL: " + e.getMessage());
        }
        return false;
    }

    @Override
    public List<RegistroAsistencia> listarHoy() {
        String hoy = RelojSistema.fechaActual().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return listarPorFiltros(hoy, null);
    }

    @Override
    public List<RegistroAsistencia> listarPorFiltros(String fecha, String nombre) {
        return listarPorFiltros(fecha, nombre, null, null);
    }

    @Override
    public List<RegistroAsistencia> listarPorFiltros(String fecha, String nombre, String tipo, String estado) {
        List<RegistroAsistencia> lista = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT id, usuario_id, uid, nombre, departamento, tipo, estado,
                   fecha_hora, horario_esperado, minutos_diferencia, multa, detalle_puntualidad
            FROM asistencias
            WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();

        if (fecha != null && !fecha.isBlank()) {
            sql.append(" AND fecha_hora LIKE ?");
            params.add(fecha.trim() + "%");
        }
        if (nombre != null && !nombre.isBlank()) {
            sql.append(" AND (LOWER(nombre) LIKE ? OR LOWER(uid) LIKE ?)");
            String term = "%" + nombre.trim().toLowerCase() + "%";
            params.add(term);
            params.add(term);
        }
        if (tipo != null && !tipo.isBlank() && !tipo.equalsIgnoreCase("TODOS")) {
            sql.append(" AND UPPER(TRIM(tipo)) = UPPER(TRIM(?))");
            params.add(tipo.trim());
        }
        if (estado != null && !estado.isBlank() && !estado.equalsIgnoreCase("TODOS")) {
            sql.append(" AND UPPER(TRIM(estado)) = UPPER(TRIM(?))");
            params.add(estado.trim());
        }

        sql.append(" ORDER BY id DESC");

        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearAsistencia(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al consultar asistencias en MySQL: " + e.getMessage());
        }
        return lista;
    }

    @Override
    public List<RegistroAsistencia> listarPorUsuarioYFecha(String uid, String fecha) {
        List<RegistroAsistencia> lista = new ArrayList<>();
        String sql = """
            SELECT id, usuario_id, uid, nombre, departamento, tipo, estado,
                   fecha_hora, horario_esperado, minutos_diferencia, multa, detalle_puntualidad
            FROM asistencias
            WHERE uid = ? AND fecha_hora LIKE ?
            ORDER BY id ASC
        """;

        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, uid);
            ps.setString(2, fecha + "%");

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearAsistencia(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al consultar asistencias por usuario y fecha en MySQL: " + e.getMessage());
        }
        return lista;
    }

    private RegistroAsistencia mapearAsistencia(ResultSet rs) throws SQLException {
        RegistroAsistencia r = new RegistroAsistencia();
        r.setId(rs.getInt("id"));
        int usuarioId = rs.getInt("usuario_id");
        r.setUsuarioId(rs.wasNull() ? null : usuarioId);
        r.setUid(rs.getString("uid"));
        r.setNombre(rs.getString("nombre"));
        r.setDepartamento(rs.getString("departamento"));
        r.setTipo(rs.getString("tipo"));
        r.setEstado(rs.getString("estado"));
        r.setFechaHora(rs.getString("fecha_hora"));
        r.setHorarioEsperado(rs.getString("horario_esperado"));
        r.setMinutosDiferencia(rs.getInt("minutos_diferencia"));
        r.setMulta(rs.getDouble("multa"));
        r.setDetallePuntualidad(rs.getString("detalle_puntualidad"));
        return r;
    }
}
