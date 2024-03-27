package server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

/**
 * Builds a connection between server and files
 * 
 * @author André Reis fc58192
 * @author Gonçalo Pinto fc58178
 * @author José Brás fc55449
 */
public class ServerFileManager {

    private static final File users = new File("ServerFiles/user.csv");
    private static final File domains = new File("ServerFiles/domains.csv");
    private static final File userDevices = new File("ServerFiles/userdevices.csv");
    private static final File testFiles = new File("ServerFiles/testFile.csv");
    

    private static final File temps = new File("ServerFiles/temps.csv");
    private static final File photos = new File("ServerFiles/photos.csv");

    /**
     * Gets all the users from User's Files
     * 
     * @return all the users from User's Files
     * @requires users != null
     * @ensures \result != null
     * @throws FileNotFoundException
     */
    protected static synchronized HashMap<String,String> getUsers() throws FileNotFoundException{
        HashMap<String,String> usersMap = new HashMap<>();
        Scanner sc = new Scanner(users);
        String line;
        while(sc.hasNextLine()) {
            line = sc.nextLine();
            if(line.isEmpty())
                continue;
            String[] keyValue = line.split(",");
            usersMap.put(keyValue[0], keyValue[1]);

        }
        sc.close();
        return usersMap;
    }

    /**
     * Gets all devices from userdevices
     * 
     * @requires userdevices != null
     * @ensures \result != null
     * @return all devices from userdevices
     * @throws FileNotFoundException
     */
    protected static synchronized HashMap<String,ArrayList<Integer>> getUsersDevices() throws FileNotFoundException{
        HashMap<String,ArrayList<Integer>> usersDevicesMap = new HashMap<>();
        Scanner sc = new Scanner(userDevices);
        String line;
        while(sc.hasNextLine()) {
            line = sc.nextLine();
            String[] keyValue = line.split(",");
            if(!usersDevicesMap.containsKey(keyValue[0])) {
                ArrayList<Integer> devices = new ArrayList<>();
                devices.add(Integer.parseInt(keyValue[1]));
                usersDevicesMap.put(keyValue[0], devices);
            }
            else {
                ArrayList<Integer> devices = usersDevicesMap.get(keyValue[0]);
                devices.add(Integer.parseInt(keyValue[1]));
            }

        }
        sc.close();
        return usersDevicesMap;
    }

    /**
     * Gets all domains and information about domains
     * 
     * @return all domains and information about domains
     * @requires domains != null
     * @ensures \result != null
     * @throws FileNotFoundException
     */
    protected synchronized static ArrayList<Domain> getDomains() throws FileNotFoundException{
        ArrayList<Domain> domainsList = new ArrayList<Domain>();
        Scanner sc = new Scanner(domains);
        String line;
        String values[] = null;
        boolean hasDomain = false;
        Domain single = null;
        while(sc.hasNextLine()) {
            line = sc.nextLine();
            values = line.split(",");
            for(Domain d: domainsList) {
                if(d.getName().equals(values[0])) {
                    hasDomain = true;
                    single = d;
                    break;
                }
            }
            if(!hasDomain) {
                single = new Domain(values[1], values[0]);
            }             
            else if (!single.userBelongsTo(values[1]))  {
                single.addUser(values[1]);
            }

            if(Integer.parseInt(values[2]) != -1)
                single.addDevice(values[1], Integer.parseInt(values[2]));

            if(!hasDomain)
                domainsList.add(single);

            
            hasDomain = false;
        }
        sc.close();
        return domainsList;
    }

    /**
     * Adds a user to users
     * 
     * @param userId the user's Id
     * @param senha the password
     * @requires userId != null && senha != null
     * @throws IOException
     */
    protected synchronized static void addUserToFile(String userId, String senha) throws IOException {
        String newLine = userId + ',' + senha + "\n";
        if(!hasDuplicate(users, newLine)) {
            FileWriter myWriter = new FileWriter(users, true);
            myWriter.write(newLine);
            myWriter.close();
        }
        
    }


