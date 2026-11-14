package com.controlacceso.basededatos;

import com.controlacceso.modelos.RegistroAsistencia;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Implementación SQLite del acceso a datos para Registros de Asistencia y Reportes.
 */
public class AsistenciaDAOSqlite implements AsistenciaDAO {

    @Override
    public boolean registrar(RegistroAsistencia reg) {
        String sql = """
            INSERT INTO asistencias (
                usuario_id, uid, nombre, departamento, tipo, estado,
                fecha_hora, horario_esperado, minutos_diferencia, multa, detalle_puntualidad
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            if (reg.getUsuarioId() != null) {
                pstmt.setInt(1, reg.getUsuarioId());
            } else {
                pstmt.setNull(1, java.sql.Types.INTEGER);
            }
            pstmt.setString(2, reg.getUid().toUpperCase().trim());
            pstmt.setString(3, reg.getNombre());
            pstmt.setString(4, reg.getDepartamento());
            pstmt.setString(5, reg.getTipo());
            pstmt.setString(6, reg.getEstado());
            pstmt.setString(7, reg.getFechaHora());
            pstmt.setString(8, reg.getHorarioEsperado() != null ? reg.getHorarioEsperado() : "-");
            pstmt.setInt(9, reg.getMinutosDiferencia());
            pstmt.setDouble(10, reg.getMulta());
            pstmt.setString(11, reg.getDetallePuntualidad() != null ? reg.getDetallePuntualidad() : "-");

            int filas = pstmt.executeUpdate();
            if (filas > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        reg.setId(rs.getInt(1));
                    }
                }
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("Error al registrar asistencia: " + e.getMessage());
            return false;
        }
    }

    @Override
    public List<RegistroAsistencia> listarHoy() {
        String hoy = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return listarPorFiltros(hoy, null);
    }

    @Override
    public List<RegistroAsistencia> listarPorFiltros(String fecha, String nombre) {
        List<RegistroAsistencia> lista = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM asistencias WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (fecha != null && !fecha.trim().isEmpty()) {
            sql.append("AND date(fecha_hora) = date(?) ");
            params.add(fecha.trim());
        }

        if (nombre != null && !nombre.trim().isEmpty()) {
            sql.append("AND (LOWER(nombre) LIKE ? OR UPPER(uid) LIKE ?) ");
            String patron = "%" + nombre.trim().toLowerCase() + "%";
            params.add(patron);
            params.add("%" + nombre.trim().toUpperCase() + "%");
        }

        sql.append("ORDER BY id DESC");

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearAsistencia(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al consultar asistencias por filtros: " + e.getMessage());
        }
        return lista;
    }

    @Override
    public List<RegistroAsistencia> listarPorUsuarioYFecha(String uid, String fecha) {
        List<RegistroAsistencia> lista = new ArrayList<>();
        String sql = """
            SELECT * FROM asistencias
            WHERE UPPER(TRIM(uid)) = UPPER(TRIM(?))
              AND date(fecha_hora) = date(?)
              AND estado = 'ACCESO PERMITIDO'
            ORDER BY id ASC
        """;

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, uid.toUpperCase().trim());
            pstmt.setString(2, fecha);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearAsistencia(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al consultar asistencias por usuario y fecha: " + e.getMessage());
        }
        return lista;
    }

    @Override
    public boolean exportarCsv(File archivo, List<RegistroAsistencia> registros) {
        return false;
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

    private String escaparCsv(String valor) {
        if (valor == null) return "-";
        return valor.replace(";", ",");
    }
}
