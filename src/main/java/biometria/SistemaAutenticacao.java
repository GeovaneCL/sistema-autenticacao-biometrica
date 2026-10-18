package biometria;

import java.io.File;
import java.nio.IntBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer;

import biometria.BiometriaException.Tipo;

//logica de comparação
public class SistemaAutenticacao {

    // Quanto MENOR a confiança, mais parecido. Acima disso o rosto é rejeitado.
    // Ajustar olhando as linhas "[FACE]" no console durante os testes.
    public static final double LIMIAR_CONFIANCA = 80.0;

    private static LBPHFaceRecognizer reconhecedor; // treinado na primeira vez que for usado

    public static boolean compararBiometria(String vetorEntrada, String vetorBD) {
        return vetorEntrada.equals(vetorBD);
    }

    //autenticar o usuario no bd Mysql
    public static UsuarioSistema autenticarUsuario(String vetorBiometricoEntrada)
            throws BiometriaException {

        if (vetorBiometricoEntrada == null || vetorBiometricoEntrada.isBlank()) {
            throw new BiometriaException(Tipo.LEITURA_INVALIDA,
                    "Leitura biométrica vazia ou inválida.");
        }

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
            throw new BiometriaException(Tipo.BANCO_DADOS,
                    "Falha ao acessar o banco de dados.", e);
        }

        throw new BiometriaException(Tipo.NAO_RECONHECIDA,
                "Biometria não reconhecida no sistema.");
    }

    // Reconhecimento facial (LBPH): compara os rostos capturados com os cadastrados na pasta 'faces'
    public static UsuarioSistema reconhecerFace(List<Mat> rostos) throws BiometriaException {
        treinarSeNecessario();

        int melhorId = -1;
        double melhorConfianca = Double.MAX_VALUE;

        // Usa o melhor resultado entre os quadros capturados (compensa quadros ruins da câmera)
        for (Mat rosto : rostos) {
            IntPointer rotulo = new IntPointer(1);
            DoublePointer confianca = new DoublePointer(1);
            reconhecedor.predict(rosto, rotulo, confianca);

            System.out.println("[FACE] id=" + rotulo.get(0) + " confiança=" + confianca.get(0));

            if (confianca.get(0) < melhorConfianca) {
                melhorConfianca = confianca.get(0);
                melhorId = rotulo.get(0);
            }
        }

        if (melhorId < 0 || melhorConfianca > LIMIAR_CONFIANCA) {
            throw new BiometriaException(Tipo.NAO_RECONHECIDA, "Rosto não reconhecido.");
        }
        return buscarUsuarioPorId(melhorId);
    }

    private static void treinarSeNecessario() throws BiometriaException {
        if (reconhecedor != null) {
            return;
        }

        File[] arquivos = new File(PreProcessamentoBiometrico.PASTA_ROSTOS)
                .listFiles((pasta, nome) -> nome.matches("\\d+_\\d+\\.png"));

        if (arquivos == null || arquivos.length == 0) {
            throw new BiometriaException(Tipo.FACIAL_INDISPONIVEL,
                    "Nenhum rosto cadastrado na pasta '" + PreProcessamentoBiometrico.PASTA_ROSTOS + "'.");
        }

        MatVector imagens = new MatVector(arquivos.length);
        Mat rotulos = new Mat(arquivos.length, 1, opencv_core.CV_32SC1);
        IntBuffer bufferRotulos = rotulos.createBuffer();

        for (int i = 0; i < arquivos.length; i++) {
            Mat imagem = opencv_imgcodecs.imread(arquivos[i].getAbsolutePath(),
                    opencv_imgcodecs.IMREAD_GRAYSCALE);
            imagens.put(i, imagem);
            // o nome do arquivo é id_n.png: o id do usuário é o que vem antes do "_"
            bufferRotulos.put(i, Integer.parseInt(arquivos[i].getName().split("_")[0]));
        }

        LBPHFaceRecognizer modelo = LBPHFaceRecognizer.create();
        modelo.train(imagens, rotulos);
        reconhecedor = modelo;
    }

    private static UsuarioSistema buscarUsuarioPorId(int idUsuario) throws BiometriaException {
        String sql = "SELECT id_usuario, nome, cargo, nivel_acesso, vetor_biometrico "
                   + "FROM tb_usuario WHERE id_usuario = ?";

        try (Connection conn = Conexao.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idUsuario);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new UsuarioSistema(
                        rs.getInt("id_usuario"),
                        rs.getString("nome"),
                        rs.getString("cargo"),
                        rs.getInt("nivel_acesso"),
                        rs.getString("vetor_biometrico")
                    );
                }
            }
        } catch (SQLException e) {
            throw new BiometriaException(Tipo.BANCO_DADOS,
                    "Falha ao acessar o banco de dados.", e);
        }

        throw new BiometriaException(Tipo.NAO_RECONHECIDA,
                "O rosto cadastrado não corresponde a nenhum usuário do banco.");
    }

    // Regra de negócio e acesso
    public static void solicitarAcessoAoRecurso(String vetorBiometricoEntrada, int idRecursoSolicitado) {
        System.out.println("--- INICIANDO AUTENTICAÇÃO E CONTROLE DE ACESSO ---");

        // Autenticar pela biometria usando UsuarioSistema
        UsuarioSistema usuario;
        try {
            usuario = autenticarUsuario(vetorBiometricoEntrada);
        } catch (BiometriaException e) {
            System.out.println("[ACESSO NEGADO] " + e.getMessage());
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
