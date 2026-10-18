package biometria;

public class PropriedadeRural {
    private final int idPropriedade;
    private final String nome;
    private final String proprietario;
    private final String municipio;
    private final String uf;
    private final double areaHectares;
    private final String situacao;
    private final int nivelSigilo;

    public PropriedadeRural(int idPropriedade, String nome, String proprietario,
                            String municipio, String uf, double areaHectares,
                            String situacao, int nivelSigilo) {
        this.idPropriedade = idPropriedade;
        this.nome = nome;
        this.proprietario = proprietario;
        this.municipio = municipio;
        this.uf = uf;
        this.areaHectares = areaHectares;
        this.situacao = situacao;
        this.nivelSigilo = nivelSigilo;
    }

    public int getIdPropriedade() { return idPropriedade; }
    public String getNome() { return nome; }
    public String getProprietario() { return proprietario; }
    public String getMunicipio() { return municipio; }
    public String getUf() { return uf; }
    public double getAreaHectares() { return areaHectares; }
    public String getSituacao() { return situacao; }
    public int getNivelSigilo() { return nivelSigilo; }
}