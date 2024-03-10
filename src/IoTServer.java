package src;
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

                // autenticar o utilizador
                String[] userInfo = getUserInfo(inStream);
                while (autentifyUserInfo(userInfo[0], userInfo[1]).equals("WRONG-PWD")) {
                    outStream.writeObject("WRONG-PWD");
                    userInfo = getUserInfo(inStream);
                }
                outStream.writeObject(autentifyUserInfo(userInfo[0], userInfo[1]));
                addNewUser(userInfo[0], userInfo[1]);

                // obter <device-id>
                Integer deviceId = getDeviceId(inStream, userInfo[0]);
                while (deviceId.equals(null)) {
                    outStream.writeObject("NOK-DEVID");
                    deviceId = getDeviceId(inStream, userInfo[0]);
                }
                addNewDevice(userInfo[0], deviceId);
                outStream.writeObject("OK-DEVID");

                // Verificar integridade dos dados
                if (!verifyEXEC(inStream)) {
                    outStream.writeObject("NOK-TESTED");
                    stop(); // TODO: Ver depois
                }
                outStream.writeObject("OK-TESTED");

                // TODO: LOOP
                String comand = (String)inStream.readObject();
                String domainName;
                String result;
                String userIdToBeAdded;
                String userImg;

                switch (comand) {
                    case "CREATE":
                        domainName = (String)inStream.readObject();
                        result = newDomain(domainName, userInfo[0], deviceId);
                        outStream.writeObject(result);
                        break;

                    case "ADD":
                        userIdToBeAdded = (String)inStream.readObject();
                        domainName = (String)inStream.readObject();
                        result = addUserToDomain(userIdToBeAdded, userInfo[0], domainName);
                        outStream.writeObject(result);
                        break;
            
                    case "RD":
                        domainName = (String)inStream.readObject();
                        result = addDeviceToDomain(userInfo[0], deviceId, domainName);
                        outStream.writeObject(result);
                        break;

                    case "ET":
                        result = getTemperature(inStream);
                        outStream.writeObject(result);
                        break;

                    case "EI":
                        result = getImage(inStream, userInfo[0], deviceId);
                        outStream.writeObject(result);
                        break;

                    case "RT":
                        domainName = (String)inStream.readObject();
                        sendDomainTemp(domainName, outStream, userInfo[0], deviceId);
                        break;

                    case "RI":
                        userImg = (String)inStream.readObject();
                        sendImage(outStream, userImg, deviceId, userInfo[0]);
                        break;

                    default:
                        System.out.println("no match");
                    }

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        
        }

        /**
         * Obter user-id e senha do cliente para o server
         * 
         * @param inStream Stream para receber dados
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
         * @param userId userId do user
         * @param senha senha do user
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
         * Recebe <device-id> do user retorna null se ja existir um igual
         * 
         * @param inStream Stream para receber dados
         * @param userId userId do user
         * @throws ClassNotFoundException
         * @throws IOException
         */
        private Integer getDeviceId(ObjectInputStream inStream, String userId) throws ClassNotFoundException, IOException {
            int deviceId = (int)inStream.readObject();

            // Verifica se existe outro IoTDevice aberto  e  autenticado  com  o  mesmo  par  (<user-id>,<dev-id>)
            if (!mapDevices.get(userId).equals(null)) {
                if (mapDevices.get(userId).contains(deviceId)) {
                    return null;
                }
            }

            return deviceId;
        }

        /**
         * Adiciona info do novo user ao file users.txt e ao hashmap mapUsers
         * 
         * @param userId userId do dispositivo a ser criado
         * @param senha senha do dispositivo a ser criado
         * @throws IOException
         */
        private void addNewUser(String userId, String senha) throws IOException {
            FileWriter myWriter = new FileWriter("ServerFiles/users.txt", true);
            myWriter.write(userId + ":" + senha + "\n");
            myWriter.close();
            mapUsers.put(userId, senha);
        }

        /**
         * Adiciona deviceId a lista do mapDevices correspondente ao user
         * 
         * @param userId userId do dispositivo em causa
         * @param deviceId deviceId a ser criado
         * @throws IOException
         */
        private void addNewDevice(String userId, Integer deviceId) throws IOException {
            ArrayList<Integer> devices = mapDevices.get(userId);
            devices.add(deviceId);
            mapDevices.put(userId, devices);
        }

        /**
         * Verifica o nome e tamanho do file passados pelo user 
         * 
         * @param inStream Stream para receber dados
         * @return true se dados passados estao corretos
         * @throws IOException 
         * @throws ClassNotFoundException 
         */
        private boolean verifyEXEC(ObjectInputStream inStream) throws ClassNotFoundException, IOException {
            File file = new File("ServerFiles/IoTDevice.class");
            String fileName = (String)inStream.readObject();
			long fileSize = (Long)inStream.readObject();
            if (file.getName().equals(fileName) && file.length()==(fileSize)) {
                return true;
            }
            return false;
        }

        /**
         * Cria domain se ainda nao existe
         * 
         * @param domainName mome do domain a ser criado
         * @param userId userId do dispositivo que cria o domain
         * @param deviceId deviceId do dispositivo que cria o domain
         * @return OK se criado ou NOK se ja existe um domain com esse nome
         * @throws IOException 
         */
        private String newDomain(String domainName, String owner, Integer deviceId) throws IOException {
            if (domainExist(domainName)) {
                return "NOK"; 
            }
            
            Domain newDomain = new Domain(owner, domainName);
            newDomain.addDevice(owner, deviceId);
            domains.add(newDomain);
            
            // Criar ficheiro .txt com log das temperaturas desse domain
            // TODO: Ver se deve mudar para json ou algo diferente
            File domainTempsLog = new File("ServerFiles/DomainTemps/" + domainName + ".txt");
            // Mudar o ficheiro Domain.txt com a informacao necessaria
            FileWriter myWriter = new FileWriter("ServerFiles/domains.txt", true);
            
            // TODO: Alteracoes ao ficheiro: ServerFiles/domains.txt
            // TODO: Ver se deve mudar para json ou algo diferente

            myWriter.close();
            
            return "OK";
        }

        /**
         * 
         * @param userID the user's id to be added
         * @param domainName the domain's name 
         * @return OK if user is added to domain, NOUSER if the user doesn't exist, NODM if domain doesn't exist or NOPERM sem permissoes
         * @throws NullPointerException
         */
        private String addUserToDomain(String userIdToBeAdded, String userIdToAdd, String domainName) throws NullPointerException {
            String result = "OK";
            boolean hasUser = mapUsers.containsKey(userIdToBeAdded);
            if(!hasUser) {
                result = "NOUSER";
                return result;
            }
            boolean hasDomain = false;
            for(Domain domain: domains) {
                if(domainName.equals(domain.getName())) {
                    hasDomain = true;
                    if (!domain.isowner(userIdToAdd)) {
                        result = "NOPERM";
                        return result;
                    }
                    domain.addUser(userIdToBeAdded);
                    // TODO: Alteracoes ao ficheiro: ServerFiles/domains.txt
                    // TODO: Ver se deve mudar para json ou algo diferente
                    return result;
                }
            }
            if(!hasDomain) {
                result = "NODM";
            }
            return result;
        }

        /**
         * Adiciona o device ao domain passado (Se ja la estiver retorna OK)
         * 
         * @param userId userId do device a adicionar
         * @param deviceId deviceId a adicionar
         * @param domainName domain a qual o device vai ser adicionado
         * @return
         * @throws NullPointerException
         */
        private String addDeviceToDomain(String userId, Integer deviceId, String domainName) throws NullPointerException {
            String result = "OK";
            boolean hasDomain = false;
            for(Domain domain: domains) {
                if(domainName.equals(domain.getName())) {
                    hasDomain = true;
                    if (!domain.belongsTo(userId)) {
                        result = "NOPERM";
                        return result;
                    }
                    domain.addDevice(userId, deviceId);
                    // TODO: Alteracoes ao ficheiro: ServerFiles/domains.txt
                    // TODO: Ver se deve mudar para json ou algo diferente
                    return result;
                }
            }
            if(!hasDomain) {
                result = "NODM";
            }
            return result;
        }

        /**
         * Obtem a temperatura emviada pelo user e faz as necessarias alteracoes
         * 
         * @param inStream Stream para receber dados
         * @return retorna NOK se ocurreu um erro e OK se tudo correu bem
         */
        private String getTemperature(ObjectInputStream inStream){
            String result = "OK"; 
            try {
                Float temperature = inStream.readFloat();
                // TODO: Alteracoes ao ficheiro: ServerFiles/DomainTemps/(...).txt
                // TODO: Ver se deve mudar para json ou algo diferente
                // TODO: Confirmar quando e que o servidor nao aceita -> Se assim funciona
                return result;
            } catch (IOException e) {
                result = "NOK";
                return result;
            }
	    }

        /**
         * Envia o ficheiro de temperatura para o <user_id>:<dev_id> passado se for possivel
         * 
         * @param domainName nome do domain referente ao ficheiro
         * @param outStream Stream para enviar dados
         * @param userId userId do dispositivo que recebe o ficheiro
         * @param deviceId deviceId do dispositivo que recebe o ficheiro
         * @throws IOException
         */
        private void sendDomainTemp(String domainName, ObjectOutputStream outStream, String userId, Integer deviceId) throws IOException {
            
            // Verifica se o domain existe
            if (!domainExist(domainName)) {
                outStream.writeObject("NODM");
                return;
            }

            // Verifica se o ficheiro existe
            if (!UtilsIoT.dataExist("ServerFiles/DomainTemps" + domainName + ".txt")) {
                outStream.writeObject("NODATA");
                return;
            }

            // Verifica se o user tem permissoes
            for(Domain domain: domains) {
                if (domain.getName().equals(domainName)) {
                    if (!domain.belongsTo(userId)) {
                        outStream.writeObject("NOPERM");
                        return;
                    }
                    break;
                }
            }

            // Diferente de como fiz na TP -> Confirmar se esta bem
            // TODO: Alteracoes ao ficheiro: ServerFiles/DomainTemps/(...).txt
            // TODO: Ver se deve mudar para json ou algo diferente
            File fileToSend = new File("ServerFiles/DomainTemps/" + domainName + ".txt");
            long size = fileToSend.length();
            // ------------------------------------------------------------------------------------

            outStream.writeLong(size);
            byte[] buffer = Files.readAllBytes(fileToSend.toPath());
            outStream.write(buffer);

            outStream.writeObject("buffer");
        }

        /**
         * Recebe imagem de um <user_id>:<dev_id>
         * 
         * @param inStream Stream para receber dados
         * @param userId userId do dispositivo que envia a imagem
         * @param deviceId deviceId do dispositivo que envia a imagem
         * @throws IOException
         */
        private String getImage(ObjectInputStream inStream, String userId, Integer deviceId) {
            String result = "OK";
            try {
                // Recebe tamanho do array
                long size = inStream.readLong();
                
                byte[] array = new byte[(int) size];
                // Recebe array com a imagem
                inStream.readFully(array);
                
                Path path = Paths.get("ServerFiles/ImageFiles/" + userId + Integer.toString(deviceId) + ".jpg");
                Files.write(path, array);
                return result;

            } catch (Exception e) {
                result = "NOK";
                return result;
            }
        }

        /**
         * Envia imagem do dispositivo <user_id>:<dev_id>
         * 
         * https://www.tutorialspoint.com/How-to-convert-Image-to-Byte-Array-in-java
         * @param outStream Stream para enviar dados
         * @param userId userId do dispositivo a que pertence a imagem
         * @param deviceId deviceId do dispositivo a que pertence a imagem
         * @throws IOException 
         */
        private void sendImage(ObjectOutputStream outStream, String userId, Integer deviceId, String userIdToRecive) throws IOException {

            // Verifica se esse device id não existe
            if (mapDevices.get(userId).equals(null)) {
                outStream.writeObject("NOID");
                return;
            } else if (!mapDevices.get(userId).contains(deviceId)) {
                outStream.writeObject("NOID");
                return;
            }

            // Verifica se o ficheiro existe
            if (!UtilsIoT.dataExist("ServerFiles/ImageFiles" + userId + Integer.toString(deviceId) + ".jpg")) {
                outStream.writeObject("NODATA");
                return;
            }

            // Verifica se o user tem permissoes
            boolean permissoes = false;
            for(Domain domain: domains) {
                if (domain.hasPermissionToRead(userId, deviceId, userIdToRecive)) {
                    permissoes = true;
                    break;
                }
            }

            if (!permissoes) {
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
         * @param domainName nome do domain
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
    }
}