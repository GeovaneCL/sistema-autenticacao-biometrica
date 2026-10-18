package biometria;

public class ControleTentativas {

    public static final int MAX_TENTATIVAS = 3;
    public static final long BLOQUEIO_MS = 30_000; // 30 segundos

    private static int falhas = 0;
    private static long bloqueadoAte = 0;

    // Lança exceção se o login ainda estiver bloqueado
    public static void verificarBloqueio() throws TentativaInvasaoException {
        long restanteMs = bloqueadoAte - System.currentTimeMillis();
        if (restanteMs > 0) {
            throw new TentativaInvasaoException((restanteMs + 999) / 1000);
        }
    }

    // Registra uma falha. Retorna true se essa falha causou o bloqueio.
    public static boolean registrarFalha() {
        falhas++;
        if (falhas >= MAX_TENTATIVAS) {
            bloqueadoAte = System.currentTimeMillis() + BLOQUEIO_MS;
            falhas = 0;
            return true;
        }
        return false;
    }

    public static int tentativasRestantes() {
        return MAX_TENTATIVAS - falhas;
    }

    public static void registrarSucesso() {
        falhas = 0;
    }
}