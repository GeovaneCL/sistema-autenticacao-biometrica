package biometria;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.Mat;

import biometria.BiometriaException.Tipo;

public class TelaLogin extends JFrame {

    // Etapa 1: reconhecimento facial (3 tentativas). Etapa 2: vetor biométrico
    // digitado.
    private static final int MAX_TENTATIVAS_FACIAIS = 3;

    // Regra de bloqueio do vetor: 3 falhas seguidas bloqueiam o login por 30
    // segundos
    private static final int MAX_TENTATIVAS = 3;
    private static final long BLOQUEIO_MS = 30_000;

    // static: a contagem continua mesmo se o usuário sair do painel e voltar ao
    // login
    private static int falhas = 0;
    private static long bloqueadoAte = 0;

    private static final String CARTAO_FACIAL = "FACIAL";
    private static final String CARTAO_VETOR = "VETOR";

    // Paleta de cores da tela (mude aqui para trocar o visual inteiro)
    private static final Color COR_PRIMARIA = new Color(27, 94, 32);     // verde escuro
    private static final Color COR_FUNDO = new Color(245, 247, 245);     // cinza bem claro
    private static final Color COR_INATIVO = new Color(224, 229, 224);   // botão não selecionado
    private static final Color COR_TEXTO = new Color(40, 40, 40);
    private static final Font FONTE_BOTAO = new Font("SansSerif", Font.BOLD, 14);

    private final CardLayout cartoes = new CardLayout();
    private final JPanel areaCentral = new JPanel(cartoes);
    private final JLabel lblCamera = new JLabel("Câmera desligada", SwingConstants.CENTER);
    private final JButton btnFace = new JButton("Reconhecer rosto");
    private final JTextField campoVetor = new JTextField();
    private final JButton btnEntrar = new JButton("Entrar");
    private final JLabel lblStatus = new JLabel(" ", SwingConstants.CENTER);

    private final OpenCVFrameConverter.ToMat conversorMat = new OpenCVFrameConverter.ToMat();
    private final Java2DFrameConverter conversorImagem = new Java2DFrameConverter();

    private final JToggleButton tglFacial = new JToggleButton("Reconhecimento facial");
    private final JToggleButton tglVetor = new JToggleButton("Vetor biométrico");

    private int tentativasFaciais = 0;

    public TelaLogin() {
        setTitle("Ministério do Meio Ambiente - Acesso Biométrico");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(520, 640);
        setLocationRelativeTo(null); // centraliza na tela
        setResizable(false);

        // Painel raiz da janela
        JPanel raiz = new JPanel(new BorderLayout(0, 0));
        raiz.setBackground(COR_FUNDO);
        setContentPane(raiz);

        // Topo: faixa verde com o título + botões de escolha do método
        JPanel topo = new JPanel(new BorderLayout());
        topo.setBackground(COR_FUNDO);
        topo.add(criarCabecalho(), BorderLayout.NORTH);
        topo.add(criarSeletorMetodo(), BorderLayout.CENTER);
        raiz.add(topo, BorderLayout.NORTH);

        // Centro: dois "cartões" (só um aparece por vez)
        areaCentral.setBackground(COR_FUNDO);
        areaCentral.setBorder(BorderFactory.createEmptyBorder(15, 30, 10, 30));
        areaCentral.add(criarCartaoFacial(), CARTAO_FACIAL);
        areaCentral.add(criarCartaoVetor(), CARTAO_VETOR);
        raiz.add(areaCentral, BorderLayout.CENTER);

        // Rodapé: mensagem de status (compartilhada pelos dois cartões)
        lblStatus.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lblStatus.setBorder(BorderFactory.createEmptyBorder(0, 20, 20, 20));
        raiz.add(lblStatus, BorderLayout.SOUTH);

        btnFace.addActionListener(e -> iniciarReconhecimentoFacial());
        btnEntrar.addActionListener(e -> tentarLoginPorVetor());
        getRootPane().setDefaultButton(btnFace); // Enter aciona o botão do cartão visível
    }

    // ---------------- Visual ----------------

    // Faixa verde no topo com o nome do órgão e o título da tela
    private JPanel criarCabecalho() {
        JLabel orgao = new JLabel("MINISTÉRIO DO MEIO AMBIENTE", SwingConstants.CENTER);
        orgao.setFont(new Font("SansSerif", Font.BOLD, 12));
        orgao.setForeground(new Color(200, 230, 201));

        JLabel titulo = new JLabel("Autenticação Biométrica", SwingConstants.CENTER);
        titulo.setFont(new Font("SansSerif", Font.BOLD, 24));
        titulo.setForeground(Color.WHITE);

        JPanel cabecalho = new JPanel(new GridLayout(2, 1, 0, 4));
        cabecalho.setBackground(COR_PRIMARIA);
        cabecalho.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        cabecalho.add(orgao);
        cabecalho.add(titulo);
        return cabecalho;
    }

