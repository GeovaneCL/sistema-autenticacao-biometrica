package biometria;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import biometria.BiometriaException.Tipo;

public class PainelAcesso extends JFrame {

    private static final String REL_PROPRIEDADES = "Propriedades por situação";
    private static final String REL_RESUMO = "Resumo de acessos";
    private static final String REL_HISTORICO = "Histórico de acessos";
    private static final String REL_INVASAO = "Tentativas de invasão";

    private final UsuarioSistema usuario;
    private final JComboBox<String> comboRelatorio = new JComboBox<>();
    private final JTable tabelaRelatorio = new JTable();
    private final JLabel lblRelatorio = new JLabel(" ");
    private DefaultTableModel relatorioAtual; // guarda o último relatório gerado (para o CSV)

    public PainelAcesso(UsuarioSistema usuario) {
        this.usuario = usuario;

        setTitle("Ministério do Meio Ambiente - " + nomeDoPainel());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(750, 450);
        setLocationRelativeTo(null);

        JPanel raiz = new JPanel(new BorderLayout(10, 10));
        setContentPane(raiz);

        raiz.add(criarCabecalho(), BorderLayout.NORTH);

        // Cada aba mostra só o que o nível do usuário permite (WHERE nivel_sigilo <= ?)
        JTabbedPane abas = new JTabbedPane();
        abas.addTab("Recursos", criarAba(
                "SELECT id_recurso, descricao, tipo, nivel_sigilo FROM tb_recurso "
              + "WHERE nivel_sigilo <= ? ORDER BY id_recurso",
                new String[] {"ID", "Descrição", "Tipo", "Nível de sigilo"}));
        abas.addTab("Propriedades Rurais", criarAba(
                "SELECT id_propriedade, nome, proprietario, municipio, uf, area_hectares, situacao "
              + "FROM tb_propriedade_rural WHERE nivel_sigilo <= ? ORDER BY id_propriedade",
                new String[] {"ID", "Propriedade", "Proprietário", "Município", "UF",
                              "Área (ha)", "Situação"}));
        if (usuario.getNivelAcesso() >= 2) {
            abas.addTab("Relatórios", criarAbaRelatorios());
        }
        raiz.add(abas, BorderLayout.CENTER);

        raiz.add(criarRodape(), BorderLayout.SOUTH);
    }

    // Texto do painel conforme o nível de acesso
    private String nomeDoPainel() {
        return switch (usuario.getNivelAcesso()) {
            case 1 -> "Painel Geral (Nível 1)";
            case 2 -> "Painel de Diretoria (Nível 2)";
            case 3 -> "Painel do Ministro (Nível 3)";
            default -> "Painel (Nível " + usuario.getNivelAcesso() + ")";
        };
    }

    // Cor do cabeçalho conforme o nível de acesso
    private Color corDoNivel() {
        return switch (usuario.getNivelAcesso()) {
            case 1 -> new Color(33, 120, 190);   // azul
            case 2 -> new Color(210, 120, 20);   // laranja
            case 3 -> new Color(150, 30, 30);    // vinho
            default -> Color.DARK_GRAY;
        };
    }

    private JPanel criarCabecalho() {
        JPanel cabecalho = new JPanel(new GridLayout(2, 1, 0, 3));
        cabecalho.setBackground(corDoNivel());
        cabecalho.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        JLabel titulo = new JLabel(nomeDoPainel());
        titulo.setFont(new Font("SansSerif", Font.BOLD, 20));
        titulo.setForeground(Color.WHITE);

        JLabel info = new JLabel("Usuário: " + usuario.getNome()
                + "  |  Cargo: " + usuario.getCargo());
        info.setForeground(Color.WHITE);

        cabecalho.add(titulo);
        cabecalho.add(info);
        return cabecalho;
    }

