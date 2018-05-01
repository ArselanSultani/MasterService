import java.util.List;
import java.util.Map;

public interface ServiceObject {
    int getId();
    Map<Integer, CachebleObj> getAreaData();
    CachebleObj getObject(String name);
    void addObjectToRoomData (CachebleObj object);
    void addClientToRoom (ServerClient client);
    void removeClientFromRoom (ServerClient client);
    List<ServerClient> getClientsInThisArea();

}
