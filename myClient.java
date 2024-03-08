import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

public class myClient {

    private Socket clientSocket;
    private ObjectInputStream inStream;
    private ObjectOutputStream outStream;

    public static void main(String[] args) throws IOException {
        try (Scanner scanner = new Scanner(System.in)) {

            System.out.println("Enter your username:");
            // String username = scanner.nextLine();
            String username = args[1];
            while ((username).equals("")) {
                System.out.println("Enter your username (must have chars):");
                username = scanner.nextLine();
            }
            String dev_id = args[2];

            System.out.println("Enter your password:");
            String password = scanner.nextLine();
            while ((password).equals("")) {
                System.out.println("Enter your username (must have chars):");
                password = scanner.nextLine();
            }

            System.out.println("cliente: " + username);

            String serverAddress = args[0];
            String[] ipPort = serverAddress.split(":");
            myClient cliente = new myClient(ipPort[0], Integer.parseInt(ipPort[1]));

            cliente.sendInfo(username, password);
            cliente.close();
        } catch(IOException e) {
            
        } catch(NullPointerException e) {
            System.err.println("Usar o comando: java IoTDevice <serverAddress> <dev-id> <user-id>\n <serverAddress> = <IP:port>");
            System.exit(-1);
        }
    }

    // Cria um novo cliente e a sua ligacao ao servidor
    public myClient(String hostName, int portNumber) throws IOException {
        this.clientSocket = new Socket(hostName, portNumber);
        this.inStream = new ObjectInputStream(clientSocket.getInputStream());
        this.outStream = new ObjectOutputStream(clientSocket.getOutputStream());
    }

    /**
     * Envia credenciais ao servidor
     * 
     * @param user     credenciais
     * @param password credenciais
     * @throws IOException
     */
    public void sendInfo(String user, String password) throws IOException {
        outStream.writeObject(user);
        outStream.writeObject(password);

        try {
            String result = (String) inStream.readObject();

            // Enviar File para Server
            if (result.equals("GET_FILE")) {

                try (Scanner scanner = new Scanner(System.in)) {
                    System.out.println("Enter your file name:");
                    String fileName = scanner.nextLine();

                   
                }

                // Receber File do Server
            } else if (result.equals("POST_FILE")) {
                try {
                    long size = inStream.readLong();
                    byte[] array = new byte[(int) size];
                    inStream.readFully(array);
                    System.out.println(new String(array));
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else if (result.equals("ERROR")) {
                System.out.println("Ejecting cause of error, (skill issue)");
            }

        } catch (ClassNotFoundException e1) {
            e1.printStackTrace();
        }
    }

    /**
     * Termina a ligacao do cliente ao servidor
     * 
     * @throws IOException
     */
    public void close() throws IOException {
        outStream.writeObject("Close_Conection");
        outStream.close();
        Stream.close();
        clientSocket.close();
    }

    public String interfaceIO(){
        String s = "--------------------------------------------------------------------\n";
        s.concat("Comandos: \n");
        s.concat("\n");
        s.concat("CREATE <dominio> - Cria um novo dominio com o nome escolhido \n");
        s.concat("ADD <username> <dominio> - Adiciona o utilizador escolhido ao dominio escolhido \n");
        s.concat("RD <dominio> - Regista o dispositivo atual ao dominio escolhido \n");
        s.concat("ET <float> - Envia o valor float da temperatura \n");
        s.concat("EI <filename.jpg> - Abre o ficheiro do cliente com o nome escolhido \n");
        s.concat("RT <dominio> - Obtem os dados da temperatura guardados no servidor de todos os dispositivos dentro do dominio \n");
        s.concat("RI <user_id>:<dev_id> - Permite obter a imagem do dispositivo escolhido\n");
        s.concat("\n");
        s.concat("--------------------------------------------------------------------\n");
        return s;
    }

    // public void sendTxtFiles(String fileName){
    //     String filePasth = ("ClientFiles/" + fileName + ".txt");
    //     File f = new File(filePasth);

    //     // send size
    //     try{
    //         System.out.println(f.length());
    //         outStream.writeLong(f.length());
    //         System.out.println("depois de " + f.length());
    //         byte[] buffer = Files.readAllBytes(f.toPath());
    //         outStream.write(buffer); 
    //     }catch(IOException e){
    //         System.out.println("txt send IO error");
    //     }
        
    // }

    // public void imgFiles(String fileName) {
    //     String filePasth = ("ClientFiles/" + fileName + ".jpg");
    //     File f = new File(filePasth);

    //     // send size
    //     try {
    //         System.out.println(f.length());
    //         outStream.writeLong(f.length());
    //         System.out.println("depois de " + f.length());
    //         byte[] buffer = Files.readAllBytes(f.toPath());
    //         outStream.write(buffer);
    //     } catch (IOException e) {
    //         System.out.println("txt send IO error");
    //     }

    // }
}
