# -*- coding: utf-8 -*-
import os
import docx
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.oxml import OxmlElement, parse_xml
from docx.oxml.ns import nsdecls, qn

def set_cell_background(cell, fill_hex):
    tcPr = cell._tc.get_or_add_tcPr()
    shd = parse_xml(f'<w:shd {nsdecls("w")} w:fill="{fill_hex}"/>')
    tcPr.append(shd)

def set_cell_margins(cell, top=100, bottom=100, left=150, right=150):
    tcPr = cell._tc.get_or_add_tcPr()
    tcMar = OxmlElement('w:tcMar')
    for m, val in [('top', top), ('bottom', bottom), ('left', left), ('right', right)]:
        node = OxmlElement(f'w:{m}')
        node.set(qn('w:w'), str(val))
        node.set(qn('w:type'), 'dxa')
        tcMar.append(node)
    tcPr.append(tcMar)

def add_callout(doc, title, text, bg_color="F0F4F8", border_color="2B579A"):
    tbl = doc.add_table(rows=1, cols=1)
    tbl.alignment = WD_TABLE_ALIGNMENT.CENTER
    tbl.autofit = False
    
    cell = tbl.cell(0, 0)
    cell.width = Inches(6.5)
    set_cell_background(cell, bg_color)
    set_cell_margins(cell, top=140, bottom=140, left=200, right=200)
    
    p = cell.paragraphs[0]
    p.paragraph_format.space_before = Pt(2)
    p.paragraph_format.space_after = Pt(4)
    run_t = p.add_run(f"📌 {title}\n")
    run_t.bold = True
    run_t.font.name = "Calibri"
    run_t.font.size = Pt(11)
    run_t.font.color.rgb = RGBColor(0x1B, 0x36, 0x5D)
    
    run_b = p.add_run(text)
    run_b.font.name = "Calibri"
    run_b.font.size = Pt(10)
    run_b.font.color.rgb = RGBColor(0x33, 0x33, 0x33)
    
    doc.add_paragraph().paragraph_format.space_after = Pt(4)

def add_code_block(doc, code_text):
    tbl = doc.add_table(rows=1, cols=1)
    tbl.alignment = WD_TABLE_ALIGNMENT.CENTER
    tbl.autofit = False
    cell = tbl.cell(0, 0)
    cell.width = Inches(6.5)
    set_cell_background(cell, "F4F5F7")
    set_cell_margins(cell, top=100, bottom=100, left=150, right=150)
    
    p = cell.paragraphs[0]
    p.paragraph_format.space_before = Pt(2)
    p.paragraph_format.space_after = Pt(2)
    run = p.add_run(code_text)
    run.font.name = "Consolas"
    run.font.size = Pt(9)
    run.font.color.rgb = RGBColor(0x1E, 0x39, 0x5B)
    doc.add_paragraph().paragraph_format.space_after = Pt(3)

