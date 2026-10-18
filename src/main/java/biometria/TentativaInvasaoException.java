package biometria;

public class TentativaInvasaoException extends BiometriaException {
    public TentativaInvasaoException(long segundosRestantes) {
        super("Acesso bloqueado por excesso de tentativas inválidas. "
              + "Aguarde " + segundosRestantes + " segundos.");
    }
}