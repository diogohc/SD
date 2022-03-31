
import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;


public class TCPServer {
    ServerSocket listenSocket;
    private int serverPort;
    private static String address;
    private static boolean primario;
    private static final int timeout = 2000;
    private static final int BUF_SIZE=1024;
    private int bufsize=100;
    private int max_falhas=5;
    private static int max_clientes=10;
    public final static int FILE_SIZE = 6022386;
    private ArrayList<Client> listaClientes = new ArrayList<>();

    public TCPServer(String endereco, int porto) {
        this.address = endereco;
        this.serverPort=porto;
    }

    private void ligacaoComCliente(int porto, int max_clientes, String endereco){
        try  {
            listenSocket =  new ServerSocket(porto, max_clientes, InetAddress.getByName(endereco));
            System.out.println("A escuta no porto "+porto);
            System.out.println("LISTEN SOCKET=" + listenSocket);
                //le_ficheiro();
            while (true) {
                Socket clientSocket = listenSocket.accept(); // BLOQUEANTE
                System.out.println("Novo cliente conectado");
                new Connection(clientSocket, listenSocket);
            }
        } catch (IOException e) {
            System.out.println("Listen:" + e.getMessage());
        }
    }

    private void iniciar(int numero) throws FileNotFoundException {
        //ler ficheiro de configuracao de clientes e adiciona na arraylist
        System.out.println(primario+"oi9oioioioio");


        if(numero==1){
            this.primario=true;
            le_ficheiro();
            FailOverPrim failPrim = new FailOverPrim();
            failPrim.start();
            //ligacaoComCliente(serverPort);
            ligacaoComCliente(serverPort, max_clientes, address);

        }

        if(numero==2){
            this.primario=false;
            le_ficheiro();
            System.out.println(primario);
            FailOverSec server2 = new FailOverSec();
            server2.run();
            //espera que a thread termine
            try{
                server2.join();
            }catch(InterruptedException e){
                System.out.println("Erro! Thread interrompida");
            }

            FailOverPrim subServer = new FailOverPrim();
            subServer.start();
            //ligacaoComCliente(serverPort);
            ligacaoComCliente(serverPort, max_clientes, address);
        }

    }


    public static void main(String[] args) throws FileNotFoundException {
        int numero = 0;
        //java TCPServer <1/2> <endereco> <porto>
        TCPServer server = new TCPServer(args[1], Integer.parseInt(args[2]));
        server.iniciar(Integer.parseInt(args[0]));


    }

