package src;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

public class ServerFileManager {

    private static final File users = new File("ServerFiles\\user.csv");
    private static final File domains = new File("ServerFiles\\domains.csv");
    private static final File userDevices = new File("ServerFiles\\userdevices.csv");
    

    private static final File temps = new File("ServerFiles\\temps.csv");
    private static final File photos = new File("ServerFiles\\photos.csv");

    /**
     * 
     * 
     * @return
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
     * 
     * @return
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
            if(line.isEmpty())
                continue;
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
     * 
     * @param userId
     * @param senha
     * @throws IOException
     */
    protected synchronized static void addUserToFile(String userId, String senha) throws IOException {
        FileWriter myWriter = new FileWriter(users, true);
        myWriter.write(userId + "," + senha + "\n");
        myWriter.close();
    }


    /**
     * 
     * @return
     * @throws IOException
     */
    protected synchronized File getTemperaturesFile(String domainName, String userID, ArrayList<Integer> deviceList) throws IOException{
        FileWriter fw = new FileWriter("ServerFiles\\temps.txt");
        Scanner sc = new Scanner(temps);
        String line;
        String[] values;
        while(sc.hasNextLine()) {
            line = sc.nextLine();
            if(line.isEmpty())
                continue;
            values = line.split(",");
            if(values[0].equals(domainName)) {
                fw.write("Domain: " + values[0] + ", Device: " + values[1] + ":" + values[2] + ", Temp: " + values[3] + "\n");
            }
        }
        fw.close();
        sc.close();

        return temps;
    }

    protected static void writeToDomainsFile(String domain, String userId, Integer device) throws IOException {
        FileWriter fw ;
        Scanner sc = new Scanner(domains);
        String line;
        List<String> lines = new ArrayList<>();
        boolean hasLine = false;
        while(sc.hasNextLine()) {
            line = sc.nextLine();
            if(line.matches(domain + "," + userId + ",.*")) {
                line = domain + "," + userId + "," + device + "\n";
                hasLine = true;
            }
            lines.add(line);
        }
        if(!hasLine) {
            fw = new FileWriter(domains, true);
            fw.write(domain + "," + userId + "," + device + "\n");
        }
        else{
            fw = new FileWriter(domains);
            for(String s: lines)
                fw.write(s);
        }
            
        sc.close();
        fw.close();
    }

    protected void writeToDomainsFile(String domain, String userId) throws IOException {
        FileWriter fw = new FileWriter(domains, true);
        Scanner sc = new Scanner(domain);
        String line;
        List<String> lines = new ArrayList<>();
        boolean hasLine = false;
        while(sc.hasNextLine()) {
            line = sc.nextLine();
            if(line.matches(domain + "," + userId + ",.*"))
                hasLine = true;

            lines.add(line);
        }
        if(!hasLine)
            fw.write("\n" + domain + "," + userId + ",-1");
        sc.close();
        fw.close();
    }

    protected static void writeTemperature(String userId, Integer device, float F) throws FileNotFoundException, IOException {
        Scanner sc = new Scanner(temps);
        List<String> lines = new ArrayList<>();
        String newLine = userId + "," + device + ",";
        boolean foundLine = false;
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            if(line.matches(newLine + ".*")) {
                line = newLine.concat(String.valueOf(F));
                foundLine = true;
            }
            lines.add(line);
        }
        FileWriter fw;
        if(!foundLine) {
            fw = new FileWriter(temps, true);
            fw.write(newLine + String.valueOf(F) + "\n");
        }
        else {
            fw = new FileWriter(temps);
            int length = lines.size();
            for(String line: lines) {
                if (length != 1) 
                    fw.write(line + "\n", 0, line.length() + 1);
                else
                    fw.write(line, 0, line.length());
                length--;
            }
        }
        
        fw.close();
        sc.close();
    }

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

    protected static void writeImageFilename(String userId, Integer device, String filename) throws IOException {
        FileWriter fw;
        Scanner sc = new Scanner(photos);
        boolean foundLine = false;
        List<String> lines = new ArrayList<>();
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            if(line.matches(userId + "," + device + ",.*")) {
                line = userId + "," + device + "," + filename;
                foundLine = true;
            }
            lines.add(line);
        }
        if(foundLine) {
            fw = new FileWriter(photos);
            for(String s: lines) {
                fw.write(s);    
            }
        }
        else {
            fw = new FileWriter(photos,true);
            fw.write(userId + "," + device + "," + filename);
        }
        
        sc.close();
        fw.close();
    } 

    /**
     * 
     * @param userId
     * @param deviceId
     * @throws IOException
     */
    protected synchronized static void addDeviceToFile(String userId, Integer deviceId) throws IOException {
        FileWriter myWriter = new FileWriter(userDevices, true);
        myWriter.write(userId + "," + String.valueOf(deviceId) + "\n");
        myWriter.close();
    }

    /**
     * 
     * 
     * @return
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

    
}