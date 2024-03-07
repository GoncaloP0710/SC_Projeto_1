import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;

public class myServer{

    HashMap<String, String> mapUsers = new HashMap<>();
    HashMap<String, ArrayList<Integer>> mapDomains = new HashMap<>();

    public static void main(String[] args) {
		System.out.println("servidor: main");
		myServer server = new myServer();
		server.startServer();
	}

	public void startServer (){
		ServerSocket sSoc = null;
        
		try {
			sSoc = new ServerSocket(12345);
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		}
         
		while(true) {
			try {
				Socket inSoc = sSoc.accept();
				ServerThread newServerThread = new ServerThread(inSoc);
				newServerThread.start();
		    }
		    catch (IOException e) {
		        e.printStackTrace();
		    }
		    
		}
		// sSoc.close();
	}

    //Threads utilizadas para comunicacao com os clientes
	class ServerThread extends Thread {

		private Socket socket = null;

		ServerThread(Socket inSoc) {
			socket = inSoc;
			System.out.println("thread do server para cada cliente");
		}
 
		public void run(){
			
            try {
                ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());
                
            } catch (IOException e) {
                e.printStackTrace();
            }
        
        }

        /**
         * Obter user-id e senha do cliente para o server
         * Assumesse que userId e senha sao validas
         * 
         * @param outStream
         * @param inStream
         * @throws ClassNotFoundException
         * @throws IOException
         */
        private String[] getUserInfo(ObjectInputStream inStream) throws ClassNotFoundException, IOException {
            String userId = (String)inStream.readObject();
			String senha = (String)inStream.readObject();
            String[] credentials = {userId, senha};
            return credentials;
        }

        /**
         * Verifica os dados do user e retorna mensagem apropriada
         * 
         * @param userId
         * @param senha
         * @throws IOException 
         */
        private String autentifyUserInfo(String userId, String senha) throws IOException {
            String result;

            // Reading text from a file
            // BufferedReader reader = new BufferedReader(new FileReader("ServerFiles/users.txt"));
            // String line;

            // Reading text from the file users.txt, splitting by ':', and populating the HashMap mapUsers
            // while ((line = reader.readLine()) != null) {
            //     String[] parts = line.split(":");
            //     if (parts.length >= 2) {
            //         mapUsers.put(parts[0].trim(), parts[1].trim());
            //     }
            // }

            // Verify whether the user already exists, is new, or has entered an incorrect password
            if (mapUsers.get(userId).equals(null)) {
                result = "OK-NEW-USER";
            } else if (mapUsers.get(userId).equals(senha)) {
                result = "OK-USER";
            } else {
                result = "WRONG-PWD";
            }

            // reader.close();
            return result;
        }

        /**
         * Recebe <device-id> do cliente e faz as verificaçoes necessarias
         * 
         * @param inStream
         * @return
         * @throws ClassNotFoundException
         * @throws IOException
         */
        private String getDeviceId(ObjectInputStream inStream, String userId) throws ClassNotFoundException, IOException {
            String result = "OK-DEVID";
            int deviceId = (int)inStream.readObject();

            // Verifica se existe outro IoTDevice aberto  e  autenticado  com  o  mesmo  par  (<user-id>,<dev-id>)
            if (!mapDomains.get(userId).equals(null)) {
                if (mapDomains.get(userId).contains(deviceId)) {
                    result = "NOK-DEVID";
                }
            }

            return result;
        }

        /**
         * Adiciona info do user ao file users.txt e ao hashmap mapUsers
         * 
         * @param userId
         * @param senha
         * @throws IOException
         */
        private void addNewUser(String userId, String senha) throws IOException {
            FileWriter myWriter = new FileWriter("ServerFiles/users.txt", true);
            myWriter.write(userId + ":" + senha + "\n");
            myWriter.close();
            mapUsers.put(userId, senha);
        }

        private void newDomain(String domainName) {
            // TODO: Verificar se tal domain ja existe
            File domainTempsLog = new File("ServerFiles/DomainTemps/" + domainName + ".txt"); // Criar ficheiro .txt com log das temperaturas desse domain
            // TODO: Todas alteracoes necessarias
        }

        private String addUserToDomain(String userId, String domain){
                String result = "OK";
                // TODO: Todas as veridicacoes, ou seja, se pode ser adicionado ou nao. Fazer todas as alteracoes necessarias (incluindo a String result)
                return result;
        }

        /**
         * Obtem a temperatura emviada pelo user
         * 
         * @param inStream
         * @return retorna null se ocurreu um erro
         */
        private Float getTemperature(ObjectInputStream inStream){
            try {
                Float temperature = inStream.readFloat();
                return temperature;
            } catch (IOException e) {
                return null;
            }
	    }

        /**
         * 
         * 
         * @param domainName
         * @param outStream
         * @throws IOException
         */
        private void sendDomainTemp(String domainName, ObjectOutputStream outStream) throws IOException {
            if (mapDomains.get(domainName).equals(null)) {
                outStream.writeObject("NODM");
                return;
            }

            // TODO: Verificar as permissoes e NODATA

            // Diferente de como esta na TP -> Confirmar se esta bem
            File fileToSend = new File("ServerFiles/DomainTemps/" + domainName + ".txt");
            long size = fileToSend.length();
            // ------------------------------------------------------------------------------------

            outStream.writeLong(size);
            byte[] buffer = Files.readAllBytes(fileToSend.toPath());
            outStream.write(buffer);

            outStream.writeObject("buffer");
        }

        private void getImage(ObjectInputStream inStream) {
            // TODO
        }

        private void sendImage(ObjectOutputStream outStream, String userId, Integer deviceId) {
            // TODO 
        }
    }
}
