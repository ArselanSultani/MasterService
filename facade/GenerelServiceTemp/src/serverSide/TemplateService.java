
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by Arsa on 14.10.2017.
 */
public abstract class TemplateService {



    /**
     *The following methods are for handling the caching of the service.
     *The caching of the system will be handled as a hashmap.
     *Where everything we have saved in the map, will have a key which
     *can retrieve it from.
     */


    abstract void setListOfLocalObject(Map<Integer, ServiceObject> listOfRooms) ;
    abstract void setListOfObservers(Map<ServerClient, ServiceObject> listOfObservers);
    abstract Map<Integer, ServiceObject> getListOfLocalObject();
    abstract Map<ServerClient, ServiceObject> getListOfObservers();
    abstract  void addClient(ServerClient newUser, ServiceObject serviceArea );


    /**
     * Getting list of message for case 1 and file names in case 2
     * @param serviceObject     The service object (ChatRoom or StorageArea) where the names will be retrieved from.
     * @return                  List of filenames and messages
     */
    public List<String> getObjectList(ServiceObject serviceObject){
        if(serviceObject == null) {
            System.out.println("Could not find room!");
            return Collections.emptyList();
        }


        System.out.println("Found the room and returning it");
        return serviceObject.getLocalData().values().stream().map(i -> i.getObjectName()).collect(Collectors.toList());

    }


    /**
     * Adding a file or message to its area
     * @param client        The client that wants to add it.
     * @param obj           The object that is being added
     * @param provider      The provider the client is using
     */
    public void putObject (ServerClient client, DataObject obj,
                                                       TemplateService provider){

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
     * For removing client from service
     * @param removeUser    the client that is being removed
     * @param provider      From the provider that the user is using
     * @return              returning a string to give feedback whether the client is removed or not
     */
    public String removeClient (ServerClient removeUser, TemplateService provider){

        Map<Integer, ServiceObject> listOfArea = provider.getListOfLocalObject();
        Map<ServerClient, ServiceObject> listOfObservers = provider.getListOfObservers();

        ServiceObject area = listOfObservers.get(removeUser);
        if(area == null) {
            System.out.println("Could not find the user.");
            return "Could not find the user";
        }

        area.removeClientFromLocal(removeUser);
        listOfObservers.remove(removeUser, area);
        if(area.getClientsInThisLocal().size() == 0) {
            listOfArea.remove(area.getId());
            return "User removed and also room removed";
        }

        provider.setListOfLocalObject(listOfArea);
        provider.setListOfObservers(listOfObservers);
        return "User removed only";

    }

    /**
     * Finding the clied
     * @param clientID      The client ID of the client that wants to be found
     * @param provider      The provider that the client is subscribed to
     * @return              The client with that client ID.
     */
    public ServerClient findClient (int clientID, TemplateService provider) {
        Map<ServerClient, ServiceObject> listOfObservers = provider.getListOfObservers();
        for(ServerClient serverClient : listOfObservers.keySet()) {
            if (clientID == serverClient.getId()) {
                return serverClient;
            }
        }
        return null;

    }

}
