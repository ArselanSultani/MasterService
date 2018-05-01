import java.util.Map;

public interface ServiceObject {
    int getId();
    Map<Integer, DataObject> getAreaData();
    void addObjectToRoomData (DataObject object);
    void addClientToRoom (ServerClient client);
    void removeClientFromRoom (ServerClient client);

}