    /**
     * Gets all temperatures from the file temperature
     * 
     * @param domainName the name of the domain
     * @param userId the user's ID
     * @param deviceList the list of the devices beloging to the user
     * @return all temperatures from the file temperature
     * @requires domainName != null && userID != null && deviceList != null && deviceList.length() > 1
     * @throws IOException
     */
    protected synchronized File getTemperaturesFile(String domainName, String userID, ArrayList<Integer> deviceList) throws IOException{
        FileWriter fw = new FileWriter("ServerFiles\\temps.txt");
        Scanner sc = new Scanner(temps);
        String line;
        String[] values;
        while(sc.hasNextLine()) {
            line = sc.nextLine();
            values = line.split(",");
            if(values[0].equals(domainName)) {
                fw.write("Domain: " + values[0] + ", Device: " + values[1] + ":" + values[2] + ", Temp: " + values[3] + "\n");
            }
        }
        fw.close();
        sc.close();

        return temps;
    }

    /**
     * Adds a domain, a user and a device to the file domains.csv
     * 
     * @param domain the name of the domain
     * @param userId the user's Id
     * @param device the device
     * @requires domain != null && userId != null && device != null
     * @throws IOException
     */
    protected static void writeToDomainsFile(String domain, String userId, Integer device) throws IOException {
        String newLine = domain + "," + userId + "," + device + "\n";
        if(!hasDuplicate(domains, newLine)) {
            FileWriter fw = new FileWriter(domains, true);
            fw.write(newLine);
            fw.close();
        }
    }

    /**
     * Adds a domain, a user and the deafault device -1 to the file domains.csv
     * 
     * @param domain the name of the domain
     * @param userId the user's Id
     * @requires domain != null && userId != null
     * @throws IOException
     */
    protected void writeToDomainsFile(String domain, String userId) throws IOException {
        String newLine = domain + "," + userId + ",.*";
        if(!hasDuplicate(domains, newLine)) {
            FileWriter fw = new FileWriter(domains, true);
            fw.write("\n" + domain + "," + userId + ",-1");
            fw.close();
        }
    }

    /**
     * Adds a temperature to the file temps.csv
     * 
     * @param userId the user's id
     * @param device the device
     * @param F the temperature in float
     * @requires userId != null && device != null
     * @throws FileNotFoundException
     * @throws IOException
     */
    protected static void writeTemperature(String userId, Integer device, float F) throws FileNotFoundException, IOException {
        String lineMatch = userId + "," + device;
        List<String> lines = getLines(temps, lineMatch, String.valueOf(F));
        FileWriter fw;
        if(lines == null) {
            fw = new FileWriter(temps, true);
            fw.write(lineMatch + "," + String.valueOf(F) + "\n");
        }
        else {
            fw = new FileWriter(temps);
            for(String line: lines) {
                    fw.write(line + "\n", 0, line.length() + 1);
            }
        }
        
        fw.close();
    }

