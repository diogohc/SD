
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Scanner;
import java.util.stream.Stream;


public class TCPServer {
    private int serverPort;
    private String address;
    private static final int timeout = 2000;
    private boolean primario;
    private int bufsize=100;
    private int max_falhas=5;
    private ArrayList<Client> listaClientes = new ArrayList<>();

    public TCPServer(String endereco, int porto) {
        this.address = endereco;
        this.serverPort=porto;
    }

    private void ligacaoComCliente(int porto){
        try (ServerSocket listenSocket = new ServerSocket(porto)) {
            System.out.println("A escuta no porto "+porto);
            System.out.println("LISTEN SOCKET=" + listenSocket);
            while (true) {
                Socket clientSocket = listenSocket.accept(); // BLOQUEANTE
                System.out.println("Novo cliente conectado");
                new Connection(clientSocket);
            }
        } catch (IOException e) {
            System.out.println("Listen:" + e.getMessage());
        }
    }

    private void iniciar(int numero) throws FileNotFoundException {
        //ler ficheiro de configuracao de clientes e adiciona na arraylist
        le_ficheiro();

        if(numero==1){
            FailOverPrim failPrim = new FailOverPrim();
            failPrim.start();
            ligacaoComCliente(serverPort);

        }

        if(numero==2){
            FailOverSec server2 = new FailOverSec();
            server2.run();
            //espera que a thread termine
            try{
                server2.join();
            }catch(InterruptedException e){
                System.out.println("Erro! Thread interrompida");
            }
            this.primario=true;
            FailOverPrim subServer = new FailOverPrim();
            subServer.start();
            ligacaoComCliente(serverPort);
        }

    }


    public static void main(String[] args) throws FileNotFoundException {
        int numero = 0;
        //java TCPServer <1/2> <endereco> <porto>
        TCPServer server = new TCPServer(args[1], Integer.parseInt(args[2]));
        server.iniciar(Integer.parseInt(args[0]));


    }

    public void le_ficheiro() {
        File ficheiro = new File("config_clients.txt");
        try {
            FileReader fr = new FileReader(ficheiro);
            BufferedReader br = new BufferedReader(fr);
            String line;
            String[] campos;
            while ((line = br.readLine()) != null) {
                line = line.replace(" ","");
                line = line.replace("]","");
                campos = line.split(",");
                if (check_directory(campos[7],campos[0])){
                    Client aux = new Client(campos[0], campos[1], campos[2], campos[3], campos[4],
                            campos[5], campos[6], campos[7] + "\\" + campos[0]);
                    this.listaClientes.add(aux);
                }
                else{
                    Client aux = new Client(campos[0], campos[1], campos[2], campos[3], campos[4],
                            campos[5], campos[6], campos[7] );
                    this.listaClientes.add(aux);
                }
            }
            br.close();
        } catch (IOException e) {
            System.out.println("Erro! Ficheiro nao foi encontrado");
            e.printStackTrace();
        }
    }

    private boolean check_directory( String diretoria,String user){
        String [] divide;
        divide = diretoria.split("\\\\");
        System.out.println(divide.length);
        if ( divide.length < 7 || divide[6].equals(user)){
            return false;
        }
        return true;
    }

    /**
     * verifica os campos para fazer login
     * @param username nome do cliente
     * @param password password do cliente
     * @return cliente se conseguir efetuar login, null se nao conseguir
     */
    public Client login(String username, String password){
        for(Client c:this.listaClientes){
            System.out.println(c.getNome());
            if(c.getNome().equals(username)){
                System.out.println(c.getPassword());

                if(c.getPassword().equals(password)){
                    return c;
                }
            }
        }
        return null;
    }

    //failover do lado do server primario
    class FailOverPrim extends Thread{
        public FailOverPrim(){

        }
        public void run() {
            try (DatagramSocket aSocket = new DatagramSocket(serverPort)) {
                while (true) {
                    byte[] buffer = new byte[bufsize];
                    DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
                    aSocket.receive(dp);
                    DatagramPacket reply = new DatagramPacket(dp.getData(), dp.getLength(), dp.getAddress(), dp.getPort());
                    aSocket.send(reply);
                    Thread.sleep(timeout);
                }
            } catch (SocketException e) {
                System.out.println("Socket: " + e.getMessage());
            } catch (IOException e){
                System.out.println("IOException::: " + e.getMessage());
            } catch(InterruptedException e){
                System.out.println("InterruptedException:" + e.getMessage());
            }
        }
    }

    //failover do lado do server secundario
    class FailOverSec extends Thread{
        public FailOverSec(){

        }

