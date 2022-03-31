
import java.net.*;
import java.sql.SQLSyntaxErrorException;
import java.util.Date;
import java.util.Scanner;
import java.io.*;

public class TCPClient extends Client{
    private static int serverporto;
    private static String serverIP;
    public final static int FILE_SIZE = 6022386;


    private static String pathCliente = "C:\\Users\\joaog\\OneDrive\\Documentos\\diretoriaspc";

    public static void main(String[] args) {
        // args[0] <- hostname of destination
        if (args.length != 2) {
            System.out.println("java TCPClient hostname port");
            System.exit(0);
        }
        serverporto = Integer.parseInt(args[1]);
        serverIP = args[0];

        // 1o passo - criar socket
        try (Socket s = new Socket(args[0], serverporto)) {
            System.out.println("SOCKET=" + s);
            // 2o passo
            DataInputStream in = new DataInputStream(s.getInputStream());
            DataOutputStream out = new DataOutputStream(s.getOutputStream());
            // 3o passo
            try (Scanner sc = new Scanner(System.in)) {
                while (true) {
                    // READ STRING FROM KEYBOARD
                    System.out.println("===Login===");
                    System.out.print("Username:");
                    String username = sc.nextLine();
                    //String username=keyboard.readLine();
                    System.out.print("Password:");
                    String password = sc.nextLine();
                    out.writeUTF(username);
                    out.writeUTF(password);

                    // READ FROM SOCKET
                    //verifica se o login foi ou nao efetuado com sucesso
                    boolean verifica_login=in.readBoolean();
                    if(verifica_login) {
                        System.out.println("\nLogin efetuado com sucesso\n");
                        if(check_directory(pathCliente,username))
                            pathCliente+=("\\" + username);
                        System.out.println(pathCliente);
                        menu(in, out);

                    }
                    else {
                        System.out.println("\nUsername/password errados\n");
                        break;
                    }
                }
            }

        } catch (UnknownHostException e) {
            System.out.println("Sock:" + e.getMessage());
        } catch (EOFException e) {
            System.out.println("EOF:" + e.getMessage());
        } catch (IOException e) {
            System.out.println("IO:" + e.getMessage());
        }
    }

    private static boolean check_directory(String diretoria,String user){
        String [] divide;
        divide = diretoria.split("\\\\");
        System.out.println(divide.length);
        if (divide.length >6){
            return false;
        }
        return true;
    }


    //menu do lado do cliente
    private static void menu(DataInputStream in, DataOutputStream out) throws IOException {
        Scanner sc = new Scanner(System.in);
        int opcao;
        do {
            System.out.println("===Menu===");
            System.out.println("1 - Alterar password");
            System.out.println("2 - Configurar servidor");
            System.out.println("3 - Listar ficheiros servidor");
            System.out.println("4 - Atualizar diretoria servidor");
            System.out.println("5 - Listar ficheiros ");
            System.out.println("0 - Sair");
            System.out.print("Opcao:");
            opcao = sc.nextInt();
            out.writeInt(opcao);
            if(opcao==1) {
                alterar_password(in, out);
                opcao=0;
            }
            if (opcao==2){
                configura_servers(in,out);
                System.exit(0);
                opcao=0;
            }
            if (opcao==3){
                listar_files_servidor(in,out);
                opcao=0;
            }
            if (opcao==4){
                atualizar_diretoria_servidor(in,out);
                opcao=0;
            }
            if (opcao==5){
                printFiles(pathCliente,out);
            }


        } while (opcao != 0);
    }

    //funcao para alterar a password
    private static void alterar_password(DataInputStream in, DataOutputStream out) throws IOException {
        Scanner sc = new Scanner(System.in);
        System.out.print("Nova password:");
        String novaPass = sc.nextLine();
        out.writeUTF(novaPass);
    }

    private static void configura_servers(DataInputStream in,DataOutputStream out) throws IOException{
        int nr_server=0;
        Scanner sc = new Scanner(System.in);
        System.out.print("Escolher server (1 ou 2):");
        nr_server = sc.nextInt();
        while(nr_server!=1 && nr_server!=2){
            System.out.println("Erro! Escolher entre 1 e 2");
            System.out.print("Escolher server (1 ou 2):");
            nr_server = sc.nextInt();
        }
        out.writeInt(nr_server);
        sc.nextLine();
        System.out.print("Novo endereco:");
        String novo_ping = sc.nextLine();
        System.out.print("Novo porto:");
        String novo_porto = sc.nextLine();
        System.out.println(novo_ping);
        System.out.println((novo_porto));
        out.writeUTF(novo_ping);
        out.writeUTF(novo_porto);
    }

