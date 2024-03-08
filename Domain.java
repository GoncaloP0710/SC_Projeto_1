import java.util.ArrayList;
import java.util.HashMap;

public class Domain {

    private String domainName;
    private String Owner;
    private ArrayList<String> userList = new ArrayList<>();
    private HashMap<String, ArrayList<Integer>> devicesList = new HashMap<>();
    
    /**
     * Cria instancia de objeto do tipo Domain
     * 
     * @param Owner
     * @param domainName
     */
    public Domain(String Owner, String domainName) {
        this.Owner = Owner;
        this.domainName = domainName;
    }

    /**
     * Retorna true se o Owner tem o Id passado
     * 
     * @param userId
     * @return true se o Owner tem o Id passado
     */
    protected boolean isOwner(String userId) {
        return userId.equals(Owner);
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

}
