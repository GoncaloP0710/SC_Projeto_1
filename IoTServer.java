import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.imageio.ImageIO;

public class IoTServer{

    HashMap<String, String> mapUsers = new HashMap<>();
    HashMap<String, ArrayList<Integer>> mapDevices = new HashMap<>();
    ArrayList<Domain> domains = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        try {
            System.out.println("servidor: main");
            IoTServer server = new IoTServer();
            server.startServer(Integer.parseInt(args[0]));
        } catch (NumberFormatException e) {
            System.err.println("Port number is not valid");
            System.exit(-1);
        } catch (IOException e) {
            System.err.println("Couldn't start server");
        }
	}

	public void startServer (Integer socket) throws IOException{
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
        BufferedReader reader = new BufferedReader(new FileReader("ServerFiles/domains.txt"));
        String line;
        // Reading text from the file users.txt, splitting by ':', and populating the HashMap mapUsers
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(":");
            String domainName = parts[0];
            Domain domain = new Domain("",domainName);
            for(int i = 1; i < parts.length(); i++) {
                
                String[] namesDev = parts[i].split("|");
                String user = namesDev[0];
                if(i == 1)
                    domain.setOwner(user);
                domain.addUser(user);
                List<Integer> devices = new ArrayList<>();
                for(int j = 1; j < namesDev; j++) {
                    devices.add(Integer.parseInt(namesDev[j]));
                    domain.addDevice(user, Integer.parseInt(namesDev[j]));
                }
                mapDevices.put(user,devices);
            }
            domains.add(domain);
        }
        reader.close();
        // -------------------------------------------------------------------------------------------------
        
		try {
			sSoc = new ServerSocket(socket);
		} catch (IOException | NullPointerException e) {
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
         * @param deviceId
         * @return OK se criado ou NOK se ja existe um domain com esse nome
         * @throws IOException 
         */
        private String newDomain(String domainName, String owner, Integer deviceId) throws IOException {
            for(Domain domain: domains) {
                if (domain.getName().equals(domainName)) {
                    return "NOK";
                }
            }
            
            Domain newDomain = new Domain(owner, domainName);
            newDomain.addDevice(owner, deviceId);
            domains.add(newDomain);
            
            // Criar ficheiro .txt com log das temperaturas desse domain
            File domainTempsLog = new File("ServerFiles/DomainTemps/" + domainName + ".txt");
            // Mudar o ficheiro Domain.txt com a informacao necessaria
            FileWriter myWriter = new FileWriter("ServerFiles/domains.txt", true);
            
            // ------------------------- TODO : Escrever no ficheiro
            myWriter.write(domainName + ":" + owner + "\n");
            // ------------------------------------------------------------------
            
            myWriter.close();
            
            return "OK";
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
        private void sendDomainTemp(String domainName, ObjectOutputStream outStream, String userId, Integer deviceId) throws IOException {
            
            // Verifica se o domain existe
            if (!domainExist(domainName)) {
                outStream.writeObject("NODM");
                return;
            }

            // Verifica se o ficheiro existe
            if (!dataExist("ServerFiles/DomainTemps" + domainName + ".txt")) {
                outStream.writeObject("NODATA");
                return;
            }

            // Verifica se o user tem permissoes
            for(Domain domain: domains) {
                if (domain.hasPermissionToRead(userId, deviceId)) {
                    break;
                }
                outStream.writeObject("NOPERM");
                return;
            }

            // Diferente de como fiz na TP -> Confirmar se esta bem
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
         * @throws IOException 
         */
        private void getImage(ObjectInputStream inStream, String userId, Integer deviceId) throws IOException {
            // Recebe tamanho do array
            long size = inStream.readLong();
            
            byte[] array = new byte[(int) size];
            // Recebe array com a imagem
            inStream.readFully(array);
            
            Path path = Paths.get("ServerFiles/ImageFiles/" + userId + Integer.toString(deviceId) + ".jpg");
            Files.write(path, array);
        }

        /**
         * 
         * 
         * https://www.tutorialspoint.com/How-to-convert-Image-to-Byte-Array-in-java
         * @param outStream
         * @param userId
         * @param deviceId
         * @throws IOException 
         */
        private void sendImage(ObjectOutputStream outStream, String userId, Integer deviceId) throws IOException {

            // Verifica se esse device id não existe
            if (mapDevices.get(userId).equals(null)) {
                outStream.writeObject("NOID");
                return;
            } else if (!mapDevices.get(userId).contains(deviceId)) {
                outStream.writeObject("NOID");
                return;
            }

            // Verifica se o ficheiro existe
            if (!dataExist("ServerFiles/ImageFiles" + userId + Integer.toString(deviceId) + ".jpg")) {
                outStream.writeObject("NODATA");
                return;
            }

            // Verifica se o user tem permissoes
            for(Domain domain: domains) {
                if (domain.hasPermissionToRead(userId, deviceId)) {
                    break;
                }
                outStream.writeObject("NOPERM");
                return;
            }

            // Envia a imagem
            BufferedImage bImage = ImageIO.read(new File("ServerFiles/ImageFiles/" + userId + Integer.toString(deviceId) + ".jpg"));
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(bImage, "jpg", bos );
            byte [] data = bos.toByteArray();
            
            outStream.writeLong(data.length);
            outStream.write(data);
        }

        /**
         * Verifica se existe um domain com o nome dado
         * 
         * @param domainName
         * @return true se existe um domain com o nome dado
         */
        private boolean domainExist(String domainName) {
            for(Domain domain: domains) {
                if (domain.getName().equals(domainName)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Verifica se existe um ficheiro com o caminho dado
         * 
         * @param path
         * @return true se existe um ficheiro com o caminho dado
         */
        private boolean dataExist(String path) {
            return new File(path).isFile();
        }
    }
}