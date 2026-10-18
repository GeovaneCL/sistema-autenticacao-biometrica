package biometria;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import biometria.BiometriaException.Tipo;

public class TelaLogin extends JFrame {

    // Regra de bloqueio: 3 falhas seguidas bloqueiam o login por 30 segundos
    private static final int MAX_TENTATIVAS = 3;
    private static final long BLOQUEIO_MS = 30_000;

    // static: a contagem continua mesmo se o usuário sair do painel e voltar ao login
    private static int falhas = 0;
    private static long bloqueadoAte = 0;

    private JTextField campoVetor;
    private JButton btnEntrar;
    private JLabel lblStatus;

    public TelaLogin() {
        setTitle("Ministério do Meio Ambiente - Acesso Biométrico");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 280);
        setLocationRelativeTo(null); // centraliza na tela
        setResizable(false);

        // Painel raiz da janela
        JPanel raiz = new JPanel(new BorderLayout(10, 10));
        setContentPane(raiz);

        // Topo: título
        JLabel titulo = new JLabel("Autenticação Biométrica", SwingConstants.CENTER);
        titulo.setFont(new Font("SansSerif", Font.BOLD, 20));
        titulo.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));
        raiz.add(titulo, BorderLayout.NORTH);

        // Centro: rótulo, campo, botão e status, cada um na sua linha
        JPanel conteudo = new JPanel(new GridLayout(4, 1, 5, 8));
        conteudo.setBorder(BorderFactory.createEmptyBorder(10, 30, 20, 30));

        campoVetor = new JTextField();
        btnEntrar = new JButton("Entrar");
        lblStatus = new JLabel(" ", SwingConstants.CENTER);

        conteudo.add(new JLabel("Vetor biométrico:"));
        conteudo.add(campoVetor);
        conteudo.add(btnEntrar);
        conteudo.add(lblStatus);
        raiz.add(conteudo, BorderLayout.CENTER);

        // Ação do botão
        btnEntrar.addActionListener(e -> tentarLogin());
        getRootPane().setDefaultButton(btnEntrar); // Enter também aciona o botão
    }

    private void tentarLogin() {
        String vetor = campoVetor.getText().trim();

        try {
            verificarBloqueio();

            UsuarioSistema usuario = SistemaAutenticacao.autenticarUsuario(vetor);

            falhas = 0;
            registrarLog(usuario.getIdUsuario(), "ACESSO_CONCEDIDO",
             "Login realizado com sucesso");

            lblStatus.setForeground(new Color(0, 128, 0));
            lblStatus.setText("Autenticado: " + usuario.getNome());

            new PainelAcesso(usuario).setVisible(true);
            dispose(); // fecha a tela de login

        } catch (BiometriaException ex) {
            tratarErro(ex);
        }
    }

    // Cada tipo de erro vira uma mensagem diferente para o usuário
    private void tratarErro(BiometriaException ex) {
        switch (ex.getTipo()) {
            case BLOQUEIO ->
                mostrarErro("Acesso bloqueado", ex.getMessage(), JOptionPane.ERROR_MESSAGE);

            case LEITURA_INVALIDA -> {
                registrarLog(null, "LEITURA_INVALIDA", ex.getMessage());
                mostrarErro("Leitura inválida", ex.getMessage(), JOptionPane.WARNING_MESSAGE);
            }

            case NAO_RECONHECIDA -> tratarFalhaBiometrica(ex);

            case BANCO_DADOS -> {
                ex.printStackTrace(); // detalhe técnico fica só no console
                mostrarErro("Erro no sistema",
                        "Não foi possível acessar o banco de dados.", JOptionPane.ERROR_MESSAGE);
            }

            default -> mostrarErro("Erro", ex.getMessage(), JOptionPane.ERROR_MESSAGE);
        }
    }

    // Só a biometria não reconhecida conta como tentativa de invasão
    private void tratarFalhaBiometrica(BiometriaException ex) {
        registrarLog(null, "BIOMETRIA_NEGADA", ex.getMessage());
        falhas++;

        if (falhas >= MAX_TENTATIVAS) {
            falhas = 0;
            bloqueadoAte = System.currentTimeMillis() + BLOQUEIO_MS;
            registrarLog(null, "BLOQUEIO", "Bloqueio após " + MAX_TENTATIVAS + " falhas seguidas");
            mostrarErro("Acesso bloqueado",
                    "Muitas tentativas inválidas. Acesso bloqueado por "
                    + (BLOQUEIO_MS / 1000) + " segundos.",
                    JOptionPane.ERROR_MESSAGE);
        } else {
            mostrarErro("Acesso negado",
                    ex.getMessage() + "\nTentativas restantes: " + (MAX_TENTATIVAS - falhas),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // Lança exceção se o login ainda estiver bloqueado
    private static void verificarBloqueio() throws BiometriaException {
        long restanteMs = bloqueadoAte - System.currentTimeMillis();
        if (restanteMs > 0) {
            long segundos = (restanteMs + 999) / 1000;
            throw new BiometriaException(Tipo.BLOQUEIO,
                    "Acesso bloqueado por excesso de tentativas inválidas. "
                    + "Aguarde " + segundos + " segundos.");
        }
    }

    // Se o log falhar, o login não pode quebrar: só avisamos no console
    private static void registrarLog(Integer idUsuario, String resultado, String detalhe) {
        String sql = "INSERT INTO tb_log_acesso (id_usuario, resultado, detalhe) VALUES (?, ?, ?)";

        try (Connection conn = Conexao.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (idUsuario == null) {
                stmt.setNull(1, Types.INTEGER);
            } else {
                stmt.setInt(1, idUsuario);
            }
            stmt.setString(2, resultado);
            stmt.setString(3, detalhe);
            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Não foi possível gravar o log de acesso: " + e.getMessage());
        }
    }

    private void mostrarErro(String titulo, String mensagem, int tipo) {
        lblStatus.setForeground(Color.RED);
        lblStatus.setText(mensagem);
        JOptionPane.showMessageDialog(this, mensagem, titulo, tipo);
        repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TelaLogin().setVisible(true));
    }
}