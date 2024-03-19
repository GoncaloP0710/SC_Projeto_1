package src;
import java.util.ArrayList;
import java.util.HashMap;

public class Domain {

    private String domainName;
    private String owner;
    private ArrayList<String> userList = new ArrayList<>();
    private HashMap<String, ArrayList<Integer>> devicesList = new HashMap<>();
    
    /**
     * Cria instancia de objeto do tipo Domain
     * 
     * @param owner
     * @param domainName
     */
    public Domain(String owner, String domainName) {
        this.owner = owner;
        this.domainName = domainName;
        addUser(owner);
        System.out.println("Domain criado -> domain name: " + domainName + " | owner: " + owner);
    }

    /**
     * Retorna true se o owner tem o Id passado
     * 
     * @param userId
     * @return true se o owner tem o Id passado
     */
    protected boolean isowner(String userId) {
        return userId.equals(owner);
    }

    /**
     * 
     * @param userId the user's name
     * @return true if user belongs to the domain
     */
    protected boolean userBelongsTo(String userId) {
        return userList.contains(userId);
    }
    /**
     * 
     * @return the domain's name 
     */
    protected String getName() {
        return domainName;
    }

    /**
     * 
     * @param user the user's name
     */
    protected void addUser(String user) {
        userList.add(user);
        ArrayList<Integer> nArrayList = new ArrayList<>();
        this.devicesList.put(user, nArrayList);
        System.out.println("User " + user + " adicionado ao domain " + domainName);
    }

    /**
     * 
     * @param user the user's name
     * @param deviceId the device's number
     */
    protected void addDevice(String user, Integer deviceId) {
        ArrayList<Integer> devices = devicesList.get(user);
        if (devices !=(null)) {
            if (!devices.contains(deviceId)) {
                devices.add(deviceId);
                devicesList.put(user, devices);
                System.out.println("Device " + user + ":" + Integer. toString(deviceId) + " adicionado ao domain " + domainName);
            }
        }
    }

    /**
     * 
     * @param owner the owner's name
     */
    protected void setOwner(String owner) {
        this.owner = owner;
    }

    /**
     * 
     * @param userId the user that will read
     * @param deviceId the device of the user that will read
     * @param userIdReader the id of the user
     * @return
     * @requires  userId != null && deviceId != null && userIdReader && devicesList.get(userId) != null
     */
    protected boolean hasPermissionToRead(String userId, Integer deviceId, String userIdReader, Integer deviceIdReader) {
        if (!deviceBelongsTo(userIdReader, deviceIdReader)) {
            return  false;
        }
        return deviceBelongsTo(userId, deviceId);
    }

    /**
     * 
     * @param userId the user
     * @param deviceId the device
     * @return
     * @requires userId != null && deviceId != null && devicesList.get(userId) != null
     */
    protected boolean deviceBelongsTo(String userId, Integer deviceId) {
        if (devicesList.get(userId)==null) {
            return false;
        }
        return devicesList.get(userId).contains(deviceId);
    }


    protected ArrayList<String> getUserList() {
        return userList;
    }

    protected HashMap<String, ArrayList<Integer>> getDevicesList() {
        return devicesList;
    }
}
