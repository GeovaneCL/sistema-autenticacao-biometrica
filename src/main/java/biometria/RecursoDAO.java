package biometria;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RecursoDAO {

    public static List<Recurso> listarPorNivel(int nivelAcesso) throws BancoDadosException {
        String sql = "SELECT id_recurso, descricao, tipo, nivel_sigilo "
                   + "FROM tb_recurso WHERE nivel_sigilo <= ? ORDER BY id_recurso";

        List<Recurso> lista = new ArrayList<>();

        try (Connection conn = Conexao.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, nivelAcesso);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Recurso(
                        rs.getInt("id_recurso"),
                        rs.getString("descricao"),
                        rs.getString("tipo"),
                        rs.getInt("nivel_sigilo")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new BancoDadosException("Falha ao listar os recursos.", e);
        }

        return lista;
    }
}