    // Dá a um botão o estilo da tela (cores, fonte, sem borda do tema padrão)
    private static void estilizarBotao(AbstractButton botao, Color fundo, Color texto) {
        botao.setBackground(fundo);
        botao.setForeground(texto);
        botao.setFont(FONTE_BOTAO);
        botao.setFocusPainted(false);
        botao.setContentAreaFilled(false);
        botao.setOpaque(true);
        botao.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        botao.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    // Botão selecionado fica verde; o outro fica cinza
    private void pintarSeletor() {
        estilizarBotao(tglFacial, tglFacial.isSelected() ? COR_PRIMARIA : COR_INATIVO,
                tglFacial.isSelected() ? Color.WHITE : COR_TEXTO);
        estilizarBotao(tglVetor, tglVetor.isSelected() ? COR_PRIMARIA : COR_INATIVO,
                tglVetor.isSelected() ? Color.WHITE : COR_TEXTO);
    }

    private JPanel criarCartaoFacial() {
        lblCamera.setPreferredSize(new Dimension(320, 240));
        lblCamera.setOpaque(true);
        lblCamera.setBackground(Color.BLACK);
        lblCamera.setForeground(Color.LIGHT_GRAY);
        lblCamera.setBorder(BorderFactory.createLineBorder(COR_PRIMARIA, 2));

        JPanel moldura = new JPanel(new FlowLayout(FlowLayout.CENTER));
        moldura.setBackground(COR_FUNDO);
        moldura.add(lblCamera);

        JLabel instrucao = new JLabel("Olhe para a câmera e clique em \"Reconhecer rosto\"",
                SwingConstants.CENTER);
        instrucao.setForeground(COR_TEXTO);

        estilizarBotao(btnFace, COR_PRIMARIA, Color.WHITE);

        JPanel cartao = new JPanel(new BorderLayout(5, 12));
        cartao.setBackground(COR_FUNDO);
        cartao.add(instrucao, BorderLayout.NORTH);
        cartao.add(moldura, BorderLayout.CENTER);
        cartao.add(btnFace, BorderLayout.SOUTH);
        return cartao;
    }

    private JPanel criarCartaoVetor() {
        JLabel rotulo = new JLabel("Vetor biométrico");
        rotulo.setFont(new Font("SansSerif", Font.BOLD, 14));
        rotulo.setForeground(COR_TEXTO);

        JLabel dica = new JLabel("Digite os valores separados por vírgula");
        dica.setForeground(Color.GRAY);

        campoVetor.setFont(new Font("Monospaced", Font.PLAIN, 15));
        campoVetor.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COR_PRIMARIA, 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));

        estilizarBotao(btnEntrar, COR_PRIMARIA, Color.WHITE);

        JPanel campos = new JPanel(new GridLayout(4, 1, 5, 8));
        campos.setBackground(COR_FUNDO);
        campos.add(rotulo);
        campos.add(dica);
        campos.add(campoVetor);
        campos.add(btnEntrar);

