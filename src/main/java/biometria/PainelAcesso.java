package biometria;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.JTabbedPane;

public class PainelAcesso extends JFrame {

    private final UsuarioSistema usuario;

    public PainelAcesso(UsuarioSistema usuario) {
        this.usuario = usuario;

        setTitle("Ministério do Meio Ambiente - " + nomeDoPainel());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(750, 450);
        setLocationRelativeTo(null);

        JPanel raiz = new JPanel(new BorderLayout(10, 10));
        setContentPane(raiz);

        raiz.add(criarCabecalho(), BorderLayout.NORTH);
        JTabbedPane abas = new JTabbedPane();
        abas.addTab("Recursos", criarTabela());
        abas.addTab("Propriedades Rurais", criarTabelaPropriedades());
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

    private JScrollPane criarTabela() {
        String[] colunas = {"ID", "Descrição", "Tipo", "Nível de sigilo"};

        // Tabela somente leitura (o usuário não edita as células)
        DefaultTableModel modelo = new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int linha, int coluna) {
                return false;
            }
        };

        try {
            List<Recurso> recursos = RecursoDAO.listarPorNivel(usuario.getNivelAcesso());
            for (Recurso r : recursos) {
                modelo.addRow(new Object[] {
                    r.getIdRecurso(), r.getDescricao(), r.getTipo(), r.getNivelSigilo()
                });
            }
        } catch (BancoDadosException ex) {
            ex.printStackTrace(); // detalhe técnico fica só no console
            JOptionPane.showMessageDialog(this,
                "Não foi possível carregar os recursos.",
                "Erro no sistema", JOptionPane.ERROR_MESSAGE);
        }

        JTable tabela = new JTable(modelo);
        tabela.setRowHeight(24);
        tabela.getColumnModel().getColumn(0).setMaxWidth(60);
        tabela.getColumnModel().getColumn(3).setMaxWidth(110);

        JScrollPane scroll = new JScrollPane(tabela);
        scroll.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));
        return scroll;
    }
    private JScrollPane criarTabelaPropriedades() {
    String[] colunas = {"ID", "Propriedade", "Proprietário", "Município", "UF",
                        "Área (ha)", "Situação"};

    DefaultTableModel modelo = new DefaultTableModel(colunas, 0) {
        @Override
        public boolean isCellEditable(int linha, int coluna) {
            return false;
        }
    };

    try {
        List<PropriedadeRural> propriedades =
            PropriedadeDAO.listarPorNivel(usuario.getNivelAcesso());
        for (PropriedadeRural p : propriedades) {
            modelo.addRow(new Object[] {
                p.getIdPropriedade(), p.getNome(), p.getProprietario(),
                p.getMunicipio(), p.getUf(), p.getAreaHectares(), p.getSituacao()
            });
        }
    } catch (BancoDadosException ex) {
        ex.printStackTrace(); // detalhe técnico fica só no console
        JOptionPane.showMessageDialog(this,
            "Não foi possível carregar as propriedades rurais.",
            "Erro no sistema", JOptionPane.ERROR_MESSAGE);
    }

    JTable tabela = new JTable(modelo);
    tabela.setRowHeight(24);
    tabela.getColumnModel().getColumn(0).setMaxWidth(50);
    tabela.getColumnModel().getColumn(4).setMaxWidth(45);

    JScrollPane scroll = new JScrollPane(tabela);
    scroll.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));
    return scroll;
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