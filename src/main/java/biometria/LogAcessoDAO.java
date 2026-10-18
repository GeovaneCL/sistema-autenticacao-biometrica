package biometria;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

public class LogAcessoDAO {

    // Se o log falhar, o login não pode quebrar: só avisamos no console
    public static void registrar(Integer idUsuario, String resultado, String detalhe) {
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
}