    // Cria uma aba com uma tabela somente leitura preenchida por uma consulta
    private JScrollPane criarAba(String sql, String[] colunas) {
        DefaultTableModel modelo;
        try {
            modelo = consultar(sql, colunas, usuario.getNivelAcesso());
        } catch (BiometriaException ex) {
            ex.printStackTrace(); // detalhe técnico fica só no console
            JOptionPane.showMessageDialog(this,
                    "Não foi possível carregar os dados.",
                    "Erro no sistema", JOptionPane.ERROR_MESSAGE);
            modelo = new DefaultTableModel(colunas, 0);
        }

        JTable tabela = new JTable(modelo);
        tabela.setRowHeight(24);
        tabela.getColumnModel().getColumn(0).setMaxWidth(60);

        JScrollPane scroll = new JScrollPane(tabela);
        scroll.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));
        return scroll;
    }

    // Executa a consulta e devolve o resultado já no formato da tabela
    private DefaultTableModel consultar(String sql, String[] colunas, Integer parametro)
            throws BiometriaException {

        // Tabela somente leitura (o usuário não edita as células)
        DefaultTableModel modelo = new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int linha, int coluna) {
                return false;
            }
        };

        try (Connection conn = Conexao.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (parametro != null) {
                stmt.setInt(1, parametro);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Object[] linha = new Object[colunas.length];
                    for (int i = 0; i < colunas.length; i++) {
                        linha[i] = rs.getObject(i + 1);
                    }
                    modelo.addRow(linha);
                }
            }
        } catch (SQLException e) {
            throw new BiometriaException(Tipo.BANCO_DADOS,
                    "Falha ao consultar o banco de dados.", e);
        }

        return modelo;
    }

    // ---------------- Aba de relatórios (níveis 2 e 3) ----------------

    private JPanel criarAbaRelatorios() {
        JPanel aba = new JPanel(new BorderLayout(10, 10));
        aba.setBorder(BorderFactory.createEmptyBorder(10, 15, 0, 15));

        // Só oferece no menu o que o nível do usuário permite
        comboRelatorio.addItem(REL_PROPRIEDADES);
        comboRelatorio.addItem(REL_RESUMO);
        if (usuario.getNivelAcesso() >= 3) {
            comboRelatorio.addItem(REL_HISTORICO);
            comboRelatorio.addItem(REL_INVASAO);
        }

        JButton btnGerar = new JButton("Gerar");
        btnGerar.addActionListener(e -> gerarRelatorio());

        JButton btnExportar = new JButton("Exportar CSV");
        btnExportar.addActionListener(e -> exportarCsv());

        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT));
        barra.add(new JLabel("Relatório:"));
        barra.add(comboRelatorio);
        barra.add(btnGerar);
        barra.add(btnExportar);

        JPanel topo = new JPanel(new BorderLayout());
        topo.add(barra, BorderLayout.NORTH);
        topo.add(lblRelatorio, BorderLayout.SOUTH);

        tabelaRelatorio.setRowHeight(24);

        aba.add(topo, BorderLayout.NORTH);
        aba.add(new JScrollPane(tabelaRelatorio), BorderLayout.CENTER);
        return aba;
    }

    // Segunda barreira de segurança: além do menu, o código também confere o nível
    private void exigirNivel(int nivelNecessario) throws BiometriaException {
        if (usuario.getNivelAcesso() < nivelNecessario) {
            throw new BiometriaException(Tipo.ACESSO_NEGADO,
                    "Acesso negado: nível " + usuario.getNivelAcesso()
                    + " não pode acessar recurso de nível " + nivelNecessario + ".");
        }
    }

    private void gerarRelatorio() {
        String escolhido = (String) comboRelatorio.getSelectedItem();
        if (escolhido == null) {
            return;
        }

        try {
            DefaultTableModel modelo = switch (escolhido) {
                case REL_PROPRIEDADES -> consultar(
                        "SELECT situacao, COUNT(*), SUM(area_hectares) "
                      + "FROM tb_propriedade_rural WHERE nivel_sigilo <= ? "
                      + "GROUP BY situacao ORDER BY situacao",
                        new String[] {"Situação", "Quantidade", "Área total (ha)"},
                        usuario.getNivelAcesso());

                case REL_RESUMO -> {
                    exigirNivel(2);
                    yield consultar(
                        "SELECT resultado, COUNT(*) FROM tb_log_acesso "
                      + "GROUP BY resultado ORDER BY COUNT(*) DESC",
                        new String[] {"Resultado", "Quantidade"}, null);
                }

                case REL_HISTORICO -> {
                    exigirNivel(3);
                    yield consultar(
                        "SELECT l.data_hora, COALESCE(u.nome, '(não identificado)'), "
                      + "l.resultado, l.detalhe "
                      + "FROM tb_log_acesso l LEFT JOIN tb_usuario u ON u.id_usuario = l.id_usuario "
                      + "ORDER BY l.id_log DESC LIMIT 200",
                        new String[] {"Data/hora", "Usuário", "Resultado", "Detalhe"}, null);
                }

                case REL_INVASAO -> {
                    exigirNivel(3);
                    yield consultar(
                        "SELECT data_hora, resultado, detalhe FROM tb_log_acesso "
                      + "WHERE resultado IN ('BIOMETRIA_NEGADA', 'BLOQUEIO') "
                      + "ORDER BY id_log DESC LIMIT 200",
                        new String[] {"Data/hora", "Resultado", "Detalhe"}, null);
                }

                default -> null;
            };

            if (modelo == null) {
                return;
            }
            tabelaRelatorio.setModel(modelo);
            lblRelatorio.setText(escolhido + " - " + modelo.getRowCount() + " linha(s)");
            relatorioAtual = modelo;

        } catch (BiometriaException ex) {
            if (ex.getTipo() == Tipo.BANCO_DADOS) {
                ex.printStackTrace(); // detalhe técnico fica só no console
                JOptionPane.showMessageDialog(this,
                        "Não foi possível gerar o relatório.",
                        "Erro no sistema", JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, ex.getMessage(),
                        "Acesso negado", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void exportarCsv() {
        if (relatorioAtual == null) {
            JOptionPane.showMessageDialog(this, "Gere um relatório antes de exportar.",
                    "Exportar", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser seletor = new JFileChooser();
        seletor.setSelectedFile(new File("relatorio.csv"));
        if (seletor.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File arquivo = seletor.getSelectedFile();

        try (BufferedWriter escritor = Files.newBufferedWriter(arquivo.toPath(), StandardCharsets.UTF_8)) {
            escritor.write('\uFEFF'); // faz o Excel reconhecer os acentos

            List<String> cabecalho = new ArrayList<>();
            for (int c = 0; c < relatorioAtual.getColumnCount(); c++) {
                cabecalho.add(relatorioAtual.getColumnName(c));
            }
            escritor.write(String.join(";", cabecalho));
            escritor.newLine();

            for (int l = 0; l < relatorioAtual.getRowCount(); l++) {
                List<String> campos = new ArrayList<>();
                for (int c = 0; c < relatorioAtual.getColumnCount(); c++) {
                    campos.add(escapar(relatorioAtual.getValueAt(l, c)));
                }
                escritor.write(String.join(";", campos));
                escritor.newLine();
            }

            JOptionPane.showMessageDialog(this, "Relatório exportado para:\n" + arquivo.getAbsolutePath(),
                    "Exportar", JOptionPane.INFORMATION_MESSAGE);

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Não foi possível salvar o arquivo.",
                    "Erro ao exportar", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String escapar(Object valor) {
        String texto = (valor == null) ? "" : valor.toString();
        return "\"" + texto.replace("\"", "\"\"") + "\"";
    }

    private JPanel criarRodape() {
        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rodape.setBorder(BorderFactory.createEmptyBorder(0, 15, 10, 15));

        JButton btnSair = new JButton("Sair");
        btnSair.addActionListener(e -> {
            dispose();
            new TelaLogin().setVisible(true);
        });

        rodape.add(btnSair);
        return rodape;
    }
}
