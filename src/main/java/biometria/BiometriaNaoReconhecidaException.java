package biometria;

public class BiometriaNaoReconhecidaException extends BiometriaException {
    public BiometriaNaoReconhecidaException() {
        super("Biometria não reconhecida no sistema.");
    }
}