        public void run(){
            try(DatagramSocket aSocket = new DatagramSocket()){
                int nr_falhas=0;
                aSocket.setSoTimeout(500);
                while(nr_falhas<max_falhas){
                    try{
                        String texto="ping";
                        byte[] m = texto.getBytes();

                        InetAddress aPrim = InetAddress.getByName("localhost");
                        DatagramPacket dp = new DatagramPacket(m, m.length, aPrim, serverPort);
                        aSocket.send(dp);

                        byte[] buffer = new byte[bufsize];
                        DatagramPacket resposta = new DatagramPacket(buffer, buffer.length);
                        aSocket.receive(resposta);
                        System.out.println("Ping...");
                    }catch(SocketException e){
                        nr_falhas++;
                        System.out.println("Pings falhados: "+nr_falhas);
                    }catch(IOException e){
                        nr_falhas++;
                        System.out.println("Pings falhados: "+nr_falhas);
                    }
                    Thread.sleep(timeout);
                }
            }catch(IOException e){
                System.out.println("IOException:"+e.getMessage());
            }catch(InterruptedException e){
                System.out.println("InterruptedException:"+e.getMessage());
            }
        }


    }

    //= Thread para tratar de cada canal de comunicação com um cliente
    class Connection extends Thread {
        DataInputStream in;
        DataOutputStream out;
        Socket clientSocket;
        ArrayList<Client> listaClientes;
        int thread_number;

        public Connection(Socket aClientSocket) {
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
                        //atualizar o ficheiro com novas diretorias
                        escreve_ficheiro();
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
                System.out.println("bazei");
                opcao = 0;
            }

            if (opcao ==4){
                System.out.println("Alterar diretoria");
                altera_diretoria(c,in,out);
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
            String novo_endereco=in.readUTF();
            String novo_porto = in.readUTF();
            this.serverPort = Integer.parseInt(novo_porto);
            this.address = novo_endereco;


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void lista_files(Client c, DataInputStream in, DataOutputStream out) {
        try {//sair daqui qdo der exit
            String lista=in.readUTF();
            if (lista.equals("listar")){
                String diretoria = c.getDiretoria_atual();
                printFiles(diretoria,out);
                //out.writeUTF(diretoria);
            }


        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private synchronized void altera_diretoria(Client c, DataInputStream in, DataOutputStream out) throws IOException {
        String atual = null;
        String novaDiretoria = null;
        boolean primeira = true;
        if (in.readUTF().equals("altera")){
            atual = c.getDiretoria_atual();
            out.writeUTF(atual);
        }
        while (in.readUTF().equals("siga")) {
            System.out.println("entreei no sever");
            if (primeira == true){
                novaDiretoria= atual;
                primeira = false;
            }
            else {
                novaDiretoria = in.readUTF();
            }
            printFiles(novaDiretoria, out);

            c.setDiretoria_atual(novaDiretoria);
            atualiza_file(novaDiretoria,c);
            System.out.println("+++++++++"+c.getDiretoria_atual());
        }
        atualiza_file(novaDiretoria,c);
        System.out.println("bazei");

    }




        private  void atualiza_file(String atualizacao,Client c) throws IOException {
        StringBuilder nova_linha = new StringBuilder("");

        try {
            File newFile = new File("config_clients.txt");

            Scanner raf = new Scanner(newFile);
            while (raf.hasNextLine()) {
                String linha = raf.nextLine();
                String[] split_linha = linha.split(",");
                if (split_linha[0].equals(c.getNome())) {
                    split_linha[7] = atualizacao;//so atualiza diretoria do user
                    nova_linha.append(Arrays.toString(split_linha)+"\n");
                }
                else{
                    nova_linha.append(Arrays.toString(split_linha).replace("[","")+"\n");
                }
            }
            raf.close();
        }catch (IOException e) {
            System.out.println("Erro! Ficheiro nao foi encontrado");
            e.printStackTrace();
        }


        PrintStream escreveFile = new PrintStream( new FileOutputStream("config_clients.txt"));
        escreveFile.print(nova_linha);
        escreveFile.close();



    }



    public static void printFiles(String path,DataOutputStream out) throws IOException {
        System.out.println(path);
        Scanner sc = new Scanner(System.in);
        File dir = new File(path);
        File[] files = dir.listFiles();
        System.out.println(files.length);
        if (files.length>0)
            out.writeUTF(String.valueOf(files.length));
        while (true) {
            if (files != null && files.length > 0) {
                for (int i = 0; i < files.length; i++) {
                    System.out.println(i);
                    if (files[i].isDirectory()) {
                        out.writeUTF(String.valueOf(i) + "." + "Directory: " + files[i].getName());
                        //System.out.println(i + "." + "Directory: " + files[i].getName());
                        //recursividade para sacar ficheiros dentro de pastas
                        //printFiles(files[i].getAbsolutePath());
                    } else {
                        out.writeUTF(String.valueOf(i) +  ". " + files[i].getName());
                        //System.out.println(i +  ". " + files[i].getName());
                    }
                }
                break;
            }
        }


    }

    
    private void escreve_ficheiro(){
        File fich = new File("config_clients.txt");
        try{
            FileWriter fw = new FileWriter(fich);
            BufferedWriter bw = new BufferedWriter(fw);

            for(Client c:listaClientes){
                bw.append(c.clienteFicheiro());
            }
            bw.close();
        }catch(IOException ex){
            System.out.println("Erro a escrever no ficheiro");
        }
    }



}