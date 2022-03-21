
import java.net.*;
import java.io.*;
import java.util.ArrayList;
;


public class TCPServer {
    private static int serverPort = 6000;
    private static int serverPing = 50;
    private ArrayList<Client> listaClientes = new ArrayList<>();

    public TCPServer() {
    }

    private void iniciar(int numero) throws FileNotFoundException {
        //ler ficheiro de configuracao de clientes e adiciona na arraylist
        le_ficheiro();

        try (ServerSocket listenSocket = new ServerSocket(serverPort)) {
            System.out.println("A escuta no porto 6000");
            System.out.println("LISTEN SOCKET=" + listenSocket);
            while (true) {
                Socket clientSocket = listenSocket.accept(); // BLOQUEANTE
                System.out.println("Novo cliente conectado");
                new Connection(clientSocket, numero);
                numero++;
            }
        } catch (IOException e) {
            System.out.println("Listen:" + e.getMessage());
        }
    }


    public static void main(String[] args) throws FileNotFoundException {
        int numero = 0;
        TCPServer server = new TCPServer();
        server.iniciar(numero);

    }

    public void le_ficheiro() {
        File ficheiro = new File("config_clients.txt");
        try {
            FileReader fr = new FileReader(ficheiro);
            BufferedReader br = new BufferedReader(fr);
            String line;
            String[] campos;
            while ((line = br.readLine()) != null) {
                campos = line.split(",");
                Client aux = new Client(campos[0], campos[1], campos[2], Integer.parseInt(campos[3]), campos[4],
                Integer.parseInt(campos[5]), campos[6],campos[7] + "\\" + campos[0]);
                this.listaClientes.add(aux);
            }
            br.close();
        } catch (IOException e) {
            System.out.println("Erro! Ficheiro nao foi encontrado");
            e.printStackTrace();
        }
    }

    /**
     * verifica os campos para fazer login
     * @param username nome do cliente
     * @param password password do cliente
     * @return cliente se conseguir efetuar login, null se nao conseguir
     */
    public Client login(String username, String password){
        for(Client c:this.listaClientes){
            if(c.getNome().equals(username)){
                if(c.getPassword().equals(password)){
                    return c;
                }
            }
        }
        return null;
    }


    //= Thread para tratar de cada canal de comunicação com um cliente
    class Connection extends Thread {
        DataInputStream in;
        DataOutputStream out;
        Socket clientSocket;
        ArrayList<Client> listaClientes;
        int thread_number;

        public Connection(Socket aClientSocket, int numero) {
            thread_number = numero;
            try {
                clientSocket = aClientSocket;
                in = new DataInputStream(clientSocket.getInputStream());
                out = new DataOutputStream(clientSocket.getOutputStream());

                this.start();
            } catch (IOException e) {
                System.out.println("Connection:" + e.getMessage());
            }
        }

        //=============================
        public void run() {
            try {
                while (true) {
                    String username = in.readUTF();
                    String password = in.readUTF();
                    System.out.println("Cliente recebido:" + username + "|" + password);
                    Client c=login(username, password);
                    if(c!=null){
                        out.writeBoolean(true);
                        menuServidor(c, in, out);
                    }
                    else{
                        out.writeBoolean(false);
                    }
                }
            } catch (EOFException e) {
                System.out.println("EOF:" + e);
            } catch (IOException e) {
                System.out.println("IO:" + e);
            }
        }

    }

    //menu do lado do servidor
    private void menuServidor(Client c, DataInputStream in, DataOutputStream out) throws IOException {
        int opcao;
        do {
            opcao = in.readInt();
            if(opcao==1){
                System.out.println("alterar password");
                alterar_password(c,in,out);
                opcao=0;
            }
            if(opcao ==2){
                System.out.println("Alterar configuracoes");
                altera_config(c,in,out);
                opcao = 0;
            }
            if (opcao ==3){
                System.out.println("Listar ficheiros");
                lista_files(c,in,out);
                opcao = 0;
            }
        } while (opcao != 0);
    }


    //funcao para alterar a password
    private synchronized void alterar_password(Client c, DataInputStream in, DataOutputStream out) {
        try {
            String novaPass=in.readUTF();
            c.setPassword(novaPass);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private synchronized void altera_config(Client c, DataInputStream in, DataOutputStream out) {
        try {
            String novo_ping=in.readUTF();
            String novo_porto = in.readUTF();
            serverPort = Integer.parseInt(novo_porto);
            serverPing = Integer.parseInt(novo_ping);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private synchronized void lista_files(Client c, DataInputStream in, DataOutputStream out) {
        try {//sair daqui qdo der exit
            String lista=in.readUTF();
            System.out.println(lista);
            if (lista.equals("listar")){
                String diretoria = c.getDiretoria_atual();
                out.writeUTF(diretoria);
            }
            String diretoria_atualizada=in.readUTF();
            c.setDiretoria_atual(diretoria_atualizada);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }




}