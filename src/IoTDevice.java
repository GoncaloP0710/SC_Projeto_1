package src;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

public class IoTDevice {

    static boolean running;
    static Socket clientSocket;
    static ObjectInputStream inStream;
    static ObjectOutputStream outStream;
    static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) throws ClassNotFoundException {
        try{
            running = true;
            setup();
            String serverAddress = args[0];
            String dev_id_s = args[1];
            String user_id = args[2];
            String[] serverAd = new String[2];
            int dev_id = Integer.parseInt(dev_id_s);
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

            //2
            while(response_info.equals("WRONG-PWD") ){
                senha = pedeSenha();
                response_info = sendInfo(user_id, senha, inStream, outStream);
            }

            //3
            String response_dev = send_device_id(dev_id, inStream, outStream);
            
            while(response_dev.equals("NOK-DEVID") ){
                System.out.println("Esse device esta a ser utilizado por outro cliente \n");
                System.out.println("Escolha outro device: \n");
                dev_id = Integer.parseInt(getAnswer());
                response_dev = send_device_id(dev_id, inStream, outStream);
            }
            //--------------------------------------------------------------

            //5
            // String fileName = IoTDevice.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            // File jarFile = new File(fileName);
            // long tamanho = getFileSize(jarFile);

            // String response_file = send_file_info(tamanho, fileName, inStream, outStream);

            //6
            // if (response_file == "NOK-TESTED") {
            //     clientSocket.close();
            //     System.out.println("Cliente nao validado pelo servidor");
            //     System.exit(0);
            // }

            //7 Imprimir menu de comandos
            System.out.println(interfaceIO());

            //8 e 9
            while(running){
                String[] comands = getComand();
                if(comands != null){
                    // enviar o pedido ao server
                    if(comands[0].equals("EI")){
                        outStream.writeObject(comands[0]);
                        sendImg(outStream, comands[1]);
                    } else{
                        for(int i = 0; i< comands.length; i++){
                            outStream.writeObject(comands[i]);
                        }
                    }
                    answerMidleware(comands[0]);
                } else {
                    System.out.println("invalid command! \n");
                }
                
            }
            
            //------------------------------------------------------

            
            sc.close();
        } catch(IOException ie){
            System.out.println("IO error");
        }
    }

    private static void setup() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    Thread.sleep(200);
                    System.out.println("Shutting down ...");
                    running = false;

                    // TODO: Fechar tudo que e necessario
                    clientSocket.close();

                    //some cleaning up code...
    
                } catch (InterruptedException | IOException e) {
                    Thread.currentThread().interrupt();
                    e.printStackTrace();
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
        String s = sc.nextLine();
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
    
    public static String interfaceIO() {
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
                return (comands.length != 3) ? null : comands;
            default:
                return null;
            }   
    }

    private static void answerMidleware(String comando) throws ClassNotFoundException, IOException{
        if(comando == "RI"){
            String resposta = (String)inStream.readObject();
            if (resposta==("OK")) {
                getImgAnswer();
            } else {
                System.out.println(resposta);
            }
        } else if (comando == "RT") {
            
        } else {
            getDefaultAnswer();
        }

    }

    private static void getDefaultAnswer() {
        try {
            String response = (String)inStream.readObject();
            System.out.println(response);
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendImg(ObjectOutputStream outStream, String fileName) throws IOException {
        // Read image file into byte array
        byte[] imageData = Files.readAllBytes(Paths.get("UserFiles/" + fileName));

        // Send image to server
        outStream.writeObject(imageData);
        outStream.flush();
        System.out.println("Image sent to server.");
    }

    private static void getImgAnswer() {
        try {
            Long imgSize = (Long) inStream.readLong();
            String userId = (String) inStream.readObject();
            Integer deviceId = (Integer) inStream.readInt();
            // Receive image from client
            byte[] imageData = (byte[]) inStream.readObject();

            // Save received image to a file
            FileOutputStream fileOutputStream = new FileOutputStream("ClientFiles/ImageFiles/" + userId + Integer.toString(deviceId) + ".jpg");
            fileOutputStream.write(imageData);
            fileOutputStream.close();
            System.out.println("OK, " + Long.toString(imgSize) + " (long)");
            ServerFileManager.writeImageFilename(userId, deviceId, userId + Integer.toString(deviceId) + ".jpg");
        } catch (Exception e) {
            System.out.println("Image not received due to an error.");
        }
    }

    private static void getTxtAnswer() {
        try {
            String response = (String)inStream.readObject();
            System.out.println(response);
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
    }
}
