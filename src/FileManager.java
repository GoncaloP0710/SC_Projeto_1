package src;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

public class FileManager {
    
    public FileManager() {
    }

    protected synchronized HashMap<String, String> getUsersFromFile() throws IOException {
        HashMap<String, String> mapUsers = new HashMap<>();
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
        return mapUsers;
    }

    protected synchronized void addUserToFile(String userId, String senha) throws IOException {
        FileWriter myWriter = new FileWriter("ServerFiles/users.txt", true);
        myWriter.write(userId + ":" + senha + "\n");
        myWriter.close();
    }

    protected synchronized void createDomainTempFile(String domainName) throws IOException {
        File domainTempsLog = new File("ServerFiles/DomainTemps/" + domainName + ".txt");
        FileWriter myWriter = new FileWriter("ServerFiles/DomainTemps/" + domainName + ".txt", true);
        myWriter.write("Ficheiro de Log de temperaturas do domain " + domainName + "\n");
        myWriter.close();
    }

    protected synchronized void addTempToDomainFile(String domainName, Float temp, String userId, Integer deviceId) throws IOException {
        FileWriter myWriter = new FileWriter("ServerFiles/DomainTemps/" + domainName + ".txt", true);
        myWriter.write("<" + userId + ":" + Integer.toString(deviceId) + "> enviou uma temperatura de: " + String.valueOf(temp) + "\n");
        myWriter.close();
    }
}
