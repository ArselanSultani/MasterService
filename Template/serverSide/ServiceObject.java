
import java.util.List;
import java.util.Map;

public interface ServiceObject {
    int getId();
    Map<Integer, DataObject> getLocalData();
    DataObject getObjectLocal(String name);
    void addObjectToLocalData (DataObject object);
    void addClientToLocal (ServerClient client);
    void removeClientFromLocal (ServerClient client);
    List<ServerClient> getClientsInThisLocal();

}
