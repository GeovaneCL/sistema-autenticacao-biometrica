package biometria;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

public class PainelRelatorios extends JPanel {

    private static final String REL_PROPRIEDADES = "Propriedades por situação";
    private static final String REL_RESUMO = "Resumo de acessos";
    private static final String REL_HISTORICO = "Histórico de acessos";
    private static final String REL_INVASAO = "Tentativas de invasão";

    private final UsuarioSistema usuario;
    private final JComboBox<String> comboRelatorio = new JComboBox<>();
    private final JTable tabela = new JTable();
    private final JLabel lblTitulo = new JLabel(" ");
    private Relatorio atual;

    public PainelRelatorios(UsuarioSistema usuario) {
        this.usuario = usuario;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 15, 0, 15));

        // Só oferece no menu o que o nível do usuário permite
        comboRelatorio.addItem(REL_PROPRIEDADES);
        comboRelatorio.addItem(REL_RESUMO);
        if (usuario.getNivelAcesso() >= 3) {
            comboRelatorio.addItem(REL_HISTORICO);
            comboRelatorio.addItem(REL_INVASAO);
        }

        JButton btnGerar = new JButton("Gerar");
        btnGerar.addActionListener(e -> gerar());

        JButton btnExportar = new JButton("Exportar CSV");
        btnExportar.addActionListener(e -> exportarCsv());

        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT));
        barra.add(new JLabel("Relatório:"));
        barra.add(comboRelatorio);
        barra.add(btnGerar);
        barra.add(btnExportar);

        JPanel topo = new JPanel(new BorderLayout());
        topo.add(barra, BorderLayout.NORTH);
        topo.add(lblTitulo, BorderLayout.SOUTH);

        tabela.setRowHeight(24);

        add(topo, BorderLayout.NORTH);
        add(new JScrollPane(tabela), BorderLayout.CENTER);
    }

    private void exigirNivel(int nivelNecessario) throws AcessoNegadoException {
        if (usuario.getNivelAcesso() < nivelNecessario) {
            throw new AcessoNegadoException(usuario.getNivelAcesso(), nivelNecessario);
        }
    }

    private void gerar() {
        String escolhido = (String) comboRelatorio.getSelectedItem();
        if (escolhido == null) {
            return;
        }

        try {
            Relatorio relatorio;
            switch (escolhido) {
                case REL_PROPRIEDADES:
                    relatorio = RelatorioDAO.propriedadesPorSituacao(usuario.getNivelAcesso());
                    break;
                case REL_RESUMO:
                    exigirNivel(2);
                    relatorio = RelatorioDAO.resumoAcessos();
                    break;
                case REL_HISTORICO:
                    exigirNivel(3);
                    relatorio = RelatorioDAO.historicoAcessos();
                    break;
                case REL_INVASAO:
                    exigirNivel(3);
                    relatorio = RelatorioDAO.tentativasInvasao();
                    break;
                default:
                    return;
            }
            mostrar(relatorio);

        } catch (AcessoNegadoException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Acesso negado", JOptionPane.WARNING_MESSAGE);

        } catch (BancoDadosException ex) {
            ex.printStackTrace(); // detalhe técnico fica só no console
            JOptionPane.showMessageDialog(this,
                    "Não foi possível gerar o relatório.",
                    "Erro no sistema", JOptionPane.ERROR_MESSAGE);

        } 
    }

    private void mostrar(Relatorio relatorio) {
        DefaultTableModel modelo = new DefaultTableModel(relatorio.getColunas(), 0) {
            @Override
            public boolean isCellEditable(int linha, int coluna) {
                return false;
            }
        };
        for (Object[] linha : relatorio.getLinhas()) {
            modelo.addRow(linha);
        }
        tabela.setModel(modelo);
        lblTitulo.setText(relatorio.getTitulo() + " - " + relatorio.getLinhas().size() + " linha(s)");
        atual = relatorio;
    }

    private void exportarCsv() {
        if (atual == null) {
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
            escritor.write(String.join(";", atual.getColunas()));
            escritor.newLine();

            for (Object[] linha : atual.getLinhas()) {
                List<String> campos = new ArrayList<>();
                for (Object valor : linha) {
                    campos.add(escapar(valor));
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
}