    /**
     * Gets the image's filename from a user's device in the file phtos.csv
     * 
     * @param  the user's id
     * @param device the device
     * @return the image's filename
     * @requires userId != null && device != null
     * @ensures \result != null
     * @throws FileNotFoundException
     */
    protected static String getImageFilename(String userId, Integer device)  throws FileNotFoundException{
        Scanner sc = new Scanner(photos);
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            if(line.isEmpty())
                continue;
            if(line.matches(userId + "," + device + ",.*")) {
                sc.close();
                String[] result = line.split(",");
                return result[2];
            }
        }
        sc.close();
        return "NODATA";
    }

    /**
     * Adds the filename to the file photos.csv
     * 
     * @param userId the user's id
     * @param device the device
     * @param filename the name of the file
     * @requires userId != null && device != null && filename != null
     * @throws IOException
     */
    protected static void writeImageFilename(String userId, Integer device, String filename) throws IOException {
        FileWriter fw;
        String lineMatch = userId + "," + device;
        List<String> lines = getLines(photos, lineMatch, filename);
        if(lines != null) {
            fw = new FileWriter(photos);
            for(String s: lines)
                fw.write(s + "\n");    
        }
        else {
            fw = new FileWriter(photos,true);
            fw.write(userId + "," + device + "," + filename + "\n");
        }
        fw.close();
    } 

    /**
     * Adds device to file usersdevice
     * 
     * @param userId the user's id
     * @param deviceId the device's id
     * @requires userId != null && deviceId != null
     * @throws IOException
     */
    protected synchronized static void addDeviceToFile(String userId, Integer deviceId) throws IOException {
        String newLine = userId + "," + deviceId + "\n";
        if(!hasDuplicate(userDevices, newLine)) {
            FileWriter fw = new FileWriter(userDevices, true);
            fw.write(newLine);
            fw.close();
        }
    }

    /**
     * Gets all devices from the file userdevices
     * 
     * @return all devices from the file userdevices
     * @ensures \result != null
     * @throws FileNotFoundException
     */
    protected static synchronized HashMap<String,Integer> getDevices() throws FileNotFoundException{
        HashMap<String,Integer> devicesMap = new HashMap<>();
        Scanner sc = new Scanner(userDevices);
        while(sc.hasNextLine()) {
            String[] keyValue = sc.nextLine().split(",");
            devicesMap.put(keyValue[0], Integer.parseInt(keyValue[1]));

        }
        sc.close();
        return devicesMap;
    }

    /**
     * Gets the temperatures of all users
     * 
     * @return temperatures of all users
     * @throws FileNotFoundException
     */
    protected static synchronized HashMap<String,ArrayList<Float[]>> getUsersDevicesTemps() throws FileNotFoundException{
        
        HashMap<String, ArrayList<Float[]>> getUsersDevicesTemps = new HashMap<>();
        Scanner sc = new Scanner(temps);
        String line;
        while(sc.hasNextLine()) {
            line = sc.nextLine();
            String[] keyValue = line.split(",");

            if (getUsersDevicesTemps.get(keyValue[0]) == null) {
                ArrayList<Float[]> listinha = new ArrayList<>();
                Float[] a = {Float.valueOf(keyValue[1]),Float.valueOf(keyValue[2])};
                listinha.add(a);
                getUsersDevicesTemps.put(keyValue[0], listinha);
            } else {
                Float[] a1 = {Float.valueOf(keyValue[1]),Float.valueOf(keyValue[2])};
                ArrayList<Float[]> a2 = getUsersDevicesTemps.get(keyValue[0]);
                a2.add(a1);
                getUsersDevicesTemps.put(keyValue[0], a2);
            }
            
        }
        sc.close();
        return getUsersDevicesTemps;
    }

    /**
     * Checks if a line already exists in a file so it prevents duplicates
     * 
     * @param f the csv file
     * @param newLine the line that will be added to file
     * @return true if the line already exists in the file
     * @requires newLine != null
     * @throws FileNotFoundException
     */
    private static boolean hasDuplicate(File f, String newLine) throws FileNotFoundException{
        boolean result = false;
        Scanner sc = new Scanner(f);
        String line;
        while (sc.hasNextLine()) {
            line = sc.nextLine();
            if(line.matches(newLine)) {
                sc.close();
                return true;
            }
        }
        sc.close();
        return result;
    }

    /**
     * Avoids repetition on files that need the lines to be changed in the middle of the file
     * 
     * @param f the file
     * @param lineMatch the line to match a line in the file
     * @param elem the element that will be added
     * @return null if the line is not found, the whole file with the fixed line
     * @requires elem != null && lineMatch != null
     * @throws FileNotFoundException
     */
    private static List<String> getLines(File f, String lineMatch, String elem) throws FileNotFoundException{
        List<String> lines = new ArrayList<>(); 
        Scanner sc = new Scanner(f);
        boolean foundLine = false;
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            if(line.matches(lineMatch + ",.*")) {
                line = lineMatch + "," + elem;
                foundLine = true;
            }
            lines.add(line);
        }
        sc.close();
        return foundLine?lines:null;
    }

    /**
     * Devolve info sobre ficheiro jar a ser verificado
     * 
     * @return info sobre ficheiro jar a ser verificado
     */
    public static String[] getFileInfo() {
        String[] fileInfo = new String[2];
        try {
            BufferedReader reader = new BufferedReader(new FileReader(testFiles));
            fileInfo[0] = reader.readLine();
            fileInfo[1] = reader.readLine();
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fileInfo;
    }
}