        JPanel cartao = new JPanel(new BorderLayout());
        cartao.setBackground(COR_FUNDO);
        cartao.add(campos, BorderLayout.NORTH);
        return cartao;
    }

    // ---------------- Escolha do método ----------------

    // Botões no topo para escolher o método de login
    private JPanel criarSeletorMetodo() {
        ButtonGroup grupo = new ButtonGroup();
        grupo.add(tglFacial);
        grupo.add(tglVetor);
        tglFacial.setSelected(true);
        pintarSeletor();

        tglFacial.addActionListener(e -> mostrarCartao(CARTAO_FACIAL));
        tglVetor.addActionListener(e -> mostrarCartao(CARTAO_VETOR));

        JPanel painel = new JPanel(new GridLayout(1, 2, 8, 0));
        painel.setBackground(COR_FUNDO);
        painel.setBorder(BorderFactory.createEmptyBorder(15, 30, 0, 30));
        painel.add(tglFacial);
        painel.add(tglVetor);
        return painel;
    }

    // Troca o cartão visível e mantém botões, foco e tecla Enter em sincronia
    private void mostrarCartao(String cartao) {
        cartoes.show(areaCentral, cartao);
        lblStatus.setForeground(Color.DARK_GRAY);
        lblStatus.setText(" ");

        if (CARTAO_VETOR.equals(cartao)) {
            tglVetor.setSelected(true);
            getRootPane().setDefaultButton(btnEntrar);
            campoVetor.requestFocusInWindow();
        } else {
            tglFacial.setSelected(true);
            getRootPane().setDefaultButton(btnFace);
        }
        pintarSeletor();
    }

    // ---------------- Etapa 1: reconhecimento facial ----------------

    private void iniciarReconhecimentoFacial() {
        try {
            verificarBloqueio();
        } catch (BiometriaException ex) {
            tratarErro(ex);
            return;
        }

        btnFace.setEnabled(false);
        tentativasFaciais++;
        lblStatus.setForeground(Color.DARK_GRAY);
        lblStatus.setText("Tentativa " + tentativasFaciais + " de " + MAX_TENTATIVAS_FACIAIS
                + ": olhe para a câmera...");

        // A captura roda em outra thread para a janela não congelar
        new Thread(() -> {
            try {
                List<Mat> rostos = new PreProcessamentoBiometrico().capturarRostos(3, this::mostrarQuadro);
                UsuarioSistema usuario = SistemaAutenticacao.reconhecerFace(rostos);
                SwingUtilities.invokeLater(() -> abrirPainel(usuario, "Login por reconhecimento facial"));
            } catch (BiometriaException ex) {
                SwingUtilities.invokeLater(() -> tratarFalhaFacial(ex));
            }
        }).start();
    }

    // Mostra cada quadro da câmera na tela (chamado pela thread de captura)
    private void mostrarQuadro(Mat quadro) {
        BufferedImage imagem = conversorImagem.convert(conversorMat.convert(quadro));
        if (imagem == null) {
            return;
        }
        Image reduzida = imagem.getScaledInstance(320, 240, Image.SCALE_FAST);
        SwingUtilities.invokeLater(() -> {
            lblCamera.setText(null);
            lblCamera.setIcon(new ImageIcon(reduzida));
        });
    }

    private void tratarFalhaFacial(BiometriaException ex) {
        // Sem câmera, sem detector ou sem rostos cadastrados: vai direto para o vetor
        if (ex.getTipo() == Tipo.FACIAL_INDISPONIVEL) {
            mostrarCartaoVetor("Reconhecimento facial indisponível:\n" + ex.getMessage()
                    + "\nInforme o vetor biométrico.");
            return;
        }

        registrarLog(null, "BIOMETRIA_NEGADA", "Reconhecimento facial falhou: " + ex.getMessage());

        if (tentativasFaciais >= MAX_TENTATIVAS_FACIAIS) {
            mostrarCartaoVetor("O reconhecimento facial falhou " + MAX_TENTATIVAS_FACIAIS
                    + " vezes.\nInforme o vetor biométrico.");
        } else {
            mostrarErro("Rosto não reconhecido",
                    ex.getMessage() + "\nTentativa " + tentativasFaciais + " de "
                            + MAX_TENTATIVAS_FACIAIS + ".",
                    JOptionPane.WARNING_MESSAGE);
            btnFace.setEnabled(true);
        }
    }

    private void mostrarCartaoVetor(String mensagem) {
        mostrarCartao(CARTAO_VETOR);
        lblStatus.setText("Informe o vetor biométrico.");
        JOptionPane.showMessageDialog(this, mensagem, "Reconhecimento facial",
                JOptionPane.INFORMATION_MESSAGE);
        campoVetor.requestFocusInWindow();
    }

    // ---------------- Etapa 2: vetor biométrico digitado ----------------

    private void tentarLoginPorVetor() {
        String vetor = campoVetor.getText().trim();

        try {
            verificarBloqueio();
            UsuarioSistema usuario = SistemaAutenticacao.autenticarUsuario(vetor);
            abrirPainel(usuario, "Login por vetor biométrico");

        } catch (BiometriaException ex) {
            tratarErro(ex);
        }
    }

    // Login concluído (por rosto ou por vetor): registra no log e abre o painel do
    // nível
    private void abrirPainel(UsuarioSistema usuario, String detalhe) {
        falhas = 0;
        registrarLog(usuario.getIdUsuario(), "ACESSO_CONCEDIDO", detalhe);

        lblStatus.setForeground(new Color(0, 128, 0));
        lblStatus.setText("Autenticado: " + usuario.getNome());

        new PainelAcesso(usuario).setVisible(true);
        dispose(); // fecha a tela de login
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
        lblStatus.setText("<html><div style='text-align:center'>"
                + mensagem.replace("\n", "<br>") + "</div></html>");
        JOptionPane.showMessageDialog(this, mensagem, titulo, tipo);
        repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TelaLogin().setVisible(true));
    }
}
