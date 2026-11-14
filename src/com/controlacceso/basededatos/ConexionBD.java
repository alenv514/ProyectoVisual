package com.controlacceso.basededatos;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Gestiona la conexión JDBC a la base de datos SQLite embebida.
 */
public class ConexionBD {

    private static final String DB_PATH;
    private static final String DB_URL;

    static {
        File dir = new File(System.getProperty("user.home"), ".control_acceso_rfid");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        DB_PATH = new File(dir, "control_acceso.db").getAbsolutePath();
        DB_URL = "jdbc:sqlite:" + DB_PATH;

        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("Driver SQLite no encontrado: " + e.getMessage());
        }
        inicializarEsquema();
    }

    public static Connection obtenerConexion() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public static String getRutaBaseDatos() {
        return DB_PATH;
    }

    public static void inicializarEsquema() {
        String sqlUsuarios = """
            CREATE TABLE IF NOT EXISTS usuarios (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                uid TEXT UNIQUE NOT NULL,
                nombre TEXT NOT NULL,
                cedula TEXT,
                departamento TEXT,
                cargo TEXT,
                activo INTEGER DEFAULT 1,
                creado_en TEXT
            );
        """;

        String sqlAsistencias = """
            CREATE TABLE IF NOT EXISTS asistencias (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                usuario_id INTEGER,
                uid TEXT NOT NULL,
                nombre TEXT,
                departamento TEXT,
                tipo TEXT NOT NULL,
                estado TEXT NOT NULL,
                fecha_hora TEXT NOT NULL,
                horario_esperado TEXT DEFAULT '-',
                minutos_diferencia INTEGER DEFAULT 0,
                multa REAL DEFAULT 0.0,
                detalle_puntualidad TEXT DEFAULT '-',
                FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
            );
        """;

        try (Connection conn = obtenerConexion(); Statement stmt = conn.createStatement()) {
            stmt.execute(sqlUsuarios);
            stmt.execute(sqlAsistencias);

            // Migraciones seguras para columnas agregadas
            try { stmt.execute("ALTER TABLE asistencias ADD COLUMN horario_esperado TEXT DEFAULT '-'"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE asistencias ADD COLUMN minutos_diferencia INTEGER DEFAULT 0"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE asistencias ADD COLUMN multa REAL DEFAULT 0.0"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE asistencias ADD COLUMN detalle_puntualidad TEXT DEFAULT '-'"); } catch (SQLException ignored) {}
        } catch (SQLException e) {
            System.err.println("Error al inicializar esquema SQLite: " + e.getMessage());
        }
    }
}