    private static void listar_files_servidor(DataInputStream in,DataOutputStream out) throws IOException {
        Scanner sc = new Scanner(System.in);
        int ctl;
        out.writeUTF("listar");
        int size = Integer.parseInt(in.readUTF());
        for (int i =0 ; i < size ;i++) {
            System.out.println(in.readUTF());
        }

        //download
        System.out.println("deseja fazer download de algum dos ficheiros?selecione:/-1 exit:");
        ctl = sc.nextInt();
        out.writeUTF(String.valueOf(ctl));
        if (ctl!=-1) {
            String file_criar;
            file_criar = in.readUTF();
            download(file_criar);
        }








       /*String diretoria_atual= in.readUTF();//diretoria atual do servidor
        System.out.println(diretoria_atual);
        diretoria_atual = diretoria_atual.replace(" ","");
        printFiles(diretoria_atual,out);
*/
    }


    private static synchronized void download(String file_criar) throws IOException {
        int bytesRead;
        int current = 0;
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        Socket sock = null;
        try {
            sock = new Socket(serverIP, 20000);
            System.out.println("Connecting...");
            // receive file
            byte [] mybytearray  = new byte [FILE_SIZE];
            InputStream is = sock.getInputStream();
            fos = new FileOutputStream(pathCliente+ "\\" + file_criar);
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
            System.out.println("File " + pathCliente
                    + " downloaded (" + current + " bytes read)");
            //de user para servidor
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






    private static void atualizar_diretoria_servidor(DataInputStream in,DataOutputStream out) throws IOException {
        out.writeUTF("altera");
        Scanner sc = new Scanner(System.in);
        String path = in.readUTF();
        File dir = new File(path);
        File[] files = dir.listFiles();
        int ctl;
        out.writeUTF("siga");
        while (true) {
            int size = Integer.parseInt(in.readUTF());
            for (int i = 0 ; i<size;i++) {
                System.out.println(in.readUTF());
            }
            System.out.println("Introduce a directory number OR ENTER.\n-1.Return to previous directory.\n-2.Exit");
            ctl = sc.nextInt();
            System.out.println(path);
            if (ctl == -2) {

                out.writeUTF("sair");
                break;
            }

            else if(ctl == -1){
                path = path.replace("\\"," ");
                System.out.println(path);
                String retPath = "";

                String[] toks;
                toks = path.split(" ");

                for(int j = 0; j < toks.length - 1; j++){
                    //System.out.println(toks[j]);

                    if (j==toks.length-2){
                        retPath += toks[j];
                    }
                    else{
                        retPath += toks[j] + "\\";
                    }
                }

                System.out.println("path retificado" + retPath);
                path = retPath;
                dir = new File(path);
                files = dir.listFiles();
                out.writeUTF("siga");
                out.writeUTF(path);
                System.out.println("enviei");

            }

            else{
                assert files != null;
                path += "\\" + files[ctl].getName() ;
                System.out.println("helllllo"+path);
                dir = new File(path);
                files = dir.listFiles();
                out.writeUTF("siga");
                out.writeUTF(path);
                //System.out.println(path);
            }
        }
        }






    public static void printFiles(String path,DataOutputStream out) throws IOException {
        Scanner sc = new Scanner(System.in);
        File dir = new File(path);
        File[] files = dir.listFiles();
        int ctl;
        while (true) {
            if (files != null && files.length > 0) {
                for (int i = 0; i < files.length; i++) {
                    if (files[i].isDirectory()) {
                        System.out.println(i + "." + "Directory: " + files[i].getName());
                        //recursividade para sacar ficheiros dentro de pastas
                        //printFiles(files[i].getAbsolutePath());
                    } else {
                        System.out.println(i +  ". " + files[i].getName());
                        System.out.println("deseja fazer upload do ficheiro?selecione:/-1 exit:");
                        ctl = sc.nextInt();
                        if (ctl!=-1){
                            out.writeUTF(files[i].getName());
                            upload(path + "\\" +files[i].getName());
                        }
                    }
                }
            }
            System.out.println("Introduce a directory number OR ENTER.\n-1.Return to previous directory.\n-2.Exit");
            ctl = sc.nextInt();
            System.out.println(path);
            if (ctl == -2) {
                pathCliente = path;
                break;
            }

            else if(ctl == -1){
                path = path.replace("\\"," ");
                //System.out.println(path);
                String retPath = "";

                String[] toks;
                toks = path.split(" ");

                for(int j = 0; j < toks.length - 1; j++){
                    //System.out.println(toks[j]);

                    if (j==toks.length-2){
                        retPath += toks[j];
                    }
                    else{
                        retPath += toks[j] + "\\";
                    }
                }

                //System.out.println("path retificado" + retPath);
                path = retPath;
                dir = new File(path);
                files = dir.listFiles();
            }

            else{
                path += "\\" + files[ctl].getName() ;
                //System.out.println(path);
                dir = new File(path);
                files = dir.listFiles();
                //System.out.println(path);
            }
        }
    }


    private static synchronized void upload(String path) throws IOException {
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        OutputStream os = null;
        ServerSocket servsock = null;
        Socket sock = null;
        try {
            while (true) {
                System.out.println("Waiting...");
                try ( ServerSocket s = new ServerSocket(10000);){
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}






