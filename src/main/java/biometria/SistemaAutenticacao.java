package biometria;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

//logica de comparação
public class SistemaAutenticacao {
    public static boolean compararBiometria(String vetorEntrada, String vetorBD) {
        return vetorEntrada.equals(vetorBD);
    }

    //autenticar o usuario no bd Mysql
    public static UsuarioSistema autenticarUsuario(String vetorBiometricoEntrada){
        String sql = "SELECT id_usuario, nome, cargo, nivel_acesso, vetor_biometrico FROM tb_usuario";

        try(Connection conn = Conexao.conectar();
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()) {


                while (rs.next()) {
                    String vetorBD = rs.getString("vetor_biometrico");

                    if (compararBiometria(vetorBiometricoEntrada, vetorBD)){
                        return new UsuarioSistema(
                            rs.getInt("id_usuario"),
                            rs.getString("nome"),
                            rs.getString("cargo"),
                            rs.getInt("nivel_acesso"),
                            vetorBD
                        );
                    }
                }
            } catch (SQLException e) {
            System.err.println("Erro ao conectar no banco MySQL: " + e.getMessage());
        }

        return null; // Usuário não encontrado ou biometria incorreta
    }

    // Regra de negócio e acesso 
    public static void solicitarAcessoAoRecurso(String vetorBiometricoEntrada, int idRecursoSolicitado) {
        System.out.println("--- INICIANDO AUTENTICAÇÃO E CONTROLE DE ACESSO ---");

        // Autenticar pela biometria usando UsuarioSistema
        UsuarioSistema usuario = autenticarUsuario(vetorBiometricoEntrada);

        if (usuario == null) {
            System.out.println("[ACESSO NEGADO] Biometria não encontrada no sistema.");
            return;
        }

        System.out.println("[AUTENTICADO] Usuário: " + usuario.getNome() + 
                           " | Cargo: " + usuario.getCargo() + 
                           " | Nível: " + usuario.getNivelAcesso());

        // Buscar o recurso solicitado no banco
        String sqlRecurso = "SELECT id_recurso, descricao, tipo, nivel_sigilo FROM tb_recurso WHERE id_recurso = ?";

        try (Connection conn = Conexao.conectar();
             PreparedStatement stmt = conn.prepareStatement(sqlRecurso)) {

            stmt.setInt(1, idRecursoSolicitado);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Recurso recurso = new Recurso(
                    rs.getInt("id_recurso"),
                    rs.getString("descricao"),
                    rs.getString("tipo"),
                    rs.getInt("nivel_sigilo")
                );

                // Validar a permissão (Nível do Usuário >= Nível de Sigilo)
                if (usuario.getNivelAcesso() >= recurso.getNivelSigilo()) {
                    System.out.println("[ACESSO CONCEDIDO] Exibindo recurso: " + recurso.getDescricao());
                } else {
                    System.out.println("[ACESSO BLOQUEADO] Nível " + usuario.getNivelAcesso() + 
                                       " não tem acesso ao recurso de Nível " + recurso.getNivelSigilo());
                }
            } else {
                System.out.println("[ERRO] Recurso não encontrado no sistema.");
            }

        } catch (SQLException e) {
            System.err.println("Erro ao consultar recurso: " + e.getMessage());
        }
    }

    // Método principal para simular os requisitos do sistema
    public static void main(String[] args) {
        // teste: tentando acessar relatório com nivel 2 sendo do nivel 2
        String biometriaDiretora = "0.98,0.21,0.11,0.67";
        solicitarAcessoAoRecurso(biometriaDiretora, 102);

        System.out.println("\n--------------------------------------------------\n");

        // teste: tentando acessar Auditoria Confidencial nivel 3 sendo nível 1
        String biometriaAnalista = "0.12,0.45,0.89,0.33";
        solicitarAcessoAoRecurso(biometriaAnalista, 103);

        System.out.println("\n--------------------------------------------------\n");

        // teste: Ministro nível 3 tentando acessar Auditoria Confidencial nivel 3
        String biometriaMinistro = "0.55,0.77,0.33,0.99";
        solicitarAcessoAoRecurso(biometriaMinistro, 103);
    }
}
