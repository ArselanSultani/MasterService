
import java.util.List;
import java.util.Map;

public interface ServiceObject {
    int getId();
    Map<Integer, CachebleObj> getLocalData();
    CachebleObj getObjectLocal(String name);
    void addObjectToLocalData (CachebleObj object);
    void addClientToLocal (ServerClient client);
    void removeClientFromLocal (ServerClient client);
    List<ServerClient> getClientsInThisLocal();

}
