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
        while(sc.hasNextLine()) {
            String[] keyValue = sc.nextLine().split(",");
            usersMap.put(keyValue[0], keyValue[1]);

        }
        sc.close();
        return usersMap;
    }

    /**
     * 
     * @return
     * @throws FileNotFoundException
     */
    protected synchronized ArrayList<Domain> getDomains() throws FileNotFoundException{
        ArrayList<Domain> domainsList = new ArrayList<Domain>();
        Scanner sc = new Scanner(domains);
        String[] values = sc.nextLine().split(",");
        
        Domain single = new Domain(values[1], values[0]);
        domainsList.add(single);
        if(Integer.parseInt(values[2]) != -1)
                single.addDevice(values[1], Integer.parseInt(values[2]));
        String[] current = values.clone();
        current[2] = "0";
        boolean hasDomain = false;
        while(sc.hasNextLine()) {
            values = sc.nextLine().split(",");
            for(Domain d: domainsList) {
                if(d.getName().equals(values[0])) {
                    hasDomain = true;
                    single = d;
                    break;
                }

            }
            if(!hasDomain) {
                single = new Domain(values[1], values[0]);
                domainsList.add(single);

            }             
            else if (!single.userBelongsTo(values[1]))  {
                single.addUser(values[1]);
            }
            
            if(Integer.parseInt(values[2]) != -1)
                single.addDevice(values[1], Integer.parseInt(values[2]));
            
            current = values.clone();
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
    protected synchronized void addUserToFile(String userId, String senha) throws IOException {
        FileWriter myWriter = new FileWriter(users, true);
        myWriter.write(userId + "," + senha + "\n");
        myWriter.close();
    }


    /**
     * 
     * @return
     * @throws IOException
     */
    protected synchronized File getTemperaturesFile(String domainName) throws IOException{
        FileWriter fw = new FileWriter("ServerFiles\\temps.csv");
        Scanner sc = new Scanner(temps);
        while(sc.hasNextLine()) {
            String[] values = sc.nextLine().split(",");
            if(values[0].equals(domainName)) {
                fw.write("Domain: " + values[0] + ", Device: " + values[1] + ":" + values[2] + ", Temp: " + values[3] + "\n");
            }
        }
        fw.close();
        sc.close();

        return temps;
    }

    protected void writeToDomainsFile(String domain, String userId, Integer device) throws IOException {
        FileWriter fw = new FileWriter(domains, true);
        fw.write("\n" + domain + "," + userId + "," + device + ",-1");
        fw.close();
    }

    protected void writeTemperature(String domainName, String userId, Integer device, float F) throws FileNotFoundException, IOException {
        Scanner sc = new Scanner(temps);
        List<String> lines = new ArrayList<>();
        String newLine = domainName + "," + userId + "," + device + ",";
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
        if(foundLine) {
            fw = new FileWriter(temps, true);
            fw.write(newLine + "," + String.valueOf(F));
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

    protected String getImageFilename(String userId, Integer device)  throws FileNotFoundException{
        Scanner sc = new Scanner(photos);
        String result = userId + "," + device + ",";
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            if(line.matches(result)) {
                sc.close();
                return line.substring(result.length());
            }
        }
        sc.close();
        return "NODATA";
    }

    protected void writeImageFilename(String userId, Integer device, String filename) throws IOException {
        FileWriter fw = new FileWriter(photos,true);
        fw.write(userId + "," + device + "," + filename);
        fw.close();
    } 



    
}