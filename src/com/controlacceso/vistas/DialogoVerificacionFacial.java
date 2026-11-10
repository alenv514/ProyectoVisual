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
 * Diálogo modal para la verificación visual con cámara web en tiempo real.
 * Se activa en la 3ra marcación para que el Administrador valide si la persona
 * frente al lector coincide con la identidad registrada antes de permitir el paso.
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
        super(parent, "Verificación Visual de Seguridad - Administrador", true);
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
        setSize(860, 580);
        setLocationRelativeTo(getParent());
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(245, 247, 250));

        // 1. Encabezado moderno
        JPanel panelHeader = new JPanel(new BorderLayout());
        panelHeader.setBackground(new Color(27, 54, 93)); // Azul institucional
        panelHeader.setBorder(new EmptyBorder(15, 20, 15, 20));

        JLabel lblTitulo = new JLabel("🛡 VERIFICACIÓN VISUAL DE IDENTIDAD (CÁMARA EN VIVO)");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblTitulo.setForeground(Color.WHITE);

        JLabel lblSubtitulo = new JLabel("Tercera marcación del día detectada. Verifique que la persona frente a la cámara sea el titular de la tarjeta.");
        lblSubtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSubtitulo.setForeground(new Color(200, 220, 245));

        panelHeader.add(lblTitulo, BorderLayout.NORTH);
        panelHeader.add(lblSubtitulo, BorderLayout.SOUTH);
        add(panelHeader, BorderLayout.NORTH);

        // 2. Contenedor central (Comparación lado a lado)
        JPanel panelCentro = new JPanel(new GridLayout(1, 2, 20, 0));
        panelCentro.setOpaque(false);
        panelCentro.setBorder(new EmptyBorder(15, 20, 15, 20));

        // --- Panel Izquierdo: Ficha del Empleado Registrado ---
        JPanel panelIzquierdo = new JPanel(new BorderLayout(10, 10));
        panelIzquierdo.setBackground(Color.WHITE);
        panelIzquierdo.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(210, 215, 225), 1, true),
                new EmptyBorder(15, 15, 15, 15)
        ));

        JLabel lblTituloReg = new JLabel("DATOS DEL TITULAR REGISTRADO", SwingConstants.CENTER);
        lblTituloReg.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTituloReg.setForeground(new Color(27, 54, 93));
        panelIzquierdo.add(lblTituloReg, BorderLayout.NORTH);

        // Avatar gráfico con silueta e iniciales
        JPanel panelAvatar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int size = 110;
                int x = (getWidth() - size) / 2;
                int y = 10;
                g2.setColor(new Color(41, 128, 185));
                g2.fillOval(x, y, size, size);

                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 36));
                String iniciales = obtenerIniciales(registro != null ? registro.getNombre() : "U");
                FontMetrics fm = g2.getFontMetrics();
                int tx = x + (size - fm.stringWidth(iniciales)) / 2;
                int ty = y + ((size - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(iniciales, tx, ty);
                g2.dispose();
            }
        };
        panelAvatar.setPreferredSize(new Dimension(130, 130));
        panelAvatar.setOpaque(false);

        // Tabla de datos del titular
        JPanel panelDatos = new JPanel(new GridLayout(5, 1, 6, 6));
        panelDatos.setOpaque(false);

        String nom = registro != null ? registro.getNombre() : "---";
        String uid = registro != null ? registro.getUid() : "---";
        String dep = (empleado != null && empleado.getDepartamento() != null) ? empleado.getDepartamento() : (registro != null ? registro.getDepartamento() : "-");
        String ced = (empleado != null && empleado.getCedula() != null) ? empleado.getCedula() : "Registrado en BD";

        panelDatos.add(crearFilaDato("Nombre Oficial:", nom));
        panelDatos.add(crearFilaDato("Cédula / ID:", ced));
        panelDatos.add(crearFilaDato("UID Tarjeta:", uid));
        panelDatos.add(crearFilaDato("Departamento:", dep));
        panelDatos.add(crearFilaDato("Estado:", "Turno previo cumplido (3ª Marcación)"));

        JPanel contenedorIzquierdo = new JPanel(new BorderLayout(5, 10));
        contenedorIzquierdo.setOpaque(false);
        contenedorIzquierdo.add(panelAvatar, BorderLayout.NORTH);
        contenedorIzquierdo.add(panelDatos, BorderLayout.CENTER);

        panelIzquierdo.add(contenedorIzquierdo, BorderLayout.CENTER);
        panelCentro.add(panelIzquierdo);

        // --- Panel Derecho: Cámara en Vivo (Webcam) ---
        JPanel panelDerecho = new JPanel(new BorderLayout(5, 5));
        panelDerecho.setBackground(Color.WHITE);
        panelDerecho.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(210, 215, 225), 1, true),
                new EmptyBorder(15, 15, 15, 15)
        ));

        JPanel panelTituloCam = new JPanel(new BorderLayout());
        panelTituloCam.setOpaque(false);
        JLabel lblTituloCam = new JLabel("CÁMARA DEL PUNTO DE ACCESO");
        lblTituloCam.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTituloCam.setForeground(new Color(27, 54, 93));

        lblEstadoCamara = new JLabel("● Iniciando cámara...");
        lblEstadoCamara.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblEstadoCamara.setForeground(new Color(230, 126, 34));

        panelTituloCam.add(lblTituloCam, BorderLayout.WEST);
        panelTituloCam.add(lblEstadoCamara, BorderLayout.EAST);
        panelDerecho.add(panelTituloCam, BorderLayout.NORTH);

        panelVideo = new PanelVideo();
        panelDerecho.add(panelVideo, BorderLayout.CENTER);

        panelCentro.add(panelDerecho);
        add(panelCentro, BorderLayout.CENTER);

        // 3. Barra de Botones Inferior
        JPanel panelInferior = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 15));
        panelInferior.setBackground(Color.WHITE);
        panelInferior.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 225, 230)));

        JButton btnAprobar = new JButton("✔ Coincide: Autorizar Paso");
        btnAprobar.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnAprobar.setBackground(new Color(39, 174, 96));
        btnAprobar.setForeground(Color.WHITE);
        btnAprobar.setOpaque(true);
        btnAprobar.setContentAreaFilled(true);
        btnAprobar.setBorderPainted(false);
        btnAprobar.setFocusPainted(false);
        btnAprobar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAprobar.setPreferredSize(new Dimension(240, 42));
        btnAprobar.addActionListener(e -> {
            autorizado = true;
            detenerCamara();
            dispose();
        });

        JButton btnRechazar = new JButton("✖ No Coincide: Rechazar Suplantación");
        btnRechazar.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnRechazar.setBackground(new Color(192, 57, 43));
        btnRechazar.setForeground(Color.WHITE);
        btnRechazar.setOpaque(true);
        btnRechazar.setContentAreaFilled(true);
        btnRechazar.setBorderPainted(false);
        btnRechazar.setFocusPainted(false);
        btnRechazar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRechazar.setPreferredSize(new Dimension(280, 42));
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

    private JPanel crearFilaDato(String etiqueta, String valor) {
        JPanel fila = new JPanel(new BorderLayout(5, 0));
        fila.setOpaque(false);
        JLabel lblEt = new JLabel(etiqueta);
        lblEt.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblEt.setForeground(new Color(100, 110, 125));
        lblEt.setPreferredSize(new Dimension(120, 20));

        JLabel lblVal = new JLabel(valor);
        lblVal.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblVal.setForeground(new Color(40, 45, 55));

        fila.add(lblEt, BorderLayout.WEST);
        fila.add(lblVal, BorderLayout.CENTER);
        return fila;
    }

    private String obtenerIniciales(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) return "U";
        String[] partes = nombre.trim().split("\\s+");
        if (partes.length == 1) return partes[0].substring(0, Math.min(2, partes[0].length())).toUpperCase();
        return ("" + partes[0].charAt(0) + partes[1].charAt(0)).toUpperCase();
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
                        panelVideo.mostrarMensaje("Cámara no disponible o en uso por otra app.\nVerifique presencialmente en ventanilla.");
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
            setBorder(new LineBorder(new Color(40, 50, 70), 2));
        }

        public void actualizarFrame(BufferedImage img) {
            this.frameActual = img;
            this.mensajeError = null;
            repaint();
        }

        public void mostrarMensaje(String msg) {
            this.mensajeError = msg;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            if (frameActual != null) {
                // Escalar preservando la relación de aspecto
                int pw = getWidth();
                int ph = getHeight();
                int iw = frameActual.getWidth();
                int ih = frameActual.getHeight();

                double scale = Math.min((double) pw / iw, (double) ph / ih);
                int nw = (int) (iw * scale);
                int nh = (int) (ih * scale);
                int x = (pw - nw) / 2;
                int y = (ph - nh) / 2;

                g2.drawImage(frameActual, x, y, nw, nh, null);
            } else if (mensajeError != null) {
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
                FontMetrics fm = g2.getFontMetrics();
                String[] lineas = mensajeError.split("\n");
                int y = (getHeight() - (lineas.length * 20)) / 2;
                for (String linea : lineas) {
                    int x = (getWidth() - fm.stringWidth(linea)) / 2;
                    g2.drawString(linea, x, y);
                    y += 22;
                }
            } else {
                g2.setColor(new Color(100, 115, 135));
                g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                String texto = "Conectando con la cámara...";
                int x = (getWidth() - g2.getFontMetrics().stringWidth(texto)) / 2;
                int y = getHeight() / 2;
                g2.drawString(texto, x, y);
            }
            g2.dispose();
        }
    }
}
