package com.controlacceso.modelos;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Reloj independiente del Sistema de Control de Acceso.
 *
 * Utiliza un contador monotónico de hardware de alta resolución (System.nanoTime())
 * a partir de la fecha y hora capturada al iniciar el sistema.
 *
 * Es inmune a cambios manuales en el reloj de la laptop (Windows): si el usuario
 * adelanta o atrasa la hora de la computadora, este reloj continúa avanzando
 * segundo a segundo de forma real, precisa e inalterable.
 */
public class RelojSistema {

    private static LocalDateTime tiempoBase;
    private static long nanosBase;

    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    static {
        inicializar(LocalDateTime.now());
    }

    /**
     * Inicializa el reloj con una fecha/hora base.
     *
     * @param base Fecha y hora base desde la cual avanza el reloj.
     */
    public static synchronized void inicializar(LocalDateTime base) {
        tiempoBase = (base != null) ? base : LocalDateTime.now();
        nanosBase = System.nanoTime();
    }

    /**
     * Retorna la fecha y hora actual calculada por el reloj independiente.
     * Inmune a alteraciones en el reloj del sistema operativo.
     */
    public static synchronized LocalDateTime ahora() {
        long nanosTranscurridos = System.nanoTime() - nanosBase;
        long segundos = nanosTranscurridos / 1_000_000_000L;
        long nanosResto = nanosTranscurridos % 1_000_000_000L;
        return tiempoBase.plusSeconds(segundos).plusNanos(nanosResto);
    }

    /**
     * Retorna la hora actual del reloj independiente.
     */
    public static LocalTime horaActual() {
        return ahora().toLocalTime();
    }

    /**
     * Retorna la fecha actual del reloj independiente.
     */
    public static LocalDate fechaActual() {
        return ahora().toLocalDate();
    }

    /**
     * Permite ajustar la hora base del sistema (para pruebas o demostraciones).
     */
    public static synchronized void ajustar(LocalDateTime nuevaFechaHora) {
        inicializar(nuevaFechaHora);
    }

    public static String getHoraFormateada() {
        return ahora().format(FORMATO_HORA);
    }

    public static String getFechaHoraFormateada() {
        return ahora().format(FORMATO_FECHA_HORA);
    }

    public static String getFechaFormateada() {
        return ahora().format(FORMATO_FECHA);
    }
}
