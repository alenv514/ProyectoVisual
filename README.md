# Sistema de Control de Acceso y Asistencia con RFID (ESP32 + RC522)

Proyecto en **Java Swing (JFrame / NetBeans Matisse)** con **SQLite**, integrado con lector **RFID RC522** y microcontrolador **ESP32**.

---

## ⏰ Reglas de Horarios y Penalización

El sistema controla automáticamente las 4 marcaciones diarias y calcula penalizaciones monetarias de forma exacta:

| Turno / Marcación | Horario Oficial | Regla de Cobro ($3.00 USD / hora) |
| :--- | :---: | :--- |
| **Entrada Mañana** | **07:00 AM** | Si entra después de las 07:00 AM: cobra **$3/hora** proporcional al tiempo de retraso. |
| **Salida Mañana** | **01:00 PM (13:00)** | Si sale antes de la 01:00 PM: cobra **$3/hora** proporcional al tiempo faltante. |
| **Entrada Tarde** | **02:00 PM (14:00)** | Si entra después de las 02:00 PM: cobra **$3/hora** proporcional al tiempo de retraso. |
| **Salida Tarde** | **08:00 PM (20:00)** | Si sale antes de las 08:00 PM: cobra **$3/hora** proporcional al tiempo faltante. |

* **Si marcan a tiempo o después de la salida cumplida:** Multa = **$0.00** (✓ A TIEMPO).

---

## 🖥️ Módulos de la Interfaz

1. **Pantalla Principal (Control de Acceso):**
   * **Tarjeta de Estado Dinámica:** Muestra en grande:
     * Nombre del empleado (quién pasó la tarjeta).
     * UID de la llave RFID.
     * Hora exacta de marcado.
     * Turno / Tipo (ej. *Entrada Mañana*, *Salida Mañana*, *Entrada Tarde*, *Salida Tarde*).
     * Horario Oficial esperado.
     * Puntualidad y Multa calculada (ej. `ATRASO: 20 min (Multa: $1.00 USD)` o `A TIEMPO ($0.00)`).
   * **Tabla de Marcaciones del Día:** Lista en tiempo real las marcaciones de hoy coloreadas según puntualidad.
   * **Simulador de Prueba:** Permite ingresar un UID manual para probar el flujo sin necesidad de conectar el ESP32 en ese instante.

2. **Gestión de Personal:**
   * Registro y edición de empleados asociando su UID de tarjeta RFID, Nombre, Cédula, Departamento y Cargo.
   * Botón **"Último"**: Pega automáticamente el último UID detectado por el lector.

3. **Historial y Reportes:**
   * Consulta histórica completa de todas las asistencias con fecha, turno, puntualidad y multa.
   * Filtros por texto, turno y estado.
   * Botón **"Exportar a Excel / CSV"** con todos los datos detallados.

---

## 🔌 Conexión Automática del ESP32
* No requiere botones manuales de conectar ni configurar baudios: el sistema escanea y se conecta **automáticamente en segundo plano** al puerto COM del ESP32 a 115200 baudios.
* La barra superior indica discretamente: `● Lector RFID Conectado (COM3)` o `● Buscando lector RFID...`.
