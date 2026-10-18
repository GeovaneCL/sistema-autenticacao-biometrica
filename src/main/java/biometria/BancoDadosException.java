package biometria;

public class BancoDadosException extends BiometriaException {
    public BancoDadosException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
