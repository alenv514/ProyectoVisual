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
 * Diálogo modal para visualizar la cámara en vivo del punto de acceso.
 * Permite al Administrador observar la persona frente al lector
 * y decidir si autoriza o deniega el paso.
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
        super(parent, "Cámara en Vivo - Administrador", true);
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
        setSize(700, 620);
        setLocationRelativeTo(getParent());
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(245, 247, 250));

        // 1. Encabezado moderno y limpio
        JPanel panelHeader = new JPanel(new BorderLayout());
        panelHeader.setBackground(new Color(27, 54, 93)); // Azul institucional
        panelHeader.setBorder(new EmptyBorder(12, 20, 12, 20));

        JLabel lblTitulo = new JLabel("📹 CÁMARA EN VIVO - CONTROL DE ACCESO");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitulo.setForeground(Color.WHITE);

        String nom = registro != null ? registro.getNombre() : "---";
        String uid = registro != null ? registro.getUid() : "---";
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

        // 2. Contenedor central: Pantalla completa de la Cámara
        JPanel panelCentro = new JPanel(new BorderLayout());
        panelCentro.setOpaque(false);
        panelCentro.setBorder(new EmptyBorder(15, 20, 15, 20));

        panelVideo = new PanelVideo();
        panelCentro.add(panelVideo, BorderLayout.CENTER);
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
