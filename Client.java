public class Client {
    private String nome;
    private String password;
    private String departamento;
    private String nrTelemovel;
    private String morada;
    private String nrCC;
    private String valCC;
    private String diretoria_atual;

    public Client() {

    }

    public Client(String username, String password) {
        setNome(username);
        setPassword(password);
    }

    public Client(String username, String password, String departamento, String nrTele, String morada,
                  String nrcc, String dataValCC,String diretoria_atual) {
        setNome(username);
        setPassword(password);
        setDepartamento(departamento);
        setNrTelemovel(nrTele);
        setMorada(morada);
        setNrCC(nrcc);
        setValCC(dataValCC);
        setDiretoria_atual(diretoria_atual);
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

    public String getNrTelemovel() {
        return nrTelemovel;
    }

    public void setNrTelemovel(String nrTelemovel) {
        this.nrTelemovel = nrTelemovel;
    }

    public String getMorada() {
        return morada;
    }

    public void setMorada(String morada) {
        this.morada = morada;
    }

    public String getNrCC() {
        return nrCC;
    }

    public void setNrCC(String nrCC) {
        this.nrCC = nrCC;
    }

    public String getValCC() {
        return valCC;
    }

    public void setValCC(String valCC) {
        this.valCC = valCC;
    }

    public String getDiretoria_atual(){
        return this.diretoria_atual;
    }

    public void setDiretoria_atual(String diretoria_atual) {
        this.diretoria_atual = diretoria_atual;
    }

    public String clienteFicheiro(){
        String s;
        s=this.getNome()+","+this.getPassword()+","+this.getDepartamento()+","+this.getNrTelemovel()+","+
                this.getMorada()+","+this.getNrCC()+","+this.getValCC()+","+this.getDiretoria_atual()+"\n";
        return s;
    }
}
