package biometria;

public class BiometriaException extends Exception {
    public BiometriaException(String mensagem) {
        super(mensagem);
    }

    public BiometriaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
