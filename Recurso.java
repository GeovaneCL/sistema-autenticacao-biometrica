package biometria;

public class Recurso {
    private int idRecurso;
    private String descricao;
    private String tipo;
    private int nivelSigilo;

    public Recurso(int idRecurso, String descricao, String tipo, int nivelSigilo){
        this.idRecurso = idRecurso;
        this.descricao = descricao;
        this.tipo = tipo;
        this.nivelSigilo = nivelSigilo;
    }

    public int getIdRecurso(){
        return idRecurso;
    }

    public String getDescricao(){
        return descricao;
    }

    public String getTipo(){
        return tipo;
    }

    public int getNivelSigilo(){
        return nivelSigilo;
    }
}
