package biometria;

public class BiometriaException extends Exception {

    // Tipos de erro tratados pelo sistema
    public enum Tipo {
        LEITURA_INVALIDA,   // vetor vazio ou ilegível
        NAO_RECONHECIDA,    // nenhum usuário corresponde à biometria
        ACESSO_NEGADO,      // nível de acesso insuficiente
        BANCO_DADOS,        // falha de conexão ou consulta
        BLOQUEIO            // login bloqueado por excesso de tentativas
    }

    private final Tipo tipo;

    public BiometriaException(Tipo tipo, String mensagem) {
        super(mensagem);
        this.tipo = tipo;
    }

    public BiometriaException(Tipo tipo, String mensagem, Throwable causa) {
        super(mensagem, causa);
        this.tipo = tipo;
    }

    public Tipo getTipo() {
        return tipo;
    }
}
