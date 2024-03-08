import java.util.ArrayList;
import java.util.HashMap;

public class Domain {

    String domainName;
    String Owner;
    ArrayList<String> userList = new ArrayList<>();
    HashMap<String, ArrayList<Integer>> devicesList = new HashMap<>();
    
    public Domain (String Owner, String domainName) {
        this.Owner = Owner;
        this.domainName = domainName;
    }

}
