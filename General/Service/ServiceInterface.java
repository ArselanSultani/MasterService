import java.util.List;

/**
 * Created by Arsa on 14.10.2017.
 */
public interface ServiceInterface {

    int returnId();


    public void startServer();

    void addClient(ServerClient newUser, ServiceObject chatRoom);
    String removeClient (ServerClient removeUser, Thread ct);
    ServerClient findClient (int clientID);

    List<String> getObjectList(ServiceObject serviceObject);
    void putObject(ServerClient client, DataObject obj);

}
