package com.controlacceso.autenticacion;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Diálogo modal para la autenticación del Administrador.
 * Protege las pestañas de Gestión de Personal e Historial y Reportes.
 */
public class DialogoLoginAdmin extends JDialog {

    private JTextField txtUsuario;
    private JPasswordField txtPassword;
    private boolean autenticado = false;

    public DialogoLoginAdmin(Frame parent) {
        super(parent, "Autenticación de Administrador", true);
        initComponents();
    }

    private void initComponents() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        setLayout(new BorderLayout());

        // Panel Superior (Encabezado)
        JPanel panelHeader = new JPanel(new BorderLayout());
        panelHeader.setBackground(new Color(33, 43, 54));
        panelHeader.setBorder(new EmptyBorder(15, 20, 15, 20));

        JLabel lblTitulo = new JLabel("Acceso de Administrador");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitulo.setForeground(Color.WHITE);

        JLabel lblSubtitulo = new JLabel("Gestión de personal y reportes protegidos");
        lblSubtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSubtitulo.setForeground(new Color(189, 195, 199));

        panelHeader.add(lblTitulo, BorderLayout.NORTH);
        panelHeader.add(lblSubtitulo, BorderLayout.SOUTH);
        add(panelHeader, BorderLayout.NORTH);

        // Panel Central (Formulario)
        JPanel panelForm = new JPanel(new GridBagLayout());
        panelForm.setBorder(new EmptyBorder(20, 25, 15, 25));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblUser = new JLabel("Usuario:");
        lblUser.setFont(new Font("Segoe UI", Font.BOLD, 12));
        gbc.gridx = 0;
        gbc.gridy = 0;
        panelForm.add(lblUser, gbc);

        txtUsuario = new JTextField("admin", 16);
        txtUsuario.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        gbc.gridx = 1;
        gbc.gridy = 0;
        panelForm.add(txtUsuario, gbc);

        JLabel lblPass = new JLabel("Contraseña:");
        lblPass.setFont(new Font("Segoe UI", Font.BOLD, 12));
        gbc.gridx = 0;
        gbc.gridy = 1;
        panelForm.add(lblPass, gbc);

        txtPassword = new JPasswordField(16);
        txtPassword.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        gbc.gridx = 1;
        gbc.gridy = 1;
        panelForm.add(txtPassword, gbc);

        add(panelForm, BorderLayout.CENTER);

        // Panel Inferior (Botones)
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        panelBotones.setBorder(new EmptyBorder(0, 15, 10, 15));

        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnCancelar.addActionListener(e -> {
            autenticado = false;
            dispose();
        });

        JButton btnLogin = new JButton("Iniciar Sesión");
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnLogin.setForeground(new Color(15, 76, 129));
        btnLogin.addActionListener(e -> procesarLogin());

        panelBotones.add(btnCancelar);
        panelBotones.add(btnLogin);
        add(panelBotones, BorderLayout.SOUTH);

        // Enter para login en ambos campos
        KeyAdapter enterKey = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    procesarLogin();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    autenticado = false;
                    dispose();
                }
            }
        };
        txtUsuario.addKeyListener(enterKey);
        txtPassword.addKeyListener(enterKey);

        getRootPane().setDefaultButton(btnLogin);
        pack();
        setLocationRelativeTo(getParent());
    }

    private void procesarLogin() {
        String usuario = txtUsuario.getText().trim();
        String pass = new String(txtPassword.getPassword());

        if (usuario.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Por favor ingrese usuario y contraseña.",
                    "Campos requeridos",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (AdminAuthService.autenticar(usuario, pass)) {
            autenticado = true;
            dispose();
        } else {
            JOptionPane.showMessageDialog(this,
                    "Credenciales incorrectas.\nUsuario o contraseña inválidos.",
                    "Acceso Denegado",
                    JOptionPane.ERROR_MESSAGE);
            txtPassword.setText("");
            txtPassword.requestFocus();
        }
    }

    public boolean isAutenticado() {
        return autenticado;
    }
}
