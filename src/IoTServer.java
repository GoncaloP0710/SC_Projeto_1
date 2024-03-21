package src;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class IoTServer{

    HashMap<String, String> mapUsers = new HashMap<>();
    HashMap<String, ArrayList<Integer>> mapDevices = new HashMap<>();
    HashMap<String, ArrayList<Integer>> mapDevicesOnline = new HashMap<>();
    ArrayList<Domain> domains = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        try {
            System.out.println("servidor: main");
            IoTServer server = new IoTServer();
            if (args.length!=1) {
                server.startServer(12345);
            } else {
                server.startServer(Integer.parseInt(args[0]));
            }
            
        } catch (NumberFormatException e) {
            System.err.println("Port number is not valid");
            System.exit(-1);
        } catch (IOException e) {
            System.err.println("Couldn't start server");
        }
	}

	public void startServer (Integer socket) throws IOException{
		ServerSocket sSoc = null;
        mapUsers = ServerFileManager.getUsers();
        domains = ServerFileManager.getDomains();
        mapDevices = ServerFileManager.getUsersDevices();
        

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
                sSoc.close();
		        // e.printStackTrace();
		    }
		    
		}
		// sSoc.close();
	}

    //Threads utilizadas para comunicacao com os clientes
	class ServerThread extends Thread {

		private Socket socket = null;
        protected String userId;
        protected Integer deviceId;

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

                System.out.println("UserId: " + userInfo[0]);
                System.out.println("UserPass: " + userInfo[1]);
                while (autentifyUserInfo(userInfo[0], userInfo[1]).equals("WRONG-PWD")) {
                    outStream.writeObject("WRONG-PWD");
                    userInfo = getUserInfo(inStream);
                    System.out.println("UserId: " + userInfo[0]);
                    System.out.println("UserPass: " + userInfo[1]);
                }

                outStream.writeObject(autentifyUserInfo(userInfo[0], userInfo[1]));
                if (autentifyUserInfo(userInfo[0], userInfo[1]) == "OK-NEW-USER") {
                    addNewUser(userInfo[0], userInfo[1]);
                }
                System.out.println("User:Password -> " + userInfo[0] + ":" + userInfo[1]);

                this.userId = userInfo[0];

                // obter <device-id>
                Integer deviceId = getDeviceId(inStream, userInfo[0]);
                while (deviceId == null) {
                    outStream.writeObject("NOK-DEVID");
                    deviceId = getDeviceId(inStream, userInfo[0]);
                }
                addDevice(userInfo[0], deviceId);
                outStream.writeObject("OK-DEVID");

                this.deviceId = deviceId;

                // Verificar integridade dos dados
                // if (!verifyEXEC(inStream)) {
                //     outStream.writeObject("NOK-TESTED");
                //     stop(); // TODO: Ver depois
                // }
                // outStream.writeObject("OK-TESTED");

                String comand = (String)inStream.readObject();
                String domainName;
                String result;
                String userIdToBeAdded;
                String userImg;

                while (true) {
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
                            result = getTemperature(inStream, userInfo[0], deviceId);
                            outStream.writeObject(result);
                            break;
    
                        case "EI":
                            System.out.print("EI: Servidor deve receber imagem \n");
                            result = getImage(inStream, userInfo[0], deviceId);
                            outStream.writeObject(result);
                            break;
    
                        case "RT":
                            domainName = (String)inStream.readObject();
                            sendDomainTemp(domainName, outStream, userInfo[0], deviceId);
                            break;
    
                        case "RI":
                            userImg = (String)inStream.readObject();
                            String[] values = userImg.split(":");
                            if (values.length!=2 || !UtilsIoT.isInteger(values[1])) {
                                outStream.writeObject("NOK");
                                break;
                            }
                            sendImage(outStream, values[0], Integer.valueOf(values[1]), userInfo[0]);
                            break;
    
                        default:
                            System.out.println("no match");
                        }
                    comand = (String)inStream.readObject();
                }

            } catch (IOException | ClassNotFoundException e) {
                System.out.println("O cliente desconectou-se");

                ArrayList<Integer> devicesOnline = mapDevicesOnline.get(userId);
                devicesOnline.remove(deviceId);
                mapDevices.put(userId, devicesOnline);
                // e.printStackTrace();
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
            if (mapUsers.get(userId)==(null)) {
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
            System.out.println("deviceId recebido: " + deviceId + "\n");

            // Verifica se existe outro IoTDevice aberto  e  autenticado  com  o  mesmo  par  (<user-id>,<dev-id>)
            if (mapDevicesOnline.get(userId)!=(null)) {
                if (mapDevicesOnline.get(userId).contains(deviceId)) {
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

            // Adicionar user ao csv
            ServerFileManager.addUserToFile(userId, senha);
            
            mapUsers.put(userId, senha);
            System.out.println("Adicionar user: " + userId);
            if (mapDevices.get(userId)==null) {
                ArrayList<Integer> newAIntegers = new ArrayList<>();
                mapDevices.put(userId, newAIntegers);
                System.out.println("entrou if");
            }
        }

        /**
         * Adiciona deviceId a lista do mapDevices correspondente ao user
         * 
         * @param userId userId do dispositivo em causa
         * @param deviceId deviceId a ser criado
         * @throws IOException
         */
        private void addDevice(String userId, Integer deviceId) throws IOException {
            ArrayList<Integer> devices = mapDevices.get(userId);
            if (!devices.contains(deviceId)) {
                devices.add(deviceId);
                mapDevices.put(userId, devices);
                ServerFileManager.addDeviceToFile(userId, deviceId);
            }
            
            ArrayList<Integer> devicesOnline = mapDevicesOnline.get(userId);
            if (devicesOnline!=null) {
                if (!devicesOnline.contains(deviceId)) {
                    devicesOnline.add(deviceId);
                }
            } else {
                ArrayList<Integer> devicesOnlineNewList = new ArrayList<>();
                devicesOnlineNewList.add(deviceId);
                mapDevicesOnline.put(userId, devicesOnlineNewList);
            }
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
            
            // Adicionar domain ao csv
            ServerFileManager.writeToDomainsFile(domainName, owner, deviceId);
            
            return "OK";
        }

        /**
         * 
         * @param userID the user's id to be added
         * @param domainName the domain's name 
         * @return OK if user is added to domain, NOUSER if the user doesn't exist, NODM if domain doesn't exist or NOPERM sem permissoes
         * @throws NullPointerException
         * @throws IOException 
         */
        private String addUserToDomain(String userIdToBeAdded, String userIdToAdd, String domainName) throws NullPointerException, IOException {
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
                    if (!domain.isowner(userIdToAdd) || !domain.deviceBelongsTo(userIdToAdd, this.deviceId)) {
                        result = "NOPERM";
                        return result;
                    }
                    domain.addUser(userIdToBeAdded);
                    ServerFileManager.writeToDomainsFile(domainName, userIdToBeAdded, -1);
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
         * @throws IOException 
         */
        private String addDeviceToDomain(String userId, Integer deviceId, String domainName) throws NullPointerException, IOException {
            String result = "OK";
            boolean hasDomain = false;
            for(Domain domain: domains) {
                if(domainName.equals(domain.getName())) {
                    hasDomain = true;
                    if (!domain.userBelongsTo(userId)) {
                        result = "NOPERM";
                        return result;
                    }
                    domain.addDevice(userId, deviceId);
                    ServerFileManager.writeToDomainsFile(domainName, userId, deviceId);

                    return result;
                }
            }
            if(!hasDomain) {
                result = "NODM";
                return result;
            }
            return result;
        }

        /**
         * Obtem a temperatura emviada pelo user e faz as necessarias alteracoes
         * 
         * @param inStream Stream para receber dados
         * @return retorna NOK se ocurreu um erro e OK se tudo correu bem
         */
        private String getTemperature(ObjectInputStream inStream, String userId, Integer deviceId){
            String result = "OK"; 
            try {
                String temperatureString = (String) inStream.readObject();
                float temperature = Float.parseFloat(temperatureString);

                System.out.println("Temperature recived from user: " + userId + " was: " + temperatureString + "\n");

                ServerFileManager.writeTemperature(userId, deviceId, temperature);

                return result;
            } catch (IOException | NumberFormatException | ClassNotFoundException e) {
                result = "NOK";
                System.out.println("Temperature format not aproved\n");
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

            // Verifica se o user tem permissoes
            for(Domain domain: domains) {
                if (domain.getName().equals(domainName)) {
                    if (!domain.userBelongsTo(userId) || !domain.deviceBelongsTo(userId, deviceId)) {
                        outStream.writeObject("NOPERM");
                        return;
                    }
                    break;
                }
            }
            
            // Get domain
            Domain domain = null;
            for(Domain domains: domains) {
                if(domains.getName().equals(domainName)){
                    domain = domains;
                    break;
                }
            }

            File fileToSend = new File("ServerFiles/DomainTemps/" + domainName + ".txt");
            FileWriter fw = new FileWriter("ServerFiles/DomainTemps/" + domainName + ".txt", false);

            String temps = "Temperaturas do Domain " + domainName + "\n";

            // Verifica NODATA e se existir edita o ficheiro das temps 
            HashMap<String, ArrayList<Float[]>> mapinha = ServerFileManager.getUsersDevicesTemps();
            
            boolean pertence = false;
            for (Map.Entry<String, ArrayList<Float[]>> entry : mapinha.entrySet()) {
                String key = entry.getKey();
                ArrayList<Float[]> value = entry.getValue();
                for (Float[] array : value) {
                    if (domain.deviceBelongsTo(key,array[0].intValue())) {
                        pertence = true;
                        String toConcat = "User " + key +" with device " + String.valueOf(array[0].intValue()) + " sent a temperature of " + String.valueOf(array[1]) + "\n";
                        temps = temps.concat(toConcat);
                    }
                }
            }

            if (!pertence) {
                outStream.writeObject("NODATA");
                fw.close();
                return;
            }

            fw.write(temps);
            fw.close();
            
            long size = fileToSend.length();
            byte[] buffer = Files.readAllBytes(fileToSend.toPath());

            outStream.writeObject("OK, " + Long.toString(size) + " (long), " + temps);
            
            //TODO: Enviar o Ficheiro
            System.out.println("File sent to client.");
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
                // Receive image from client
                byte[] imageData = (byte[]) inStream.readObject();

                // Save received image to a file
                FileOutputStream fileOutputStream = new FileOutputStream("ServerFiles/ImageFiles/" + userId + Integer.toString(deviceId) + ".jpg");
                fileOutputStream.write(imageData);
                fileOutputStream.close();
                System.out.println("Image received and saved.");
                ServerFileManager.writeImageFilename(userId, deviceId, userId + Integer.toString(deviceId) + ".jpg");

                return result;

            } catch (Exception e) {
                result = "NOK";
                return result;
            }
        }

        /**
         * Envia imagem do dispositivo <user_id>:<dev_id>
         * 
         * @param outStream Stream para enviar dados
         * @param userId userId do dispositivo a que pertence a imagem
         * @param deviceId deviceId do dispositivo a que pertence a imagem
         * @throws IOException 
         */
        private void sendImage(ObjectOutputStream outStream, String userId, Integer deviceId, String userIdToRecive) throws IOException {

            // Verifica se esse device id não existe
            if (mapDevices.get(userId)==(null)) {
                outStream.writeObject("NOID");
                return;
            } else if (!mapDevices.get(userId).contains(deviceId)) {
                outStream.writeObject("NOID");
                return;
            }

            // Verifica se o ficheiro existe
            if (!UtilsIoT.dataExist("ServerFiles/ImageFiles/" + userId + Integer.toString(deviceId) + ".jpg")) {
                outStream.writeObject("NODATA");
                return;
            }

            // Verifica se o user tem permissoes
            boolean permissoes = false;
            for(Domain domain: domains) {
                // TODO: Alterar o this e fazer chamar na funcao
                if (domain.hasPermissionToRead(userId, deviceId, userIdToRecive, this.deviceId)) {
                    permissoes = true;
                    break;
                }
            }

            if (!permissoes) {
                outStream.writeObject("NOPERM");
                return;
            }
    
            String filename = ServerFileManager.getImageFilename(userId, deviceId);
            System.out.println(filename);

            // Read image file into byte array
            byte[] imageData = Files.readAllBytes(Paths.get("ServerFiles/ImageFiles/" + filename));

            // Send image to server
            outStream.writeObject("OK, " + Long.toString(imageData.length) + " (long)");
            outStream.writeObject(userId);
            outStream.writeInt(deviceId);

            outStream.writeObject(imageData);
            outStream.flush();

            System.out.println("Image sent to client.");
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