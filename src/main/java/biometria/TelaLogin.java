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
            ControleTentativas.verificarBloqueio();

            UsuarioSistema usuario = SistemaAutenticacao.autenticarUsuario(vetor);

            ControleTentativas.registrarSucesso();
            LogAcessoDAO.registrar(usuario.getIdUsuario(), "ACESSO_CONCEDIDO",
                    "Login realizado com sucesso");

            lblStatus.setForeground(new Color(0, 128, 0));
            lblStatus.setText("Autenticado: " + usuario.getNome());

            new PainelAcesso(usuario).setVisible(true);
            dispose(); // fecha a tela de login

        } catch (TentativaInvasaoException ex) {
            mostrarErro("Acesso bloqueado", ex.getMessage(), JOptionPane.ERROR_MESSAGE);

        } catch (LeituraBiometricaException ex) {
            LogAcessoDAO.registrar(null, "LEITURA_INVALIDA", ex.getMessage());
            mostrarErro("Leitura inválida", ex.getMessage(), JOptionPane.WARNING_MESSAGE);

        } catch (BiometriaNaoReconhecidaException ex) {
            LogAcessoDAO.registrar(null, "BIOMETRIA_NEGADA", ex.getMessage());

            if (ControleTentativas.registrarFalha()) {
                LogAcessoDAO.registrar(null, "BLOQUEIO",
                        "Bloqueio após " + ControleTentativas.MAX_TENTATIVAS + " falhas seguidas");
                mostrarErro("Acesso bloqueado",
                        "Muitas tentativas inválidas. Acesso bloqueado por "
                                + (ControleTentativas.BLOQUEIO_MS / 1000) + " segundos.",
                        JOptionPane.ERROR_MESSAGE);
            } else {
                mostrarErro("Acesso negado",
                        ex.getMessage() + "\nTentativas restantes: "
                                + ControleTentativas.tentativasRestantes(),
                        JOptionPane.ERROR_MESSAGE);
            }

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