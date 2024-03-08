import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class IoTDevice {
    

    public static void main(String[] args) {
        try{
            String serverAddress = args[0];
            String dev_id_s = args[1];
            String user_id = args[0];
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
            //------------------------
            String senha = pedeSenha();

            //-------------------------------------
            Socket clientSocket = new Socket(ip, port);
            ObjectInputStream inStream = new ObjectInputStream(clientSocket.getInputStream());
            ObjectOutputStream outStream = new ObjectOutputStream(clientSocket.getOutputStream());
            //-------------------------------------------------------------

            String response = sendInfo(user_id, senha, inStream, outStream);

            while(response == "WRONG-PWD"){
                senha = pedeSenha();
                response = sendInfo(user_id, senha, inStream, outStream);
            }

            String response_dev = send_device_id(dev_id, inStream, outStream);
            while(response_dev == "NOK-DEVID"){
                System.out.println("Esse device esta a ser utilizado por outro cliente \n");
                System.out.println("Escolha outro device: \n");
                dev_id = Integer.parseInt(getAnswer());
                response_dev = send_device_id(dev_id, inStream, outStream);
            }


            clientSocket.close();
        } catch(IOException e){
            System.out.println("IO error");
        }
    }

    private static String pedeSenha(){
        System.out.println("Indique a sua senha:");
        return getAnswer();
    }

    private static String getAnswer() {
        Scanner sc = new Scanner(System.in);
        String s = sc.nextLine();
        sc.close();
        return s;
    }


    private static String sendInfo(String user, String password, ObjectInputStream in, ObjectOutputStream out) throws IOException {
        out.writeObject(user);
        out.writeObject(password);
        try{
            String result = (String) in.readObject();
            return result;
        } catch (ClassNotFoundException e1) {
            e1.printStackTrace();
        }
        return null;
    }

    private static String send_device_id(int dev_id, ObjectInputStream in, ObjectOutputStream out) throws IOException {
        out.writeObject(dev_id);
        try {
            String result = (String) in.readObject();
            return result;
        } catch (ClassNotFoundException e1) {
            e1.printStackTrace();
        }
        return null;
    }

}