    public void le_ficheiro() {
        System.out.println(primario+"aquiiiiiiiiiii");
        Client aux;
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
                    if (primario){
                         aux = new Client(campos[0], campos[1], campos[2], campos[3], campos[4],
                                campos[5], campos[6], campos[7] + "\\" + campos[0]);

                    }
                    else {
                        System.out.println("li pra diretoria sec");
                         aux = new Client(campos[0], campos[1], campos[2], campos[3], campos[4],
                                campos[5], campos[6], campos[8] + "\\" + campos[0]);
                    }
                    this.listaClientes.add(aux);
                }
                else{
                    if (primario){
                        aux = new Client(campos[0], campos[1], campos[2], campos[3], campos[4],
                                campos[5], campos[6], campos[7]);

                    }
                    else {
                        System.out.println("li pra diretoria sec");
                        aux = new Client(campos[0], campos[1], campos[2], campos[3], campos[4],
                                campos[5], campos[6], campos[8] );
                    }
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
        ServerSocket servSocket;
        ArrayList<Client> listaClientes;
        int thread_number;

        public Connection(Socket aClientSocket, ServerSocket ss) {
            try {
                clientSocket = aClientSocket;
                servSocket=ss;
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
                        menuServidor(servSocket, c, in, out);
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
    private void menuServidor(ServerSocket servSocket, Client c, DataInputStream in, DataOutputStream out) throws IOException {
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
                altera_config(servSocket, c,in,out);
                servSocket.close();
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

            if (opcao == 5){
                download(in,c);
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

    private synchronized void altera_config(ServerSocket s, Client c, DataInputStream in, DataOutputStream out) {
        int server=0;
        String novo_endereco;
        String novo_porto;
        try{
            server = in.readInt();
        }catch(IOException e){
            e.printStackTrace();
        }

        if(server==1) {
            try {
                novo_endereco = in.readUTF();
                novo_porto = in.readUTF();
                this.serverPort = Integer.parseInt(novo_porto);
                this.address = novo_endereco;
                System.out.println("Novo endereco: "+this.address+"\t"+"Novo porto:"+this.serverPort);
                s.close();
                ligacaoComCliente(this.serverPort, max_clientes, this.address);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private synchronized void lista_files(Client c, DataInputStream in, DataOutputStream out) {
        try {//sair daqui qdo der exit
            String lista=in.readUTF();
            if (lista.equals("listar")){
                String diretoria = c.getDiretoria_atual();
                printFiles(diretoria,out);
                //out.writeUTF(diretoria);
                int valor = Integer.valueOf(in.readUTF());
                if (valor!=-1){
                    //selecionar o ficheiro
                    File dir = new File(diretoria);
                    File[] files = dir.listFiles();
                    System.out.println(files.length);
                    while (true) {
                        if (files != null && files.length > 0) {
                            for (int i = 0; i < files.length; i++) {
                                System.out.println(i);
                                if (files[i].isDirectory()) {
                                    //out.writeUTF(String.valueOf(i) + "." + "Directory: " + files[i].getName());
                                    if (i==valor){
                                        System.out.println(i + "." + "Directory: " + files[i].getName()+"nao pode fazer download de diretorias");
                                        break;
                                    }

                                    //recursividade para sacar ficheiros dentro de pastas
                                    //printFiles(files[i].getAbsolutePath());
                                } else {
                                    if (i==valor){
                                        out.writeUTF(files[i].getName());
                                        upload(diretoria +"\\"+files[i].getName());
                                        break;
                                    }
                                    //System.out.println(i +  ". " + files[i].getName());
                                }
                            }
                            break;
                        }
                    }
                }
                else{
                    return;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private synchronized void upload(String path) throws IOException {
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        OutputStream os = null;
        ServerSocket servsock = null;
        Socket sock = null;
        try {
            while (true) {
                System.out.println("Waiting...");
                try ( ServerSocket s = new ServerSocket(20000);){
                    sock = s.accept();
                    System.out.println("Accepted connection : " + sock);
                    // send file
                    File myFile = new File (path);
                    byte [] mybytearray  = new byte [(int)myFile.length()];
                    fis = new FileInputStream(myFile);
                    bis = new BufferedInputStream(fis);
                    bis.read(mybytearray,0,mybytearray.length);
                    os = sock.getOutputStream();
                    System.out.println("Sending " + path + "(" + mybytearray.length + " bytes)");
                    os.write(mybytearray,0,mybytearray.length);
                    os.flush();
                    System.out.println("Done.");
                }
                finally {
                    if (bis != null) bis.close();
                    if (os != null) os.close();
                    if (sock!=null) sock.close();
                }
                break;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (servsock != null) servsock.close();
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
                    if (primario)
                        split_linha[7] = atualizacao;//so atualiza diretoria do user
                    else
                        split_linha[8]=atualizacao;
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



    private static synchronized void download(DataInputStream in,Client c) throws IOException {
        String file_criar= in.readUTF();
        int bytesRead;
        int current = 0;
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        Socket sock = null;
        try {
            sock = new Socket(address, 10000);
            System.out.println("Connecting...");
            // receive file
            byte [] mybytearray  = new byte [FILE_SIZE];
            InputStream is = sock.getInputStream();
            fos = new FileOutputStream(c.getDiretoria_atual()+ "\\" + file_criar);
            bos = new BufferedOutputStream(fos);
            bytesRead = is.read(mybytearray,0,mybytearray.length);
            current = bytesRead;

            do {
                bytesRead =
                        is.read(mybytearray, current, (mybytearray.length-current));
                if(bytesRead >= 0) current += bytesRead;
            } while(bytesRead > -1);

            bos.write(mybytearray, 0 , current);
            bos.flush();
            System.out.println("File " + c.getDiretoria_atual()+ "\\" + file_criar
                    + " downloaded (" + current + " bytes read)");
            //de user para servidor
            if(primario){
                System.out.println("enviei backup");
                envia_para_backup(c.getDiretoria_atual()+ "\\" + file_criar);//passar path completo do ficheiro a enviar
            }
            /*else{
                System.out.println("entrei aqui");
                recebe_backup("C:\\Users\\joaog\\OneDrive\\Documentos\\diretoriaserversec\\user1"+file_criar);//passar path completo do sitio onde guardar o ficheiro (incluindo o nome do ficheiro no fim do path)
            }*/
            //replicar
            //escrevo na diretoria
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) fos.close();
            if (bos != null) bos.close();
            if (sock!=null) sock.close();
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

    public static void recebe_backup(String path){
        try (DatagramSocket socket = new DatagramSocket(6000)) {
            FileOutputStream fos = new FileOutputStream(path);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int tam_fich;
            byte[] buffer = new byte[BUF_SIZE];
            DatagramPacket request = new DatagramPacket(buffer, buffer.length);
            socket.receive(request);
            String s = new String(request.getData(), 0, request.getLength());
            tam_fich=Integer.parseInt(s);
            System.out.println("Tamanho:"+tam_fich);
            int nr_iteracoes = tam_fich/BUF_SIZE;
            int i=0;
            while(i<=nr_iteracoes){
                request = new DatagramPacket(buffer, buffer.length);
                socket.receive(request);
                fos.write(buffer,0,buffer.length);
                fos.flush();
                i++;
            }
            fos.close();
        } catch (SocketException e) {
            System.out.println("Socket: " + e.getMessage());
        }catch(IOException e){
            System.out.println("IOException: "+e.getMessage());
        }
    }

    public static void envia_para_backup(String path){
        try (DatagramSocket ds = new DatagramSocket()) {
            File fich = new File(path);
            FileInputStream fis;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            byte [] data;
            byte [] buffer = new byte[BUF_SIZE];

            fis = new FileInputStream(fich);
            System.out.println("Tam fich:"+fis.available());
            dos.writeLong(fis.available());
            String s= String.valueOf((int)fis.available());
            data = s.getBytes();
            DatagramPacket dp = new DatagramPacket(data, data.length, InetAddress.getByName("localhost"), 6000);
            ds.send(dp);

            int nread;
            do{
                nread = fis.read(buffer);
                if(nread>0){
                    DatagramPacket clpkt=new DatagramPacket(buffer,nread,InetAddress.getByName("localhost"),6000);
                    ds.send(clpkt);
                }
            }while(nread!= -1);
            dos.close();
            fis.close();

        }catch (SocketException e){
            System.out.println("Socket: " + e.getMessage());
        }catch(FileNotFoundException e){
            System.out.println("FileNotFoundException: "+e.getMessage());
        }catch(IOException e){
            System.out.println("IOException: "+e.getMessage());
        }
    }



}