def build_document(output_paths):
    doc = docx.Document()
    
    # Configurar márgenes de 1 pulgada
    for section in doc.sections:
        section.top_margin = Inches(1)
        section.bottom_margin = Inches(1)
        section.left_margin = Inches(1)
        section.right_margin = Inches(1)

    # -------------------------------------------------------------
    # PORTADA / TÍTULO PRINCIPAL
    # -------------------------------------------------------------
    p_title = doc.add_paragraph()
    p_title.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_title.paragraph_format.space_before = Pt(10)
    p_title.paragraph_format.space_after = Pt(6)
    r_title = p_title.add_run("SISTEMA DE CONTROL DE ACCESO Y ASISTENCIA RFID")
    r_title.bold = True
    r_title.font.name = "Arial"
    r_title.font.size = Pt(20)
    r_title.font.color.rgb = RGBColor(0x1A, 0x36, 0x5D)

    p_sub = doc.add_paragraph()
    p_sub.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_sub.paragraph_format.space_after = Pt(18)
    r_sub = p_sub.add_run("GUÍA MAESTRA DE DISTRIBUCIÓN GRUPAL, FLUJO GIT/GITHUB Y PROMPTS PARA IA\nArquitectura 100% Orientada a Objetos (POO) • Hardware ESP32 + RC522")
    r_sub.font.name = "Calibri"
    r_sub.font.size = Pt(12)
    r_sub.font.color.rgb = RGBColor(0x4A, 0x55, 0x68)

    # Tabla de Integrantes
    tbl_team = doc.add_table(rows=7, cols=3)
    tbl_team.alignment = WD_TABLE_ALIGNMENT.CENTER
    headers = ["N°", "Integrante", "Rol Asignado en el Proyecto"]
    for i, h in enumerate(headers):
        cell = tbl_team.cell(0, i)
        set_cell_background(cell, "1A365D")
        p = cell.paragraphs[0]
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        r = p.add_run(h)
        r.bold = True
        r.font.name = "Calibri"
        r.font.size = Pt(10)
        r.font.color.rgb = RGBColor(0xFF, 0xFF, 0xFF)

    team_data = [
        ("1", "Alen Villamarín (Líder)", "Arquitectura General, Vista Swing (Matisse) e Integración Main"),
        ("2", "Evelyn", "Modelos de Dominio y POO (Herencia Persona -> Empleado)"),
        ("3", "Mateo", "Lógica de Turnos y Penalizaciones ($3/h) con Polimorfismo"),
        ("4", "Juan", "Capa de Persistencia, Patrón DAO y Base de Datos MySQL (XAMPP)"),
        ("5", "David", "Comunicación Serial USB (jSerialComm) y Eventos Hardware"),
        ("6", "Semblantes", "Firmware ESP32 (C++ SPI RC522) y Pruebas Unitarias JUnit 5")
    ]

    for row_idx, (num, name, role) in enumerate(team_data, start=1):
        row = tbl_team.rows[row_idx]
        bg = "F7FAFC" if row_idx % 2 == 1 else "EDF2F7"
        for col_idx, val in enumerate([num, name, role]):
            cell = row.cells[col_idx]
            set_cell_background(cell, bg)
            set_cell_margins(cell, top=80, bottom=80, left=100, right=100)
            p = cell.paragraphs[0]
            if col_idx == 0:
                p.alignment = WD_ALIGN_PARAGRAPH.CENTER
            r = p.add_run(val)
            r.font.name = "Calibri"
            r.font.size = Pt(9.5)
            r.font.color.rgb = RGBColor(0x2D, 0x37, 0x48)

    add_callout(
        doc,
        "REPOSITORIO CENTRAL EN GITHUB (SUBIDA OFICIAL)",
        "Todos los integrantes deben clonar y subir sus contribuciones exclusivamente a este repositorio:\n"
        "• URL Web en GitHub: https://github.com/alenv514/ProyectoVisual\n"
        "• URL de Clonado Git (HTTPS): https://github.com/alenv514/ProyectoVisual.git\n"
        "• Rama Principal de Producción: main\n"
        "Todas las funcionalidades se integran mediante ramas 'feature/' y Pull Requests (PR) hacia 'main'.",
        bg_color="EFF6FF",
        border_color="2563EB"
    )

    doc.add_page_break()

    # -------------------------------------------------------------
    # SECCIÓN 1: ESPECIFICACIONES TÉCNICAS Y REGLAS DE NEGOCIO
    # -------------------------------------------------------------
    h1 = doc.add_heading("1. ESPECIFICACIONES TÉCNICAS Y REGLAS DE NEGOCIO", level=1)
    h1.paragraph_format.space_before = Pt(12)
    h1.paragraph_format.space_after = Pt(6)

    p_desc = doc.add_paragraph(
        "El sistema es una solución integral de control de acceso físico y registro de asistencia que combina "
        "hardware embebido (microcontrolador ESP32 DevKit conectado vía SPI a un lector RFID MFRC522) y una "
        "aplicación de escritorio desarrollada en Java Swing bajo el patrón arquitectónico Modelo-Vista-Controlador (MVC), "
        "con base de datos relacional MySQL (vía XAMPP / base 'control_acceso_rfid') y comunicación serial en tiempo real a 115200 baudios."
    )
    p_desc.paragraph_format.space_after = Pt(8)

    add_callout(
        doc,
        "REGLAS OFICIALES DE HORARIOS Y COBROS DE MULTAS ($3.00/HORA)",
        "• Turno Mañana: Entrada oficial 07:00 AM | Salida oficial 01:00 PM (13:00)\n"
        "• Turno Tarde: Entrada oficial 02:00 PM (14:00) | Salida oficial 08:00 PM (20:00)\n"
        "• Penalización por atraso: $3.00 USD por hora de retraso ($0.05 por minuto exacto transcurrido).\n"
        "• Penalización por salida anticipada: $3.00 USD por hora si el empleado marca antes de la hora oficial.\n"
        "• Asistencia puntual: Si marca a la hora o con anticipación a la entrada, la multa es $0.00 (A TIEMPO).\n"
        "• Detección automática: La secuencia de marcaciones del día determina si es Entrada o Salida de cada turno.",
        bg_color="FFF8E7",
        border_color="D97706"
    )

    doc.add_heading("Estructura de Paquetes en Español (Arquitectura Modular)", level=2)
    p_pkg = doc.add_paragraph(
        "Para máxima claridad académica y profesional, el código está organizado en 6 paquetes autoexplicativos:"
    )
    p_pkg.paragraph_format.space_after = Pt(4)

    pkgs_info = [
        ("com.controlacceso.principal", "Contiene Main.java, punto de entrada de la aplicación, configuración Look & Feel y carga de DLL nativa."),
        ("com.controlacceso.modelos", "Contiene las entidades del negocio (Persona, Empleado, RegistroAsistencia), y las clases de cálculo polimórfico de turnos (Turno, TurnoManana, TurnoTarde, GestorTurnos)."),
        ("com.controlacceso.basededatos", "Gestión de MySQL (XAMPP base dedicada 'control_acceso_rfid') y patrón DAO (EmpleadoDAO, EmpleadoDAOMySql, AsistenciaDAO, AsistenciaDAOMySql)."),
        ("com.controlacceso.controlador", "ControladorAcceso.java que orquesta la comunicación entre el lector serial, la lógica de negocio y las vistas."),
        ("com.controlacceso.comunicacion_esp32", "SerialManager y SerialListener para conexión bidireccional USB con el microcontrolador a 115200 baud."),
        ("com.controlacceso.vistas", "VentanaPrincipal.java y VentanaPrincipal.form con diseño gráfico NetBeans Matisse y renderizado de tablas.")
    ]

    for pkg_name, pkg_desc in pkgs_info:
        p = doc.add_paragraph(style='List Bullet')
        r_b = p.add_run(pkg_name + ": ")
        r_b.bold = True
        r_b.font.color.rgb = RGBColor(0x1A, 0x36, 0x5D)
        p.add_run(pkg_desc)

    doc.add_page_break()

    # -------------------------------------------------------------
    # SECCIÓN 2: ESTRATEGIA DE COMMITS Y GITHUB (6 INTEGRANTES)
    # -------------------------------------------------------------
    h2 = doc.add_heading("2. ESTRATEGIA DE RAMAS, COMMITS Y PULL REQUESTS EN GITHUB", level=1)
    h2.paragraph_format.space_before = Pt(12)
    h2.paragraph_format.space_after = Pt(6)

    p_git_desc = doc.add_paragraph(
        "Para que la entrega universitaria refleje un auténtico trabajo colaborativo, no se debe subir todo en un solo commit gigante. "
        "Cada integrante trabajará sobre una rama con prefijo 'feature/' y realizará entre 3 y 4 commits atómicos que muestren "
        "la evolución de su módulo, finalizando con un Pull Request (PR) aprobado y fusionado (Merged) hacia la rama principal 'main'."
    )
    p_git_desc.paragraph_format.space_after = Pt(8)

    add_callout(
        doc,
        "ESTÁNDAR DE COMMITS: CONVENTIONAL COMMITS",
        "Cada mensaje de commit debe seguir la convención internacional:\n"
        "• feat(...): Para agregar nueva funcionalidad o clases.\n"
        "• test(...): Para agregar o ejecutar pruebas unitarias.\n"
        "• docs(...): Para añadir comentarios JavaDoc o documentación.\n"
        "• refactor(...): Para mejorar la estructura sin cambiar funcionalidad.\n"
        "• chore(...): Para configuraciones de proyecto (build.xml, nbproject, .gitignore).",
        bg_color="F0FDF4",
        border_color="16A34A"
    )

    add_callout(
        doc,
        "VINCULACIÓN DEL REPOSITORIO GITHUB: alenv514/ProyectoVisual",
        "El repositorio oficial del proyecto está configurado en:\n"
        "  • URL Web:  https://github.com/alenv514/ProyectoVisual\n"
        "  • URL Clon: https://github.com/alenv514/ProyectoVisual.git\n\n"
        "Paso inicial del Líder (Alen) para publicar la base del proyecto:\n"
        "  1. git init\n"
        "  2. git remote add origin https://github.com/alenv514/ProyectoVisual.git\n"
        "  3. git branch -M main\n"
        "  4. git add build.xml manifest.mf nbproject lib .gitignore README.md\n"
        "  5. git commit -m \"chore(setup): configuración inicial del proyecto Java Ant\"\n"
        "  6. git push -u origin main\n\n"
        "Flujo de trabajo para los demás 5 integrantes:\n"
        "  1. Clonar en su equipo: git clone https://github.com/alenv514/ProyectoVisual.git\n"
        "  2. Ingresar a la carpeta: cd ProyectoVisual\n"
        "  3. Crear su rama asignada: git checkout -b feature/<nombre-rama>\n"
        "  4. Desarrollar sus clases y realizar sus 3 o 4 commits atómicos.\n"
        "  5. Subir su rama: git push -u origin feature/<nombre-rama>\n"
        "  6. En GitHub (https://github.com/alenv514/ProyectoVisual), pulsar el botón verde 'Compare & pull request' para fusionar a 'main'.",
        bg_color="EFF6FF",
        border_color="2563EB"
    )

    p_sec = doc.add_paragraph("Orden de Fusión de Ramas (Cronología Natural):")
    p_sec.bold = True
    
    cronologia = [
        ("Paso 1", "Alen Villamarín", "Rama main", "Estructura inicial del proyecto Java Ant (build.xml, nbproject, lib/, .gitignore, README.md)."),
        ("Paso 2", "Semblantes", "feature/hardware-firmware", "Firmware ESP32 C++ para lectura RC522 y primeras pruebas unitarias."),
        ("Paso 3", "Evelyn", "feature/modelos-herencia", "Entidades Persona, Empleado (Herencia) y RegistroAsistencia."),
        ("Paso 4", "Mateo", "feature/turnos-polimorfismo", "Clases Turno, TurnoManana, TurnoTarde y GestorTurnos (Polimorfismo)."),
        ("Paso 5", "Juan", "feature/basedatos-dao-mysql", "Clase Conexion, interfaces y DAOs MySQL con reportes JasperReports."),
        ("Paso 6", "David", "feature/comunicacion-serial", "SerialManager y SerialListener con jSerialComm y auto-detección."),
        ("Paso 7", "Alen Villamarín", "feature/interfaz-grafica-mvc", "ControladorAcceso MVC, integración JFrame Swing y suite JUnit final.")
    ]

    for paso, autor, rama, desc in cronologia:
        p = doc.add_paragraph(style='List Bullet')
        r1 = p.add_run(f"{paso} - {autor} ({rama}): ")
        r1.bold = True
        p.add_run(desc)

    doc.add_page_break()

    # -------------------------------------------------------------
    # SECCIÓN 3: GUÍA INDIVIDUAL Y PROMPTS PARA CADA INTEGRANTE
    # -------------------------------------------------------------
    h3 = doc.add_heading("3. GUÍA INDIVIDUAL, COMMITS Y PROMPTS PARA IA (POR INTEGRANTE)", level=1)
    h3.paragraph_format.space_before = Pt(12)
    h3.paragraph_format.space_after = Pt(6)

    # DATOS DE LOS 6 INTEGRANTES CON PROMPTS DETALLADOS
    fichas = [
        {
            "num": "1",
            "nombre": "Alen Villamarín",
            "rol": "Líder de Proyecto, Interfaz Gráfica (Swing / Matisse) e Integración Main",
            "rama": "feature/interfaz-grafica-mvc",
            "paquetes": "com.controlacceso.principal, com.controlacceso.vistas, build.xml, nbproject, lib",
            "commits": [
                ("Commit 1", "chore(setup): configuración inicial del proyecto Java Ant con build.xml, nbproject, carpeta lib (JARs) y .gitignore"),
                ("Commit 2", "feat(ui): creación del formulario VentanaPrincipal.form con paleta Matisse y pestañas Monitoreo, Personal e Historial"),
                ("Commit 3", "feat(ui): lógica de visualización de turnos, renderers con alertas de colores para multas y botones de acción"),
                ("Commit 4", "feat(main): punto de entrada Main.java con configuración de LookAndFeel Windows y enlace al Event Dispatch Thread")
            ],
            "comandos": (
                "# 1. Inicializar repositorio y conectar al repositorio en GitHub:\n"
                "git init\n"
                "git remote add origin https://github.com/alenv514/ProyectoVisual.git\n"
                "git branch -M main\n\n"
                "# 2. Subir la estructura base del proyecto Java Ant a la rama 'main':\n"
                "git add build.xml manifest.mf nbproject lib .gitignore README.md\n"
                "git commit -m \"chore(setup): configuración inicial del proyecto Java Ant con librerías JAR\"\n"
                "git push -u origin main\n\n"
                "# 3. Crear rama para el módulo de Interfaz Gráfica y Main:\n"
                "git checkout -b feature/interfaz-grafica-mvc\n"
                "git add src/com/controlacceso/vistas/VentanaPrincipal.form\n"
                "git commit -m \"feat(ui): creación del formulario VentanaPrincipal.form con diseño Matisse\"\n"
                "git add src/com/controlacceso/vistas/VentanaPrincipal.java\n"
                "git commit -m \"feat(ui): renderizado dinámico de tablas y alertas visuales de multas\"\n"
                "git add src/com/controlacceso/principal/Main.java\n"
                "git commit -m \"feat(main): punto de entrada Main.java y configuración LookAndFeel\"\n"
                "git push -u origin feature/interfaz-grafica-mvc\n"
                "# Ir a https://github.com/alenv514/ProyectoVisual y abrir el Pull Request a main."
            ),
            "prompt_ia": (
                "\"Actúa como un Desarrollador Java Senior especialista en Swing y NetBeans Matisse. "
                "Formo parte de un equipo de 6 personas en un proyecto de Control de Acceso y Asistencia con RFID. "
                "Mi rol asignado es la Interfaz Gráfica de Usuario (Vista) y el punto de entrada Main. "
                "Por favor, genera:\n"
                "1. La clase 'Main.java' en el paquete 'com.controlacceso.principal', encargada de configurar el LookAndFeel nativo de Windows, registrar la propiedad de la librería nativa jSerialComm ('jSerialComm.library.path') y desplegar la ventana en el EDT (SwingUtilities.invokeLater).\n"
                "2. La clase 'VentanaPrincipal.java' en el paquete 'com.controlacceso.vistas', que actúe como Vista pura bajo el patrón MVC, recibiendo eventos de 'ControladorAcceso'. Debe implementar 'SerialListener', gestionar tres pestañas (Monitoreo en Vivo con indicador badge de conexión verde/gris, Gestión de Personal con CRUD de empleados, e Historial con filtros por fecha/nombre y generación de reportes con JasperReports).\n"
                "3. Explícame cómo el método 'mostrarResultadoEnPantalla' actualiza dinámicamente los colores a verde (#27AE60) si no hay multa y a naranja/rojo (#D35400) si existe multa por retraso o salida anticipada.\""
            ),
            "defensa": [
                ("¿Por qué la ventana no consulta directamente la base de datos?", 
                 "Porque aplicamos el patrón MVC (Modelo-Vista-Controlador). La VentanaPrincipal es una Vista pura; delega las consultas y el cálculo al ControladorAcceso, desacoplando la interfaz gráfica de la persistencia y permitiendo un mantenimiento limpio."),
                ("¿Cómo garantiza que la interfaz gráfica no se congele durante la lectura serial?", 
                 "Utilizamos SwingUtilities.invokeLater() para que la actualización visual se ejecute siempre en el Event Dispatch Thread (EDT), mientras que la lectura del puerto USB corre en un hilo de fondo en el SerialManager.")
            ]
        },
        {
            "num": "2",
            "nombre": "Evelyn",
            "rol": "Modelado del Dominio y POO (Principio de Herencia y Encapsulamiento)",
            "rama": "feature/modelos-herencia",
            "paquetes": "com.controlacceso.modelos (Persona.java, Empleado.java, RegistroAsistencia.java)",
            "commits": [
                ("Commit 1", "feat(modelos): clase abstracta Persona con atributos encapsulados (id, nombre, cedula)"),
                ("Commit 2", "feat(modelos): subclase Empleado que hereda de Persona y añade datos laborales y uidTarjeta"),
                ("Commit 3", "feat(modelos): entidad RegistroAsistencia para modelar los registros de accesos y multas"),
                ("Commit 4", "docs(modelos): documentación javadoc y métodos polimórficos getDescripcionRol()")
            ],
            "comandos": (
                "# 1. Clonar el repositorio oficial del proyecto:\n"
                "git clone https://github.com/alenv514/ProyectoVisual.git\n"
                "cd ProyectoVisual\n\n"
                "# 2. Crear y cambiarse a su rama asignada:\n"
                "git checkout -b feature/modelos-herencia\n\n"
                "# 3. Agregar clases de modelos y registrar commits:\n"
                "git add src/com/controlacceso/modelos/Persona.java\n"
                "git commit -m \"feat(modelos): clase abstracta Persona con atributos encapsulados\"\n"
                "git add src/com/controlacceso/modelos/Empleado.java\n"
                "git commit -m \"feat(modelos): subclase Empleado aplicando principio de herencia\"\n"
                "git add src/com/controlacceso/modelos/RegistroAsistencia.java\n"
                "git commit -m \"feat(modelos): entidad RegistroAsistencia para auditoría de marcaciones\"\n\n"
                "# 4. Subir la rama al repositorio de GitHub:\n"
                "git push -u origin feature/modelos-herencia\n"
                "# Ir a https://github.com/alenv514/ProyectoVisual y abrir el Pull Request a main."
            ),
            "prompt_ia": (
                "\"Actúa como una Ingeniera de Software experta en Programación Orientada a Objetos en Java. "
                "En mi proyecto grupal de Sistema de Control de Asistencia RFID (6 integrantes), mi responsabilidad "
                "es diseñar el Modelo del Dominio aplicando estrictamente los pilares de Abstracción, Encapsulamiento y Herencia. "
                "Por favor, diséñame las siguientes clases para el paquete 'com.controlacceso.modelos':\n"
                "1. 'Persona.java': Clase base abstracta con campos protegidos (id, nombre, cedula), constructores sobrecargados, getters/setters y el método abstracto 'public abstract String getDescripcionRol();'.\n"
                "2. 'Empleado.java': Subclase concreta que extiende de 'Persona' ('public class Empleado extends Persona'). Debe añadir campos privados: uidTarjeta, departamento, cargo, activo (boolean) y creadoEn (String). Implementar 'getDescripcionRol()' retornando 'Empleado [depto - cargo]'.\n"
                "3. 'RegistroAsistencia.java': Entidad que representa cada marcación física (id, usuarioId, uid, nombre, departamento, tipo, estado, fechaHora, horarioEsperado, minutosDiferencia, multa, detallePuntualidad).\n"
                "4. Explícame con claridad cómo se demuestra el principio de Herencia y Encapsulamiento en este diseño para defenderlo ante el docente.\""
            ),
            "defensa": [
                ("¿Por qué Persona es una clase abstracta?", 
                 "Porque en nuestro sistema una 'Persona' genérica no existe por sí sola; siempre será un 'Empleado'. Hacerla abstracta evita instancias incompletas y fuerza a las subclases a definir su rol mediante el método abstracto getDescripcionRol()."),
                ("¿Cómo se cumple el Encapsulamiento en Empleado?", 
                 "Todos los atributos son privados o protegidos, impidiendo su modificación directa desde fuera. El acceso y validación se hace únicamente a través de métodos de acceso (getters y setters).")
            ]
        },
        {
            "num": "3",
            "nombre": "Mateo",
            "rol": "Lógica de Negocio de Turnos y Penalizaciones (Polimorfismo)",
            "rama": "feature/turnos-polimorfismo",
            "paquetes": "com.controlacceso.modelos (Turno.java, TurnoManana.java, TurnoTarde.java, ResultadoCalculoTurno.java, GestorTurnos.java)",
            "commits": [
                ("Commit 1", "feat(turnos): clase base abstracta Turno con constantes horarias y cálculo de penalización ($3.00/h)"),
                ("Commit 2", "feat(turnos): subclases polimórficas TurnoManana (07:00 a 13:00) y TurnoTarde (14:00 a 20:00)"),
                ("Commit 3", "feat(turnos): clase GestorTurnos para orquestar la secuencia de entradas y salidas en el día"),
                ("Commit 4", "refactor(turnos): desacoplamiento de resultado en DTO ResultadoCalculoTurno")
            ],
            "comandos": (
                "# 1. Clonar el repositorio oficial del proyecto:\n"
                "git clone https://github.com/alenv514/ProyectoVisual.git\n"
                "cd ProyectoVisual\n\n"
                "# 2. Crear y cambiarse a su rama asignada:\n"
                "git checkout -b feature/turnos-polimorfismo\n\n"
                "# 3. Agregar clases de turnos y registrar commits:\n"
                "git add src/com/controlacceso/modelos/ResultadoCalculoTurno.java src/com/controlacceso/modelos/Turno.java\n"
                "git commit -m \"feat(turnos): clase abstracta Turno con regla de cobro de $3/hora\"\n"
                "git add src/com/controlacceso/modelos/TurnoManana.java src/com/controlacceso/modelos/TurnoTarde.java\n"
                "git commit -m \"feat(turnos): subclases TurnoManana y TurnoTarde aplicando polimorfismo\"\n"
                "git add src/com/controlacceso/modelos/GestorTurnos.java\n"
                "git commit -m \"feat(turnos): GestorTurnos para control de secuencia de marcaciones diarias\"\n\n"
                "# 4. Subir la rama al repositorio de GitHub:\n"
                "git push -u origin feature/turnos-polimorfismo\n"
                "# Ir a https://github.com/alenv514/ProyectoVisual y abrir el Pull Request a main."
            ),
            "prompt_ia": (
                "\"Actúa como un Arquitecto de Software especialista en Reglas de Negocio y Polimorfismo en Java. "
                "En mi proyecto de Control de Acceso RFID, mi rol es desarrollar el motor de cálculo de horarios y multas. "
                "Las especificaciones son estrictas:\n"
                "- Turno Mañana: Entrada 07:00 AM | Salida 01:00 PM (13:00).\n"
                "- Turno Tarde: Entrada 02:00 PM (14:00) | Salida 08:00 PM (20:00).\n"
                "- Multa: $3.00 USD por hora de retraso en la entrada o salida temprana ($0.05 por minuto exacto). Si llega puntual la multa es $0.00.\n"
                "Por favor, génerame para el paquete 'com.controlacceso.modelos':\n"
                "1. 'ResultadoCalculoTurno.java': DTO que almacena (tipo, horarioEsperado, minutosDiferencia, multa, detallePuntualidad).\n"
                "2. 'Turno.java': Clase base abstracta con TARIFA_POR_HORA = 3.00, atributos nombreTurno, horaEntradaEsperada, horaSalidaEsperada, y métodos polimórficos 'evaluarEntrada(LocalTime)' y 'evaluarSalida(LocalTime)'.\n"
                "3. 'TurnoManana.java' y 'TurnoTarde.java': Subclases que inicializan sus horas exactas.\n"
                "4. 'GestorTurnos.java': Que reciba (LocalDateTime momento, List<RegistroAsistencia> marcasHoy) y determine automáticamente si corresponde Entrada Mañana, Salida Mañana, Entrada Tarde o Salida Tarde según las marcas previas del empleado.\""
            ),
            "defensa": [
                ("¿Cómo se evidencia el Polimorfismo en el cálculo de horarios?", 
                 "La clase GestorTurnos maneja referencias de tipo genérico 'Turno'. Al llamar al método evaluarEntrada() o evaluarSalida(), Java ejecuta dinámicamente la instancia concreta (TurnoManana o TurnoTarde) sin necesidad de condicionales acoplados."),
                ("¿Cómo se calculan exactamente las fracciones de retraso?", 
                 "Utilizamos java.time.Duration.between() para calcular la diferencia en minutos y la multiplicamos por (3.00 / 60.0), es decir, $0.05 por minuto, redondeando a dos decimales con Math.round().")
            ]
        },
        {
            "num": "4",
            "nombre": "Juan",
            "rol": "Capa de Persistencia, Patrón DAO y Base de Datos MySQL (XAMPP)",
            "rama": "feature/basedatos-dao-mysql",
            "paquetes": "conexion (Conexion.java), com.controlacceso.basededatos (EmpleadoDAO, EmpleadoDAOMySql, AsistenciaDAO, AsistenciaDAOMySql)",
            "commits": [
                ("Commit 1", "feat(db): clase Conexion en paquete conexion para MySQL localhost:3306/control_acceso_rfid con XAMPP"),
                ("Commit 2", "feat(dao): interfaces EmpleadoDAO y AsistenciaDAO para abstracción de persistencia"),
                ("Commit 3", "feat(dao): implementaciones MySQL (EmpleadoDAOMySql y AsistenciaDAOMySql) con PreparedStatement"),
                ("Commit 4", "feat(reportes): paquete Reportes con plantilla JasperReports y GestorReportes con JasperViewer")
            ],
            "comandos": (
                "# 1. Clonar el repositorio oficial del proyecto:\n"
                "git clone https://github.com/alenv514/ProyectoVisual.git\n"
                "cd ProyectoVisual\n\n"
                "# 2. Crear y cambiarse a su rama asignada:\n"
                "git checkout -b feature/basedatos-dao-mysql\n\n"
                "# 3. Agregar conexión y DAOs con commits:\n"
                "git add src/conexion/Conexion.java\n"
                "git commit -m \"feat(db): conexión MySQL localhost:3306/control_acceso_rfid vía XAMPP\"\n"
                "git add src/com/controlacceso/basededatos/EmpleadoDAO.java src/com/controlacceso/basededatos/AsistenciaDAO.java\n"
                "git commit -m \"feat(dao): interfaces EmpleadoDAO y AsistenciaDAO para patrón DAO\"\n"
                "git add src/com/controlacceso/basededatos/EmpleadoDAOMySql.java src/com/controlacceso/basededatos/AsistenciaDAOMySql.java\n"
                "git commit -m \"feat(dao): implementaciones MySQL de DAOs con DDL automático\"\n\n"
                "# 4. Subir la rama al repositorio de GitHub:\n"
                "git push -u origin feature/basedatos-dao-mysql\n"
                "# Ir a https://github.com/alenv514/ProyectoVisual y abrir el Pull Request a main."
            ),
            "prompt_ia": (
                "\"Actúa como un Especialista en Bases de Datos y Patrón DAO en Java. "
                "En mi proyecto grupal de Control de Acceso RFID, mi responsabilidad es la Capa de Persistencia con MySQL a través de XAMPP. "
                "La base de datos dedicada se llama 'control_acceso_rfid' en localhost:3306, con usuario 'root' y contraseña vacía. "
                "Necesito desacoplar totalmente la base de datos de la interfaz gráfica usando el Patrón DAO (Data Access Object). "
                "Por favor, genera:\n"
                "1. En el paquete 'conexion', la clase 'Conexion.java' con 'getConexion()' que cargue 'com.mysql.cj.jdbc.Driver' y conecte a 'jdbc:mysql://localhost:3306/control_acceso_rfid'. Debe incluir 'inicializarEsquema()' para auto-crear la base y las tablas en MySQL.\n"
                "2. 'EmpleadoDAO.java' y 'EmpleadoDAOMySql.java': Interfaz e implementación con métodos CRUD: guardar(Empleado), actualizar(Empleado), eliminar(int id), buscarPorUid(String uid), listarTodos(). Utilizar PreparedStatement y try-with-resources.\n"
                "3. 'AsistenciaDAO.java' y 'AsistenciaDAOMySql.java': Interfaz e implementación con métodos: registrar(RegistroAsistencia), listarHoy(), listarPorFiltros(fecha, nombre) y listarPorUsuarioYFecha(uid, fecha).\n"
                "4. Paquete 'Reportes' con plantilla 'ReporteAsistencias.jrxml' y clase 'GestorReportes' utilizando JasperReports y JasperViewer para visualización, impresión y exportación oficial a PDF.\""
            ),
            "defensa": [
                ("¿Por qué se creó una base de datos dedicada 'control_acceso_rfid' en MySQL?", 
                 "Porque aísla de forma profesional y segura los registros del sistema de control de acceso respecto a cualquier otro proyecto en phpMyAdmin, permitiendo una administración ordenada, independiente y limpia."),
                ("¿Qué ventaja aporta el Patrón DAO?", 
                 "Desacopla la lógica de negocio de la tecnología de base de datos. Gracias a este patrón, el cambio de SQLite a MySQL se realizó sin modificar ni una sola línea de la interfaz gráfica (VentanaPrincipal) ni de la lógica de turnos.")
            ]
        },
        {
            "num": "5",
            "nombre": "David",
            "rol": "Comunicaciones de Hardware y Puerto Serie USB (jSerialComm)",
            "rama": "feature/comunicacion-serial",
            "paquetes": "com.controlacceso.comunicacion_esp32 (SerialListener.java, SerialManager.java)",
            "commits": [
                ("Commit 1", "feat(serial): interfaz SerialListener para desacoplar eventos asíncronos de recepción de datos"),
                ("Commit 2", "feat(serial): clase SerialManager con auto-conexión y descarte inteligente de puertos Bluetooth"),
                ("Commit 3", "feat(serial): listener por eventos SerialPortMessageListener con delimitador de fin de línea"),
                ("Commit 4", "feat(serial): comunicación bidireccional para envío de comandos OK/ERR hacia el ESP32")
            ],
            "comandos": (
                "# 1. Clonar el repositorio oficial del proyecto:\n"
                "git clone https://github.com/alenv514/ProyectoVisual.git\n"
                "cd ProyectoVisual\n\n"
                "# 2. Crear y cambiarse a su rama asignada:\n"
                "git checkout -b feature/comunicacion-serial\n\n"
                "# 3. Agregar comunicación serial y confirmar commits:\n"
                "git add src/com/controlacceso/comunicacion_esp32/SerialListener.java\n"
                "git commit -m \"feat(serial): interfaz SerialListener para eventos asíncronos\"\n"
                "git add src/com/controlacceso/comunicacion_esp32/SerialManager.java\n"
                "git commit -m \"feat(serial): clase SerialManager con auto-conexión y parser por eventos\"\n\n"
                "# 4. Subir la rama al repositorio de GitHub:\n"
                "git push -u origin feature/comunicacion-serial\n"
                "# Ir a https://github.com/alenv514/ProyectoVisual y abrir el Pull Request a main."
            ),
            "prompt_ia": (
                "\"Actúa como un Desarrollador de Sistemas Embebidos y Comunicaciones en Java. "
                "En mi proyecto grupal de Control de Acceso RFID, mi rol es gestionar la comunicación serial USB "
                "entre la PC y el microcontrolador ESP32 mediante la librería 'jSerialComm 2.11.0'. "
                "Por favor, diséñame para el paquete 'com.controlacceso.comunicacion_esp32':\n"
                "1. 'SerialListener.java': Interfaz con onUidReceived(String uid), onPortStateChanged(boolean conectado, String mensaje), onLogMessage(String log).\n"
                "2. 'SerialManager.java': "
                "- Método estático 'obtenerPuertosDisponibles()' que filtre y descarte enlaces Bluetooth virtuales (COM3, COM4, COM5, COM6) y priorice adaptadores USB reales (CP210x, CH340, FTDI).\n"
                "- 'iniciarAutoConexion()' y 'detenerAutoConexion()' mediante hilo en segundo plano que detecte automáticamente el puerto COM a 115200 baudios sin intervención del usuario.\n"
                "- Uso de 'activePort.addDataListener(new SerialPortMessageListener())' delimitado por '\\n' para evitar problemas de timeout en reposo y evitar falsas desconexiones.\n"
                "- Método 'enviarRespuesta(boolean permitido, String nombre)' que transmita comandos 'OK:nombre' o 'ERR:nombre' hacia el ESP32.\""
            ),
            "defensa": [
                ("¿Cómo resolvió el problema de las falsas desconexiones cuando nadie pasa una tarjeta?", 
                 "Sustituimos el sondeo por BufferedReader con un SerialPortMessageListener basado en eventos. Este listener no tiene tiempos de espera artificiales; permanece dormido y Windows solo lo despierta cuando ingresa un paquete delimitado por salto de línea."),
                ("¿Por qué el sistema no intenta conectarse a los puertos Bluetooth?", 
                 "En obtenerPuertosDisponibles() filtramos las descripciones de los puertos ('port.getPortDescription()'). Si contiene 'bluetooth' o 'bth', se descarta, garantizando que el ESP32 (Silicon Labs CP210x) en COM7 se conecte al instante.")
            ]
        },
        {
            "num": "6",
            "nombre": "Semblantes",
            "rol": "Firmware del Microcontrolador ESP32 y Suite de Pruebas Unitarias JUnit 5",
            "rama": "feature/firmware-esp32-testing",
            "paquetes": "arduino/esp32_rfid_access/esp32_rfid_access.ino, test/com/controlacceso/ControlAccesoTest.java",
            "commits": [
                ("Commit 1", "feat(firmware): sketch C++ para ESP32 configurando bus SPI explícito (SCK 18, MISO 19, MOSI 23, SS 21)"),
                ("Commit 2", "feat(firmware): soporte WakeupA y calibración de ganancia de antena para clones RC522 (0xB2)"),
                ("Commit 3", "test(poo): pruebas unitarias JUnit 5 para validación de Herencia y Polimorfismo de Turnos"),
                ("Commit 4", "test(integracion): suite de pruebas automatizadas para operaciones DAO y verificación de plantilla JasperReports")
            ],
            "comandos": (
                "# 1. Clonar el repositorio oficial del proyecto:\n"
                "git clone https://github.com/alenv514/ProyectoVisual.git\n"
                "cd ProyectoVisual\n\n"
                "# 2. Crear y cambiarse a su rama asignada:\n"
                "git checkout -b feature/firmware-esp32-testing\n\n"
                "# 3. Agregar firmware C++ y pruebas unitarias con commits:\n"
                "git add arduino/esp32_rfid_access/esp32_rfid_access.ino\n"
                "git commit -m \"feat(firmware): código C++ ESP32 + MFRC522 con activación WakeupA\"\n"
                "git add test/com/controlacceso/ControlAccesoTest.java\n"
                "git commit -m \"test(unit): suite completa de pruebas unitarias JUnit 5 para POO y DAO\"\n\n"
                "# 4. Subir la rama al repositorio de GitHub:\n"
                "git push -u origin feature/firmware-esp32-testing\n"
                "# Ir a https://github.com/alenv514/ProyectoVisual y abrir el Pull Request a main."
            ),
            "prompt_ia": (
                "\"Actúa como un Ingeniero de QA y Firmware en Sistemas Embebidos (ESP32 + C++ / Java JUnit 5). "
                "En mi proyecto grupal de Control de Acceso RFID, mi responsabilidad abarca el código embebido del microcontrolador y el aseguramiento de calidad de software mediante pruebas automatizadas. "
                "Por favor, desarróllame:\n"
                "1. El archivo 'esp32_rfid_access.ino' en C++ para Arduino IDE con la librería MFRC522: "
                "Mapeo de pines SPI para ESP32 DevKit (SCK: 18, MISO: 19, MOSI: 23, SS/SDA: 21, RST: 22). Uso de 'SPI.begin(18,19,23,21)' y 'mfrc522.PICC_WakeupA()' para detectar tarjetas y llaveros clones (chip versión 0xB2). Envío del UID por Serial en formato 'UID:XXXXXX\\n' y recepción de comandos 'OK:' y 'ERR:' para activar LEDs y Buzzer.\n"
                "2. La clase de pruebas 'ControlAccesoTest.java' en 'test/com/controlacceso' con JUnit 5: "
                "- testHerenciaPersonaEmpleado(): Verifica que Empleado sea 'instanceof Persona'.\n"
                "- testPolimorfismoTurnos(): Valida que 20 min de atraso cobren $1.00 USD y 30 min antes cobren $1.50 USD.\n"
                "- testCrudEmpleadoDAO(): Prueba guardado, consulta por UID y eliminación en MySQL.\n"
                "- testControladorAccesoRfid(): Verifica accesos Permitidos vs Denegados.\n"
                "- testPlantillaJasperReports(): Comprueba la existencia y carga del archivo compilado .jasper de reportes.\""
            ),
            "defensa": [
                ("¿Por qué el lector RC522 no leía al inicio y cómo se solucionó en el firmware?", 
                 "El módulo del kit contiene un chip clon versión 0xB2. La función estándar PICC_IsNewCardPresent() no lo detectaba en reposo. Se implementó PICC_WakeupA() y PCD_AntennaOn() con pines SPI explícitos en el bus VSPI de ESP32, logrando lectura instantánea."),
                ("¿Por qué es importante incluir pruebas unitarias en un proyecto universitario?", 
                 "Garantizan que la lógica matemática del cobro ($3/hora) y la herencia POO funcionen de forma determinística antes de integrar con la interfaz gráfica, cumpliendo con los estándares de calidad de software.")
            ]
        }
    ]

    for ficha in fichas:
        doc.add_heading(f"3.{ficha['num']} Ficha de Trabajo: {ficha['nombre']}", level=2)
        
        p_info = doc.add_paragraph()
        r = p_info.add_run(f"• Rol: {ficha['rol']}\n")
        r.bold = True
        p_info.add_run(f"• Rama Git: {ficha['rama']}\n")
        p_info.add_run(f"• Paquetes / Archivos: {ficha['paquetes']}\n")

        # Tabla de Commits
        doc.add_paragraph("Secuencia de Commits Progresivos (Obligatorio realizar en este orden):").bold = True
        tbl_c = doc.add_table(rows=len(ficha['commits']) + 1, cols=2)
        tbl_c.alignment = WD_TABLE_ALIGNMENT.CENTER
        
        c0 = tbl_c.cell(0, 0)
        c0.width = Inches(1.5)
        set_cell_background(c0, "2B579A")
        r = c0.paragraphs[0].add_run("Commit")
        r.bold = True
        r.font.color.rgb = RGBColor(0xFF, 0xFF, 0xFF)

        c1 = tbl_c.cell(0, 1)
        c1.width = Inches(5.0)
        set_cell_background(c1, "2B579A")
        r = c1.paragraphs[0].add_run("Mensaje del Commit (Conventional Commits)")
        r.bold = True
        r.font.color.rgb = RGBColor(0xFF, 0xFF, 0xFF)

        for c_idx, (c_label, c_msg) in enumerate(ficha['commits'], start=1):
            row = tbl_c.rows[c_idx]
            bg_c = "F9FAFB" if c_idx % 2 == 1 else "EDF2F7"
            
            cell_lbl = row.cells[0]
            set_cell_background(cell_lbl, bg_c)
            set_cell_margins(cell_lbl, top=60, bottom=60, left=80, right=80)
            r_l = cell_lbl.paragraphs[0].add_run(c_label)
            r_l.bold = True

            cell_msg = row.cells[1]
            set_cell_background(cell_msg, bg_c)
            set_cell_margins(cell_msg, top=60, bottom=60, left=80, right=80)
            r_m = cell_msg.paragraphs[0].add_run(c_msg)
            r_m.font.name = "Consolas"
            r_m.font.size = Pt(8.5)

        doc.add_paragraph().paragraph_format.space_after = Pt(2)

        # Comandos Git
        doc.add_paragraph("Comandos de Terminal Git sugeridos:").bold = True
        add_code_block(doc, ficha['comandos'])

        # Prompt para IA
        doc.add_paragraph(f"Prompt listo para que {ficha['nombre']} copie y pegue en su IA (ChatGPT / Claude / Gemini):").bold = True
        add_callout(doc, f"PROMPT PARA IA - {ficha['nombre'].upper()}", ficha['prompt_ia'], bg_color="EEF2FF", border_color="4338CA")

        # Preguntas de Defensa
        doc.add_paragraph("Preguntas probables del docente durante la defensa:").bold = True
        for preg, resp in ficha['defensa']:
            p_q = doc.add_paragraph(style='List Bullet')
            r_q = p_q.add_run(f"Pregunta: {preg}\n")
            r_q.bold = True
            r_a = p_q.add_run(f"Respuesta técnica sugerida: {resp}")
            r_a.font.color.rgb = RGBColor(0x2D, 0x37, 0x48)

        doc.add_page_break()

    # -------------------------------------------------------------
    # SECCIÓN 4: PLAN B - EJECUCIÓN CENTRALIZADA POR ALEN
    # -------------------------------------------------------------
    h4 = doc.add_heading("4. PLAN B: GUÍA PARA QUE ALEN SIMULE LOS COMMITS DESDE SU PC", level=1)
    h4.paragraph_format.space_before = Pt(12)
    h4.paragraph_format.space_after = Pt(6)

    p_plan_b = doc.add_paragraph(
        "Si por falta de tiempo o problemas de configuración de Git en las computadoras de los compañeros necesitas "
        "subir todas las ramas desde tu propia computadora, Git te permite indicar el nombre y correo de GitHub de cada "
        "uno mediante la bandera '--author'. De esta forma, GitHub asignará automáticamente los commits a sus perfiles."
    )
    p_plan_b.paragraph_format.space_after = Pt(6)

    script_plan_b = (
        "# 1. Vincular tu carpeta local al repositorio oficial en GitHub:\n"
        "git init\n"
        "git remote add origin https://github.com/alenv514/ProyectoVisual.git\n"
        "git branch -M main\n"
        "git add build.xml manifest.mf nbproject lib .gitignore README.md\n"
        "git commit -m \"chore(setup): configuración inicial del proyecto Java Ant\"\n"
        "git push -u origin main\n\n"
        "# 2. Ejemplo para subir la rama de Evelyn y firmar como ella con su correo de GitHub:\n"
        "git checkout main\n"
        "git checkout -b feature/modelos-herencia\n"
        "git add src/com/controlacceso/modelos/Persona.java\n"
        "git commit --author=\"Evelyn <correo_github_de_evelyn@ejemplo.com>\" -m \"feat(modelos): clase abstracta Persona con atributos encapsulados\"\n"
        "git add src/com/controlacceso/modelos/Empleado.java\n"
        "git commit --author=\"Evelyn <correo_github_de_evelyn@ejemplo.com>\" -m \"feat(modelos): subclase Empleado aplicando principio de herencia\"\n"
        "git add src/com/controlacceso/modelos/RegistroAsistencia.java\n"
        "git commit --author=\"Evelyn <correo_github_de_evelyn@ejemplo.com>\" -m \"feat(modelos): entidad RegistroAsistencia para auditoría de marcaciones\"\n"
        "git push -u origin feature/modelos-herencia\n\n"
        "# 3. Ir a https://github.com/alenv514/ProyectoVisual y hacer clic en el botón verde 'Compare & pull request' para fusionarla a main."
    )
    add_code_block(doc, script_plan_b)

    add_callout(
        doc,
        "REQUISITO CLAVE PARA LA FOTO DE PERFIL EN GITHUB",
        "El correo que coloques dentro de '--author=\"Nombre <correo@ejemplo.com>\"' debe ser exactamente "
        "el mismo correo electrónico que tu compañero tiene registrado en su cuenta de GitHub. "
        "Al coincidir el correo, GitHub enlazará automáticamente su foto de perfil y sumará los commits a sus estadísticas.",
        bg_color="FEF2F2",
        border_color="DC2626"
    )

    # Guardar en las rutas requeridas
    for p in output_paths:
        doc.save(p)
        print(f"Documento guardado con éxito en: {p}")

if __name__ == "__main__":
    rutas = [
        r"C:\Users\Acer Predator\Desktop\Guia_Trabajo_Grupal_Control_Acceso_RFID.docx",
        r"C:\Users\Acer Predator\Documents\NetBeansProjects\ControlAccesoRFID\Guia_Trabajo_Grupal_Control_Acceso_RFID.docx"
    ]
    build_document(rutas)
