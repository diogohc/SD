public class Client {
    private String nome;
    private String password;
    private String departamento;
    private int nrTelemovel;
    private String morada;
    private int nrCC;
    private String valCC;

    public Client() {

    }

    public Client(String username, String password) {
        setNome(username);
        setPassword(password);
    }

    public Client(String username, String password, String departamento, int nrTele, String morada,
                  int nrcc, String dataValCC) {
        setNome(username);
        setPassword(password);
        setDepartamento(departamento);
        setNrTelemovel(nrTele);
        setMorada(morada);
        setNrCC(nrcc);
        setValCC(dataValCC);
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDepartamento() {
        return departamento;
    }

    public void setDepartamento(String departamento) {
        this.departamento = departamento;
    }

    public int getNrTelemovel() {
        return nrTelemovel;
    }

    public void setNrTelemovel(int nrTelemovel) {
        this.nrTelemovel = nrTelemovel;
    }

    public String getMorada() {
        return morada;
    }

    public void setMorada(String morada) {
        this.morada = morada;
    }

    public int getNrCC() {
        return nrCC;
    }

    public void setNrCC(int nrCC) {
        this.nrCC = nrCC;
    }

    public String getValCC() {
        return valCC;
    }

    public void setValCC(String valCC) {
        this.valCC = valCC;
    }
}
