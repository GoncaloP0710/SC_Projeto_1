package src;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class ServerFileManager {

    private static final File users = new File("ServerFiles\\user.csv");
    private static final File domains = new File("ServerFiles\\domains.csv");


    protected static HashMap<String,String> getUsers() throws FileNotFoundException{
        HashMap<String,String> usersMap = new HashMap<>();
        Scanner sc = new Scanner(users);
        while(sc.hasNextLine()) {
            String[] keyValue = sc.nextLine().split(",");
            usersMap.put(keyValue[0], keyValue[1]);

        }
        sc.close();
        return usersMap;
    }

    protected static ArrayList<Domain> getDomains() throws FileNotFoundException{
        ArrayList<Domain> domainsList = new ArrayList<Domain>();
        Scanner sc = new Scanner(domains);
        String[] values = sc.nextLine().split(",");
        
        Domain single = new Domain(values[1], values[0]);
        domainsList.add(single);
        if(Integer.parseInt(values[2]) != -1)
                single.addDevice(values[1], Integer.parseInt(values[2]));
        String[] current = values.clone();
        current[2] = "0";
        while(sc.hasNextLine()) {
            values = sc.nextLine().split(",");
            if(!values[0].equals(current[0])) {
                single = new Domain(values[1], values[0]);
                domainsList.add(single);

            }             
            else if (!values[1].equals(current[1]))  {
                single.addUser(values[1]);
            }
            
            if(Integer.parseInt(values[2]) != -1)
                single.addDevice(values[1], Integer.parseInt(values[2]));
            
            current = values.clone();
        }
        sc.close();
        return domainsList;
    }

    protected synchronized void addUserToFile(String userId, String senha) throws IOException {
        FileWriter myWriter = new FileWriter(users, true);
        myWriter.write(userId + "," + senha + "\n");
        myWriter.close();
    }

    

    public static void main(String[] args) {
        try {
            ArrayList<Domain> domainList = getDomains();
            for(Domain d: domainList) {
                System.out.println("Domain: " + d.getName());
                System.out.println("Users:" + d.getUserList().toString());
                HashMap<String, ArrayList<Integer>> devices = d.getDevicesList();
                for (Map.Entry<String,ArrayList<Integer>> dev: devices.entrySet()) {
                   System.out.println("Devices user: " + dev.getKey() + " devs: " + dev.getValue().toString());

                }
            }
            
        } catch (FileNotFoundException e) {
            System.err.println(e);
        }
        
    }
}