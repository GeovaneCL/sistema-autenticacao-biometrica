package biometria;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class TelaLogin extends JFrame {

    private JTextField campoVetor;
    private JButton btnEntrar;
    private JLabel lblStatus;

    public TelaLogin() {
        setTitle("Ministério do Meio Ambiente - Acesso Biométrico");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 280);
        setLocationRelativeTo(null); // centraliza na tela
        setResizable(false);
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
            UsuarioSistema usuario = SistemaAutenticacao.autenticarUsuario(vetor);

            lblStatus.setForeground(new Color(0, 128, 0));
            lblStatus.setText("Autenticado: " + usuario.getNome());

            // Vamos abrir o painel do nível de acesso.
            // Por enquanto, só mostramos os dados do usuário.
            JOptionPane.showMessageDialog(this,
                    "Bem-vindo(a), " + usuario.getNome()
                            + "\nCargo: " + usuario.getCargo()
                            + "\nNível de acesso: " + usuario.getNivelAcesso(),
                    "Acesso liberado", JOptionPane.INFORMATION_MESSAGE);
            repaint();

        } catch (LeituraBiometricaException ex) {
            mostrarErro("Leitura inválida", ex.getMessage(), JOptionPane.WARNING_MESSAGE);

        } catch (BiometriaNaoReconhecidaException ex) {
            mostrarErro("Acesso negado", ex.getMessage(), JOptionPane.ERROR_MESSAGE);

        } catch (BancoDadosException ex) {
            ex.printStackTrace(); // detalhe técnico fica só no console
            mostrarErro("Erro no sistema",
                    "Não foi possível acessar o banco de dados.", JOptionPane.ERROR_MESSAGE);

        } catch (BiometriaException ex) {
            mostrarErro("Erro", ex.getMessage(), JOptionPane.ERROR_MESSAGE);
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