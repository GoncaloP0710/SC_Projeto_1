import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;

public class myServer{

    HashMap<String, String> mapUsers = new HashMap<>();
    HashMap<String, ArrayList<Integer>> mapDevices = new HashMap<>();
    ArrayList<Domain> domains = new ArrayList<>();

    public static void main(String[] args) throws IOException {
		System.out.println("servidor: main");
		myServer server = new myServer();
		server.startServer();
	}

	public void startServer () throws IOException{
		ServerSocket sSoc = null;

        // Atualizar mapUsers com base no .txt ------------------------------------------------------------
        BufferedReader reader = new BufferedReader(new FileReader("ServerFiles/users.txt"));
        String line;
        // Reading text from the file users.txt, splitting by ':', and populating the HashMap mapUsers
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(":");
            if (parts.length >= 2) {
                mapUsers.put(parts[0].trim(), parts[1].trim());
            }
        }
        reader.close();
        // -------------------------------------------------------------------------------------------------
        
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

                // TODO: Comportamento do servidor com o user
                
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

            // Verify whether the user already exists, is new, or has entered an incorrect password
            if (mapUsers.get(userId).equals(null)) {
                result = "OK-NEW-USER";
            } else if (mapUsers.get(userId).equals(senha)) {
                result = "OK-USER";
            } else {
                result = "WRONG-PWD";
            }
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
            if (!mapDevices.get(userId).equals(null)) {
                if (mapDevices.get(userId).contains(deviceId)) {
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


        /**
         * Cria domain se ainda nao existe
         * 
         * @param domainName
         * @param userId
         * @return OK se criado ou NOK se ja existe um domain com esse nome
         * @throws IOException 
         */
        private String newDomain(String domainName, String userId) throws IOException {
            boolean domainExist = false;
            for (int i = 0; i < domains.size(); i++) {
                if (domains.get(i).getName().equals(domainName)) {
                    domainExist = true;
                    break;
                }
            }
            if (domainExist) {
                return "NOK";
            }else {
                Domain newDomain = new Domain(userId, domainName);
                domains.add(newDomain);
                // Criar ficheiro .txt com log das temperaturas desse domain
                File domainTempsLog = new File("ServerFiles/DomainTemps/" + domainName + ".txt");
                // Mudar o ficheiro Domain.txt com a informacao necessaria
                FileWriter myWriter = new FileWriter("ServerFiles/Domain.txt", true);
                myWriter.write(domainName + ":" + userId + "\n");
                myWriter.close();
                
                return "OK";
            }

        }

        /**
         * 
         * @param userID the user's id
         * @param domainName the domain's name 
         * @return OK if user is added to domain, NOUSER if the user doesn't exist, NODM if domain doesn't exist or NOPERM sem permissoes
         * @throws NullPointerException
         */
        private String addUserToDomain(String userId, String domainName) throws NullPointerException {
            String result = "OK";
            boolean hasUser = mapUsers.containsKey(userId);
            if(!hasUser) {
                result = "NOUSER";
                return result;
            }
            boolean hasDomain = false;
            for(Domain domain: domains) {
                if(domainName.equals(domain.getName())) {
                    hasDomain = true;
                    domain.addUser(userId);
                }
            }
            if(!hasDomain) {
                result = "NODM";
            }
            
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
            if (mapDevices.get(domainName).equals(null)) {
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

        /**
         * 
         * @param inStream
         */
        private void getImage(ObjectInputStream inStream) {
            // TODO
        }

        /**
         * 
         * @param outStream
         * @param userId
         * @param deviceId
         */
        private void sendImage(ObjectOutputStream outStream, String userId, Integer deviceId) {
            // TODO 
        }
    }
}