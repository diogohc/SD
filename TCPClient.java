
import java.net.*;
import java.util.Date;
import java.util.Scanner;
import java.io.*;

public class TCPClient extends Client{
    private static int serversocket = 6000;

    String path = "C:\\Users\\joaog\\OneDrive\\Ambiente de Trabalho\\Cadeiras de licenciatura\\SD\\diretorias";

    public static void main(String[] args) {
        // args[0] <- hostname of destination
        if (args.length == 0) {
            System.out.println("java TCPClient hostname");
            System.exit(0);
        }

        // 1o passo - criar socket
        try (Socket s = new Socket(args[0], serversocket)) {
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

    //menu do lado do cliente
    private static void menu(DataInputStream in, DataOutputStream out) throws IOException {
        Scanner sc = new Scanner(System.in);
        int opcao;
        do {
            System.out.println("===Menu===");
            System.out.println("1 - Alterar password");
            System.out.println("2 - Configurar servidor");
            System.out.println("0 - Sair");
            System.out.print("Opcao:");
            opcao = sc.nextInt();
            out.writeInt(opcao);
            if(opcao==1) {
                alterar_password(in, out);
                opcao=0;
            }
            if (opcao==2){
                configura_fail(in,out);
                opcao=0;
            }
            if (opcao==3){
                listar_files(in,out);
                opcao=0;
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

    private static void configura_fail(DataInputStream in,DataOutputStream out) throws IOException{
        Scanner sc = new Scanner(System.in);
        System.out.print("ping desejado:");
        String novo_ping = sc.nextLine();
        System.out.print("porto desejado:");
        String novo_porto = sc.nextLine();
        System.out.println(novo_ping);
        System.out.println((novo_porto));
        out.writeUTF(novo_ping);
        out.writeUTF(novo_porto);
    }

    private static void listar_files(DataInputStream in,DataOutputStream out) throws IOException {
        out.writeUTF("listar");
        String diretoria_atual= in.readUTF();//diretoria atual do servidor
        System.out.println(diretoria_atual);
        diretoria_atual = diretoria_atual.replace(" ","");
        System.out.println(diretoria_atual.length());
        String ola = "C:\\Users\\joaog\\OneDrive\\Documentos\\diretorias\\user1";
        System.out.println(ola.length());
        printFiles(diretoria_atual,out);

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
                    }
                }
            }
            System.out.println("Introduce a directory number OR ENTER.\n-1.Return to previous directory.\n-2.Exit");
            ctl = sc.nextInt();
            System.out.println(path);
            if (ctl == -2) {

                out.writeUTF(path);
                break;
            }

            else if(ctl == -1){
                path = path.replace("\\"," ");
                System.out.println(path);
                String retPath = "";

                String[] toks;
                toks = path.split(" ");

                for(int j = 0; j < toks.length - 1; j++){
                    System.out.println(toks[j]);

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
                System.out.println(path);
            }

            else{
                path += "\\" + files[ctl].getName() + "\\";
                System.out.println(path);
                dir = new File(path);
                files = dir.listFiles();
                //System.out.println(path);
            }
        }
    }







}