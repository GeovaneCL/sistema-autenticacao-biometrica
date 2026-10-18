package biometria;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RelatorioDAO {

    public static Relatorio propriedadesPorSituacao(int nivelAcesso) throws BancoDadosException {
        String sql = "SELECT situacao, COUNT(*), SUM(area_hectares) "
                   + "FROM tb_propriedade_rural WHERE nivel_sigilo <= ? "
                   + "GROUP BY situacao ORDER BY situacao";
        return consultar("Propriedades por situação",
                new String[] {"Situação", "Quantidade", "Área total (ha)"}, sql, nivelAcesso);
    }

    public static Relatorio resumoAcessos() throws BancoDadosException {
        String sql = "SELECT resultado, COUNT(*) FROM tb_log_acesso "
                   + "GROUP BY resultado ORDER BY COUNT(*) DESC";
        return consultar("Resumo de acessos",
                new String[] {"Resultado", "Quantidade"}, sql, null);
    }

    public static Relatorio historicoAcessos() throws BancoDadosException {
        String sql = "SELECT l.data_hora, COALESCE(u.nome, '(não identificado)'), "
                   + "l.resultado, l.detalhe "
                   + "FROM tb_log_acesso l LEFT JOIN tb_usuario u ON u.id_usuario = l.id_usuario "
                   + "ORDER BY l.id_log DESC LIMIT 200";
        return consultar("Histórico de acessos (últimos 200)",
                new String[] {"Data/hora", "Usuário", "Resultado", "Detalhe"}, sql, null);
    }

    public static Relatorio tentativasInvasao() throws BancoDadosException {
        String sql = "SELECT data_hora, resultado, detalhe FROM tb_log_acesso "
                   + "WHERE resultado IN ('BIOMETRIA_NEGADA', 'BLOQUEIO') "
                   + "ORDER BY id_log DESC LIMIT 200";
        return consultar("Tentativas de invasão (últimas 200)",
                new String[] {"Data/hora", "Resultado", "Detalhe"}, sql, null);
    }

    // Método auxiliar: executa a consulta e monta o Relatorio
    private static Relatorio consultar(String titulo, String[] colunas, String sql, Integer parametro)
            throws BancoDadosException {

        List<Object[]> linhas = new ArrayList<>();

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
                    linhas.add(linha);
                }
            }
        } catch (SQLException e) {
            throw new BancoDadosException("Falha ao gerar o relatório: " + titulo, e);
        }

        return new Relatorio(titulo, colunas, linhas);
    }
}