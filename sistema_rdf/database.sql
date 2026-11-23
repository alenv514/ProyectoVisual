CREATE TABLE IF NOT EXISTS `usuarios` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `uid` varchar(50) NOT NULL,
  `nombre` varchar(100) NOT NULL,
  `cedula` varchar(20) DEFAULT NULL,
  `departamento` varchar(100) DEFAULT NULL,
  `cargo` varchar(100) DEFAULT NULL,
  `activo` tinyint(4) DEFAULT 1,
  `creado_en` varchar(30) DEFAULT NULL,
  `foto` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uid` (`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `asistencias` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `usuario_id` int(11) DEFAULT NULL,
  `uid` varchar(50) NOT NULL,
  `nombre` varchar(100) DEFAULT NULL,
  `departamento` varchar(100) DEFAULT NULL,
  `tipo` varchar(50) NOT NULL,
  `estado` varchar(50) NOT NULL,
  `fecha_hora` varchar(30) NOT NULL,
  `horario_esperado` varchar(30) DEFAULT '-',
  `minutos_diferencia` int(11) DEFAULT 0,
  `multa` double DEFAULT 0,
  `detalle_puntualidad` varchar(100) DEFAULT '-',
  PRIMARY KEY (`id`),
  KEY `usuario_id` (`usuario_id`),
  CONSTRAINT `asistencias_ibfk_1` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `administradores` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `usuario` varchar(50) NOT NULL,
  `password` varchar(100) NOT NULL,
  `nombre` varchar(100) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `usuario` (`usuario`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `reloj_sistema` (
  `id` int(11) NOT NULL,
  `timestamp_base` bigint(20) NOT NULL,
  `hrtime_base` varchar(40) NOT NULL,
  `ultimo_timestamp` bigint(20) NOT NULL,
  `actualizado_en` varchar(30) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT IGNORE INTO `administradores` (`id`, `usuario`, `password`, `nombre`)
VALUES (1, 'admin', 'admin123', 'Administrador del Sistema');
