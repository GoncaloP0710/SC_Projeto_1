package client;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

/**
 * 
 * @author André Reis fc58192
 * @author Gonçalo Pinto fc58178
 * @author Jose Bras fc55449
 */
public class IoTDevice {

    static boolean running;
    static Socket clientSocket;
    static ObjectInputStream inStream;
    static ObjectOutputStream outStream;
    static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        try{
            running = true;

            if(args.length != 3)
                throw new ArrayIndexOutOfBoundsException("IoTDevice <serverAddress> <dev-id> <user-id>");
            String serverAddress = args[0];
            String dev_id_s = args[1];
            String user_id = args[2];
            String[] serverAd = new String[2];
            int dev_id = Integer.parseInt(dev_id_s);
            if (dev_id <= 0) {
                throw new NumberFormatException("Device Id can't be used");
            }
            setup();
            if(serverAddress.contains(":")){
                serverAd = serverAddress.split(":");
            } else{
                serverAd[0] = serverAddress;
                serverAd[1] = "12345";
            }
            String ip = serverAd[0];
            int port = Integer.parseInt(serverAd[1]);
            
            //-------------------------------------
            clientSocket = new Socket(ip, port);
            inStream = new ObjectInputStream(clientSocket.getInputStream());
            outStream = new ObjectOutputStream(clientSocket.getOutputStream());
            //-------------------------------------------------------------


            //1
            String senha = pedeSenha();

            String response_info = sendInfo(user_id, senha, inStream, outStream);
            System.out.println(response_info);
            //2
            while(response_info.equals("WRONG-PWD") ){
                senha = pedeSenha();
                response_info = sendInfo(user_id, senha, inStream, outStream);
                System.out.println(response_info);

            }

            //3
            String response_dev = send_device_id(dev_id, inStream, outStream);
            System.out.println(response_dev);
            
            while(response_dev.equals("NOK-DEVID") ){
                System.out.println("Esse device esta a ser utilizado por outro cliente \n");
                System.out.println("Escolha outro device: \n");
                dev_id = Integer.parseInt(getAnswer());
                response_dev = send_device_id(dev_id, inStream, outStream);
                System.out.println(response_dev);

            }
            //--------------------------------------------------------------

            //5
            String fileName = "./IoTDevice.jar";
            File jar = new File(fileName);
            long tamanhoL = jar.length();
            String tamanhoS = Long.toString(tamanhoL);

            String response_file = send_file_info(tamanhoS, fileName, inStream, outStream);

            //6
            if (response_file.equals("NOK-TESTED") ) {
                clientSocket.close();
                sc.close();
                inStream.close();
                outStream.close();
                System.out.println("NOK-TESTED \n Cliente nao validado pelo servidor");
                System.exit(0);
            }
            System.out.println("OK-TESTED");
            //7 Imprimir menu de comandos
            interfaceIO();

            //8 e 9
            while(running){
                String[] comands = getComand();
                if(comands != null){
                    // enviar o pedido ao server
                    if(comands[0].equals("EI")){
                        sendImg(outStream, comands[1], comands[0]);
                    } else{
                        for(int i = 0; i< comands.length; i++){
                            outStream.writeObject(comands[i]);
                        }
                        if (comands[0].equals("RT")) {
                            answerMidleware(comands[0], comands[1]);
                        } else {
                            answerMidleware(comands[0]);
                        }
                    }
                    
                } else {
                    Thread.sleep(200);
                    System.out.println("invalid command! \n");
                }
                
            }
            
            //------------------------------------------------------

            sc.close();
        } catch(IOException | InterruptedException ie){
            System.out.println("IO error");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            System.out.println("Device Id can't be used");
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("IoTDevice <serverAddress> <dev-id> <user-id>");
        }
    }

    private static String send_file_info(String tamanhoS, String fileName, ObjectInputStream in,
            ObjectOutputStream out) throws IOException {
        out.writeObject(fileName);
        out.writeObject(tamanhoS);
        return recebe(in);
    }

    private static void setup() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    Thread.sleep(0);
                    
                    running = false;
                    System.out.println("Shutting down...");

                    clientSocket.close();


                    //some cleaning up code...
    
                } catch (InterruptedException | IOException e) {
                    Thread.currentThread().interrupt();
                    e.printStackTrace();
                }  catch (NullPointerException e) {
                    System.out.println("Incorrect port");
                }
            }
        });
    }

    private static String[] getComand(){
        String comando = getAnswer();
        return verifyCommand(comando);
    }

    private static String pedeSenha(){
        System.out.println("Indique a sua senha:");
        return getAnswer();
    }

    private static String getAnswer() {
        String s = sc.hasNextLine()?sc.nextLine():"";
        return s;
    }

    private static String sendInfo(String user, String password, ObjectInputStream in, ObjectOutputStream out) throws IOException {
        out.writeObject(user);
        out.writeObject(password);
        return recebe(in);
    }

    private static String send_device_id(int dev_id, ObjectInputStream in, ObjectOutputStream out) throws IOException {
        System.out.println("Dev Id a mandar: " + dev_id + "\n");
        out.writeObject(dev_id);
        return recebe(in);
    }

    private static String recebe(ObjectInputStream in) throws IOException{
        try{
            String result = (String) in.readObject();
            return result;
        } catch (ClassNotFoundException e1) {
            e1.printStackTrace();
        }
        return null;
    }
    
    public static void interfaceIO() {
        System.out.println("--------------------------------------------------------------------\n");
        System.out.println("Comandos: \n");
        System.out.println("\n");
        System.out.println("CREATE <dominio> - Cria um novo dominio com o nome escolhido \n");
        System.out.println("ADD <username> <dominio> - Adiciona o utilizador escolhido ao dominio escolhido \n");
        System.out.println("RD <dominio> - Regista o dispositivo atual ao dominio escolhido \n");
        System.out.println("ET <float> - Envia o valor float da temperatura \n");
        System.out.println("EI <filename.jpg> - Abre o ficheiro do cliente com o nome escolhido \n");
        System.out.println("RT <dominio> - Obtem os dados da temperatura guardados no servidor de todos os dispositivos dentro do dominio \n");
        System.out.println("RI <user_id>:<dev_id> - Permite obter a imagem do dispositivo escolhido\n");
        System.out.println("\n");
        System.out.println("--------------------------------------------------------------------\n");
    }

    private static String[] verifyCommand(String comando){
        String[] comands = comando.split(" ");
        switch (comands[0]) {
            case "CREATE":
                return (comands.length != 2) ? null : comands;
            case "ADD":
                return (comands.length != 3) ? null : comands;
            case "RD":
                return (comands.length != 2) ? null : comands;
            case "ET":
                return (comands.length != 2) ? null : comands;
            case "EI":
                return (comands.length != 2) ? null : comands;
            case "RT":
                return (comands.length != 2) ? null : comands;
            case "RI":
                return (comands.length != 2) ? null : comands;
            default:
                return null;
            }   
    }

    private static void answerMidleware(String comando) throws ClassNotFoundException, IOException{
        if(comando.equals("RI")){
            String resposta = (String)inStream.readObject();
            System.out.println(resposta);
            if (resposta.contains("OK") && !resposta.equals("NOK")) {
                getImgAnswer();
            }
        } else {
            String resposta = getDefaultAnswer();
            System.out.println(resposta);
        }
    }

    private static void answerMidleware(String comando, String fileName) throws IOException {
        String resposta = getDefaultAnswer();
        if(resposta.matches("OK.*")) {
            fileName = "./UserFiles/"+fileName+".txt";
            saveFile(resposta,fileName);
        }
        else
            System.out.println(resposta);
    }

    private static String getDefaultAnswer() {
        try {
            return(String)inStream.readObject();
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void sendImg(ObjectOutputStream outStream, String fileName, String comando) {
        // Read image file into byte array
        byte[] imageData;
        try {
            imageData = Files.readAllBytes(Paths.get("UserFiles/" + fileName));
            outStream.writeObject(comando);

            // Send image to server
            outStream.writeObject(imageData);
            outStream.flush();

            answerMidleware(comando);
            
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("NOK");
            // e.printStackTrace();
        }
    }

    private static void getImgAnswer() {
        try {

            String userId = (String)inStream.readObject();
    
			Integer deviceId = (Integer)inStream.readInt();

            // Receive image from client
            byte[] imageData = (byte[]) inStream.readObject();

            // Save received image to a file
            FileOutputStream fileOutputStream = new FileOutputStream("UserFiles/" + userId + Integer.toString(deviceId) + ".jpg");

            fileOutputStream.write(imageData);
           
            fileOutputStream.close();

        } catch (Exception e) {
            System.out.println("NOK");
        }
    }


    private static void saveFile(String contents, String pathname) throws IOException{
        FileWriter fw = new FileWriter(pathname, false);
        fw.write(contents);
        fw.close();
    }
}
