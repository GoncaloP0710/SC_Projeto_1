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

    protected boolean belongsTo(String userId) {
        return userList.contains(userId);
    }

    protected String getName() {
        return domainName;
    }

    protected void addUser(String user) {
        userList.add(user);
    }

    protected void addDevice(String user, Integer deviceId) {
        if (!devicesList.get(user).equals(null)) {
            if (!devicesList.get(user).contains(deviceId)) {
                ArrayList<Integer> devices = devicesList.get(user);
                devices.add(deviceId);
                devicesList.put(user, devices);
            }
        }
    }

    protected void setOwner(String owner) {
        this.owner = owner;
    }

    protected boolean hasPermissionToRead(String userId, Integer deviceId, String userIdReader) {
        if (!belongsTo(userIdReader)) {
            return  false;
        }
        if (devicesList.get(userId).equals(null)) {
            return  false;
        } else if (!devicesList.get(userId).contains(deviceId)) {
            return false;
        }
        return true;
    }
}
