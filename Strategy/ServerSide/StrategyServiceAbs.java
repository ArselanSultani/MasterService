import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by Arsa on 14.10.2017.
 */
public abstract class StrategyServiceAbs {


    abstract void setListOfLocal(Map<Integer, ServiceObject> listofLocal) ;
    abstract void setListOfObservers(Map<ServerClient, ServiceObject> listOfObservers);
    abstract Map<Integer, ServiceObject> getListOfLocal();
    abstract Map<ServerClient, ServiceObject> getListOfObservers();

    /**
     * For adding client to a service
     * @param newUser           The client that is going to be added
     * @param serviceArea       The service area that it is going to be added on
     */
    abstract void addClient(ServerClient newUser, ServiceObject serviceArea);

    /**
     * For starting the service with necessary information
     * @param clientSocket      The socket which the client is connected through
     * @param fromClient        The stream to receive data from client
     * @param toClient          The stream for sending data to client
     */
    abstract void startService(Socket clientSocket, ObjectInputStream fromClient, ObjectOutputStream toClient);


    /**
     * For getting a list of objects from a ServiceArea
     * @param serviceObject     From the serviceObject to get data from
     * @return                  Return list of file names or messages from the serviceObject
     */
    public List<String> getObjectList(ServiceObject serviceObject){
        if(serviceObject == null) {
            System.out.println("Could not find room!");
            return null;
        }


        System.out.println("Found the room and returning it");
        return serviceObject.getLocalData().values().stream().map(i -> i.getObjectName()).collect(Collectors.toList());

    }

    /**
     * Putting object to the area
     * @param client            The client that wants to add object
     * @param obj               The object that is being added
     * @param provider          The provider for which the client is subscribed to
     */
    public void putObject (ServerClient client, DataObject obj,
                                                       StrategyServiceAbs provider){

        Map<ServerClient, ServiceObject> listOfObservers = provider.getListOfObservers();

        if (obj == null) {
            System.out.println("No object to add");
            return ;
        }


        ServiceObject objectArea = listOfObservers.get(client);

        if(objectArea == null) {
            System.out.println("User is not subscribed to this service");
            return;
        }

        objectArea.addObjectToLocalData(obj);

        provider.setListOfObservers(listOfObservers);


    }


    /**
     * For removing client from service and its area
     * @param removeUser        The client that is being removed
     * @param provider          The provider for which the client is subscribed to
     * @return                  A message whether the client is removed or not
     */
    public String removeClient (ServerClient removeUser, StrategyServiceAbs provider){

        Map<Integer, ServiceObject> listOfRooms = provider.getListOfLocal();
        Map<ServerClient, ServiceObject> listOfObservers = provider.getListOfObservers();

        ServiceObject chatRoom = listOfObservers.get(removeUser);
        if(chatRoom == null) {
            System.out.println("Could not find the user.");
            return "Could not find the user";
        }

        chatRoom.removeClientFromLocal(removeUser);
        listOfObservers.remove(removeUser, chatRoom);
        if(chatRoom.getClientsInThisLocal().size() == 0) {
            listOfRooms.remove(chatRoom.getId());
            return "User removed and also room removed";
        }

        provider.setListOfLocal(listOfRooms);
        provider.setListOfObservers(listOfObservers);
        return "User removed only";

    }


}
