package biometria;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PropriedadeDAO {

    public static List<PropriedadeRural> listarPorNivel(int nivelAcesso) throws BancoDadosException {
        String sql = "SELECT id_propriedade, nome, proprietario, municipio, uf, "
                   + "area_hectares, situacao, nivel_sigilo "
                   + "FROM tb_propriedade_rural WHERE nivel_sigilo <= ? ORDER BY id_propriedade";

        List<PropriedadeRural> lista = new ArrayList<>();

        try (Connection conn = Conexao.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, nivelAcesso);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(new PropriedadeRural(
                        rs.getInt("id_propriedade"),
                        rs.getString("nome"),
                        rs.getString("proprietario"),
                        rs.getString("municipio"),
                        rs.getString("uf"),
                        rs.getDouble("area_hectares"),
                        rs.getString("situacao"),
                        rs.getInt("nivel_sigilo")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new BancoDadosException("Falha ao listar as propriedades rurais.", e);
        }

        return lista;
    }
}