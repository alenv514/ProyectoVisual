-- =============================================================================
-- BASE DE DATOS: control_acceso_rfid
-- Sistema de Control de Acceso y Asistencia RFID (MySQL / XAMPP)
-- =============================================================================

CREATE DATABASE IF NOT EXISTS control_acceso_rfid CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE control_acceso_rfid;

-- -----------------------------------------------------------------------------
-- Tabla: usuarios (Empleados registrados en el sistema)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS usuarios (
    id INT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(50) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    cedula VARCHAR(20),
    departamento VARCHAR(100),
    cargo VARCHAR(100),
    activo TINYINT DEFAULT 1,
    creado_en VARCHAR(30),
    foto VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------------------------------------------------------
-- Tabla: asistencias (Historial de marcaciones y penalizaciones de turnos)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS asistencias (
    id INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id INT,
    uid VARCHAR(50) NOT NULL,
    nombre VARCHAR(100),
    departamento VARCHAR(100),
    tipo VARCHAR(50) NOT NULL,
    estado VARCHAR(50) NOT NULL,
    fecha_hora VARCHAR(30) NOT NULL,
    horario_esperado VARCHAR(30) DEFAULT '-',
    minutos_diferencia INT DEFAULT 0,
    multa DOUBLE DEFAULT 0.0,
    detalle_puntualidad VARCHAR(100) DEFAULT '-',
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------------------------------------------------------
-- Tabla: administradores (Credenciales de acceso para Gestión y Reportes)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS administradores (
    id INT AUTO_INCREMENT PRIMARY KEY,
    usuario VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    nombre VARCHAR(100) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO administradores (id, usuario, password, nombre) 
VALUES (1, 'admin', 'admin123', 'Administrador del Sistema');
