package com.controlacceso.vistas;

import com.controlacceso.modelos.Empleado;
import com.controlacceso.modelos.RegistroAsistencia;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;

/**
 * Diálogo modal para visualización lado a lado:
 * 1. Izquierda: Foto registrada e identidad del empleado en la Base de Datos.
 * 2. Derecha: Cámara en vivo en el punto de acceso RFID para comparación visual directa.
 */
public class DialogoVerificacionFacial extends JDialog {

    private final RegistroAsistencia registro;
    private final Empleado empleado;

    private boolean autorizado = false;
    private boolean rechazado = false;

    private volatile boolean camaraActiva = false;
    private Process procesoCamara = null;
    private Thread hiloCamara = null;

    private PanelVideo panelVideo;
    private JLabel lblEstadoCamara;

    public DialogoVerificacionFacial(JFrame parent, RegistroAsistencia reg, Empleado emp) {
        super(parent, "Verificación Facial - Administrador", true);
        this.registro = reg;
        this.empleado = emp;

        initComponents();
        iniciarTransmisionCamara();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                detenerCamara();
            }
        });
    }

    private void initComponents() {
        setSize(980, 640);
        setLocationRelativeTo(getParent());
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(245, 247, 250));

        // 1. Encabezado moderno y limpio
        JPanel panelHeader = new JPanel(new BorderLayout());
        panelHeader.setBackground(new Color(27, 54, 93)); // Azul institucional
        panelHeader.setBorder(new EmptyBorder(12, 20, 12, 20));

        JLabel lblTitulo = new JLabel("📹 VERIFICACIÓN FACIAL Y COMPARACIÓN DE IDENTIDAD");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitulo.setForeground(Color.WHITE);

        String nom = registro != null ? registro.getNombre() : (empleado != null ? empleado.getNombre() : "---");
        String uid = registro != null ? registro.getUid() : (empleado != null ? empleado.getUidTarjeta() : "---");
        String dep = (empleado != null && empleado.getDepartamento() != null) ? empleado.getDepartamento() : (registro != null ? registro.getDepartamento() : "-");

        JLabel lblSubtitulo = new JLabel("Persona: " + nom + "  |  UID: " + uid + "  |  Departamento: " + dep);
        lblSubtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSubtitulo.setForeground(new Color(200, 220, 245));

        lblEstadoCamara = new JLabel("● Iniciando cámara...");
        lblEstadoCamara.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblEstadoCamara.setForeground(new Color(230, 126, 34));

        JPanel panelTitulos = new JPanel(new GridLayout(2, 1, 0, 4));
        panelTitulos.setOpaque(false);
        panelTitulos.add(lblTitulo);
        panelTitulos.add(lblSubtitulo);

        panelHeader.add(panelTitulos, BorderLayout.WEST);
        panelHeader.add(lblEstadoCamara, BorderLayout.EAST);
        add(panelHeader, BorderLayout.NORTH);

        // 2. Contenedor central: Comparación Lado a Lado (Foto de Perfil vs Cámara en Vivo)
        JPanel panelCentro = new JPanel(new GridLayout(1, 2, 16, 0));
        panelCentro.setOpaque(false);
        panelCentro.setBorder(new EmptyBorder(14, 18, 14, 18));

        panelCentro.add(crearPanelFotoIdentidad());
        panelCentro.add(crearPanelCamara());
        add(panelCentro, BorderLayout.CENTER);

        // 3. Barra de Botones Inferior
        JPanel panelInferior = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 12));
        panelInferior.setBackground(Color.WHITE);
        panelInferior.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 225, 230)));

        JButton btnAprobar = new JButton("✔ Autorizar Paso");
        btnAprobar.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnAprobar.setBackground(new Color(39, 174, 96));
        btnAprobar.setForeground(Color.WHITE);
        btnAprobar.setOpaque(true);
        btnAprobar.setContentAreaFilled(true);
        btnAprobar.setBorderPainted(false);
        btnAprobar.setFocusPainted(false);
        btnAprobar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAprobar.setPreferredSize(new Dimension(200, 42));
        btnAprobar.addActionListener(e -> {
            autorizado = true;
            detenerCamara();
            dispose();
        });

        JButton btnRechazar = new JButton("✖ Denegar Paso");
        btnRechazar.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnRechazar.setBackground(new Color(192, 57, 43));
        btnRechazar.setForeground(Color.WHITE);
        btnRechazar.setOpaque(true);
        btnRechazar.setContentAreaFilled(true);
        btnRechazar.setBorderPainted(false);
        btnRechazar.setFocusPainted(false);
        btnRechazar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRechazar.setPreferredSize(new Dimension(200, 42));
        btnRechazar.addActionListener(e -> {
            rechazado = true;
            detenerCamara();
            dispose();
        });

        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnCancelar.setPreferredSize(new Dimension(100, 42));
        btnCancelar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCancelar.addActionListener(e -> {
            detenerCamara();
            dispose();
        });

        panelInferior.add(btnAprobar);
        panelInferior.add(btnRechazar);
        panelInferior.add(btnCancelar);
        add(panelInferior, BorderLayout.SOUTH);
    }

    private JPanel crearPanelFotoIdentidad() {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(220, 225, 230), 1, true),
            new EmptyBorder(12, 16, 12, 16)
        ));

        // Título del panel
        JPanel headerFoto = new JPanel(new GridLayout(2, 1, 0, 2));
        headerFoto.setOpaque(false);
        JLabel lblTituloFoto = new JLabel("👤 FOTO REGISTRADA (PERFIL OFICIAL)");
        lblTituloFoto.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTituloFoto.setForeground(new Color(40, 50, 70));

        JLabel lblSubFoto = new JLabel("Imagen y datos almacenados en la Base de Datos");
        lblSubFoto.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblSubFoto.setForeground(new Color(120, 130, 145));
        headerFoto.add(lblTituloFoto);
        headerFoto.add(lblSubFoto);
        card.add(headerFoto, BorderLayout.NORTH);

        // Foto centrada
        JLabel lblFoto = new JLabel();
        lblFoto.setHorizontalAlignment(SwingConstants.CENTER);
        lblFoto.setVerticalAlignment(SwingConstants.CENTER);
        lblFoto.setBackground(new Color(245, 247, 250));
        lblFoto.setOpaque(true);
        lblFoto.setBorder(new LineBorder(new Color(210, 215, 225), 1));
        lblFoto.setPreferredSize(new Dimension(240, 240));

        ImageIcon iconFoto = cargarFotoEmpleado();
        if (iconFoto != null) {
            lblFoto.setIcon(iconFoto);
        } else {
            lblFoto.setText("<html><center><font color='#888888' size='4'>📷<br>Sin fotografía<br>registrada</font></center></html>");
        }
        card.add(lblFoto, BorderLayout.CENTER);

        // Datos del empleado
        JPanel panelDatos = new JPanel(new GridLayout(5, 1, 0, 4));
        panelDatos.setOpaque(false);
        panelDatos.setBorder(new EmptyBorder(8, 4, 2, 4));

        String nombre = empleado != null && empleado.getNombre() != null ? empleado.getNombre() : (registro != null ? registro.getNombre() : "---");
        String cedula = empleado != null && empleado.getCedula() != null && !empleado.getCedula().isEmpty() ? empleado.getCedula() : "-";
        String uid = empleado != null && empleado.getUidTarjeta() != null ? empleado.getUidTarjeta() : (registro != null ? registro.getUid() : "-");
        String depto = empleado != null && empleado.getDepartamento() != null ? empleado.getDepartamento() : (registro != null ? registro.getDepartamento() : "-");
        String cargo = empleado != null && empleado.getCargo() != null && !empleado.getCargo().isEmpty() ? empleado.getCargo() : "-";

        panelDatos.add(crearFilaDato("Nombre:", nombre, true));
        panelDatos.add(crearFilaDato("Cédula / DNI:", cedula, false));
        panelDatos.add(crearFilaDato("UID Tarjeta:", uid, false));
        panelDatos.add(crearFilaDato("Departamento:", depto, false));
        panelDatos.add(crearFilaDato("Cargo:", cargo, false));

        card.add(panelDatos, BorderLayout.SOUTH);

        return card;
    }

    private ImageIcon cargarFotoEmpleado() {
        java.util.List<File> posiblesArchivos = new java.util.ArrayList<>();

        String rutaBd = empleado != null && empleado.getFoto() != null ? empleado.getFoto().trim() : null;
        String uid = empleado != null && empleado.getUidTarjeta() != null ? empleado.getUidTarjeta().trim() :
                     (registro != null && registro.getUid() != null ? registro.getUid().trim() : null);

        String[] baseDirs = new String[]{
            "",
            System.getProperty("user.dir") + File.separator,
            "C:/Users/Acer Predator/Documents/NetBeansProjects/ControlAccesoRFID/",
            "C:/xampp/htdocs/sistema rdf/"
        };

        for (String base : baseDirs) {
            if (rutaBd != null && !rutaBd.isEmpty()) {
                posiblesArchivos.add(new File(base + rutaBd));
                posiblesArchivos.add(new File(base + "fotos_empleados/" + new File(rutaBd).getName()));
            }
            if (uid != null && !uid.isEmpty()) {
                posiblesArchivos.add(new File(base + "fotos_empleados/" + uid + ".jpg"));
                posiblesArchivos.add(new File(base + "fotos_empleados/" + uid + ".png"));
                posiblesArchivos.add(new File(base + "fotos_empleados/" + uid + ".jpeg"));
            }
        }

        for (File f : posiblesArchivos) {
            if (f != null && f.exists() && f.isFile()) {
                try {
                    BufferedImage bimg = ImageIO.read(f);
                    if (bimg != null) {
                        int maxW = 260;
                        int maxH = 260;
                        double scale = Math.min((double) maxW / bimg.getWidth(), (double) maxH / bimg.getHeight());
                        int w = Math.max(1, (int) (bimg.getWidth() * scale));
                        int h = Math.max(1, (int) (bimg.getHeight() * scale));
                        Image scaled = bimg.getScaledInstance(w, h, Image.SCALE_SMOOTH);
                        return new ImageIcon(scaled);
                    }
                } catch (Exception ex) {
                    System.err.println("Error al cargar foto: " + ex.getMessage());
                }
            }
        }
        return null;
    }

    private JPanel crearFilaDato(String etiqueta, String valor, boolean resaltar) {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setOpaque(false);
        JLabel lblE = new JLabel(etiqueta);
        lblE.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblE.setForeground(new Color(90, 100, 115));
        lblE.setPreferredSize(new Dimension(95, 18));

        JLabel lblV = new JLabel(valor);
        lblV.setFont(new Font("Segoe UI", resaltar ? Font.BOLD : Font.PLAIN, resaltar ? 13 : 12));
        lblV.setForeground(resaltar ? new Color(20, 30, 50) : new Color(50, 60, 75));

        p.add(lblE, BorderLayout.WEST);
        p.add(lblV, BorderLayout.CENTER);
        return p;
    }

    private JPanel crearPanelCamara() {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(220, 225, 230), 1, true),
            new EmptyBorder(12, 16, 12, 16)
        ));

        // Título del panel
        JPanel headerCam = new JPanel(new GridLayout(2, 1, 0, 2));
        headerCam.setOpaque(false);
        JLabel lblTituloCam = new JLabel("📹 CÁMARA EN VIVO (PUNTO DE ACCESO)");
        lblTituloCam.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTituloCam.setForeground(new Color(40, 50, 70));

        JLabel lblSubCam = new JLabel("Observe a la persona en tiempo real para comparar");
        lblSubCam.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblSubCam.setForeground(new Color(120, 130, 145));
        headerCam.add(lblTituloCam);
        headerCam.add(lblSubCam);
        card.add(headerCam, BorderLayout.NORTH);

        panelVideo = new PanelVideo();
        panelVideo.setPreferredSize(new Dimension(380, 260));
        card.add(panelVideo, BorderLayout.CENTER);

        // Indicador inferior
        JPanel footerCam = new JPanel(new BorderLayout());
        footerCam.setOpaque(false);
        footerCam.setBorder(new EmptyBorder(6, 4, 2, 4));
        JLabel lblAyuda = new JLabel("⚠️ Compare los rasgos de la persona frente a la foto del perfil.");
        lblAyuda.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblAyuda.setForeground(new Color(211, 84, 0)); // Naranja
        footerCam.add(lblAyuda, BorderLayout.CENTER);
        card.add(footerCam, BorderLayout.SOUTH);

        return card;
    }

    private void iniciarTransmisionCamara() {
        camaraActiva = true;
        hiloCamara = new Thread(() -> {
            try {
                File scriptCam = new File("camara/cam_stream.py");
                if (!scriptCam.exists()) {
                    File alterno = new File(System.getProperty("user.dir"), "camara/cam_stream.py");
                    if (alterno.exists()) scriptCam = alterno;
                }

                ProcessBuilder pb = new ProcessBuilder("python", scriptCam.getAbsolutePath());
                pb.redirectErrorStream(false);
                procesoCamara = pb.start();

                SwingUtilities.invokeLater(() -> {
                    lblEstadoCamara.setText("● EN VIVO");
                    lblEstadoCamara.setForeground(new Color(39, 174, 96));
                });

                DataInputStream dis = new DataInputStream(procesoCamara.getInputStream());
                while (camaraActiva && procesoCamara.isAlive()) {
                    int tamano = dis.readInt();
                    if (tamano <= 0 || tamano > 2000000) continue;

                    byte[] buffer = new byte[tamano];
                    dis.readFully(buffer);

                    BufferedImage frame = ImageIO.read(new ByteArrayInputStream(buffer));
                    if (frame != null) {
                        panelVideo.actualizarFrame(frame);
                    }
                }
            } catch (Exception ex) {
                if (camaraActiva) {
                    SwingUtilities.invokeLater(() -> {
                        lblEstadoCamara.setText("● Cámara no disponible");
                        lblEstadoCamara.setForeground(new Color(192, 57, 43));
                        panelVideo.mostrarMensaje("Cámara no disponible o en uso por otra aplicación.");
                    });
                }
            }
        });
        hiloCamara.setDaemon(true);
        hiloCamara.start();
    }

    public synchronized void detenerCamara() {
        camaraActiva = false;
        if (procesoCamara != null && procesoCamara.isAlive()) {
            procesoCamara.destroyForcibly();
            procesoCamara = null;
        }
        if (hiloCamara != null) {
            hiloCamara.interrupt();
            hiloCamara = null;
        }
    }

    public boolean isAutorizado() {
        return autorizado;
    }

    public boolean isRechazado() {
        return rechazado;
    }

    /**
     * Componente gráfico personalizado para renderizar el flujo de video con escalado suave.
     */
    private static class PanelVideo extends JPanel {
        private BufferedImage frameActual = null;
        private String mensajeError = null;

        public PanelVideo() {
            setBackground(new Color(20, 25, 35));
            setBorder(new LineBorder(new Color(40, 50, 70), 2, true));
        }

        public void actualizarFrame(BufferedImage img) {
            this.frameActual = img;
            this.mensajeError = null;
            repaint();
        }

        public void mostrarMensaje(String msj) {
            this.frameActual = null;
            this.mensajeError = msj;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (frameActual != null) {
                int panelW = getWidth();
                int panelH = getHeight();
                int imgW = frameActual.getWidth();
                int imgH = frameActual.getHeight();

                double scale = Math.min((double) panelW / imgW, (double) panelH / imgH);
                int targetW = (int) (imgW * scale);
                int targetH = (int) (imgH * scale);
                int x = (panelW - targetW) / 2;
                int y = (panelH - targetH) / 2;

                g2.drawImage(frameActual, x, y, targetW, targetH, null);
            } else {
                g2.setColor(new Color(160, 175, 195));
                g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                String texto = (mensajeError != null) ? mensajeError : "Conectando con la cámara...";
                String[] lineas = texto.split("\n");
                FontMetrics fm = g2.getFontMetrics();
                int totalH = lineas.length * fm.getHeight();
                int startY = (getHeight() - totalH) / 2 + fm.getAscent();

                for (int i = 0; i < lineas.length; i++) {
                    int textW = fm.stringWidth(lineas[i]);
                    int x = (getWidth() - textW) / 2;
                    g2.drawString(lineas[i], x, startY + (i * fm.getHeight()));
                }
            }
            g2.dispose();
        }
    }
}
