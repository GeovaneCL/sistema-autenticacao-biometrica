package biometria;

public class AcessoNegadoException extends BiometriaException {
    public AcessoNegadoException(int nivelUsuario, int nivelRecurso) {
        super("Acesso negado: nível " + nivelUsuario
              + " não pode acessar recurso de nível " + nivelRecurso + ".");
    }
}
