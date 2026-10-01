package biometria;

public class UsuarioSistema {
    private int idUsuario;
    private String nome;
    private String cargo;
    private int nivelAcesso;
    private String vetorBiometrico;

    public UsuarioSistema (int idUsuario, String nome, String cargo, int nivelAcesso, String vetorBiometrico) {
        this.idUsuario = idUsuario;
        this.nome = nome;
        this.cargo = cargo;
        this.nivelAcesso = nivelAcesso;
        this.vetorBiometrico = vetorBiometrico;
    }

    public int getIdUsuario() {
         return idUsuario; 
        }

    public String getNome() { 
        return nome; 
    }

    public String getCargo() {
         return cargo;
    }

    public int getNivelAcesso() { 
        return nivelAcesso; 
    }

    public String getVetorBiometrico() { 
        return vetorBiometrico; 
    }
}
