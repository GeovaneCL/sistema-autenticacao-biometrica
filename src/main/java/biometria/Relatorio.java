package biometria;

import java.util.List;

public class Relatorio {
    private final String titulo;
    private final String[] colunas;
    private final List<Object[]> linhas;

    public Relatorio(String titulo, String[] colunas, List<Object[]> linhas) {
        this.titulo = titulo;
        this.colunas = colunas;
        this.linhas = linhas;
    }

    public String getTitulo() { return titulo; }
    public String[] getColunas() { return colunas; }
    public List<Object[]> getLinhas() { return linhas; }
}