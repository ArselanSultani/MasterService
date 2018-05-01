
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by Arsa on 14.10.2017.
 */
public abstract class ServiceInterface {



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
    abstract void notifyUsers(ServerClient cl, CachebleObj obj);



    //getObject recieves a key from parameter and returns the object from
    public List<String> getObject(ServiceObject serviceObject){
        if(serviceObject == null) {
            System.out.println("Could not find room!");
            return Collections.emptyList();
        }


        System.out.println("Found the room and returning it");
        return serviceObject.getAreaData().values().stream().map(i -> i.getObjectName()).collect(Collectors.toList());

    }


    //Here we store a data in our caching system.
    public void putObject (ServerClient client, CachebleObj obj,
                                                       ServiceInterface provider){

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

        objectArea.addObjectToRoomData(obj);

        notifyUsers(client, obj);
        provider.setListOfObservers(listOfObservers);


    }




    //If the time of object on our caching system has expired, then we will
    //remove it from our caching system.
    public void removeObject(Map.Entry<Integer, ServiceObject> pair,
                                                   ServiceInterface provider){
        Map<Integer, ServiceObject> listOfRooms = provider.getListOfLocalObject();
        listOfRooms.remove(pair.getKey(), pair.getValue());
        provider.setListOfLocalObject(listOfRooms);
    }



    //Adding client to the list of observers
    public void addClient(ServerClient newUser, ServiceObject serviceArea, ServiceInterface provider ) {
        Map<Integer, ServiceObject> listOfRooms = provider.getListOfLocalObject();
        Map<ServerClient, ServiceObject> listOfObservers = provider.getListOfObservers();

        if(listOfObservers.get(newUser) != null) {
            System.out.println("User already subscribed");
            return;
        }

        //IF room already exists
        if(listOfRooms.get(serviceArea) != null) {
            System.out.println("Found Existing room to add client to");
            listOfObservers.put(newUser, (ServiceProviderChat.ChatRoom) serviceArea);
            serviceArea.addClientToRoom(newUser);
            return;
        }


        //if the room does not exist

        serviceArea.addClientToRoom(newUser);
        listOfRooms.put(serviceArea.getId(), serviceArea);
        listOfObservers.put(newUser, serviceArea);

        provider.setListOfObservers(listOfObservers);
        provider.setListOfLocalObject(listOfRooms);


        System.out.println("Added client " + newUser.getId()  + " to observer list");

    }

    //Removing client from the list of observers
    public String removeClient (ServerClient removeUser, ServiceInterface provider){

        Map<Integer, ServiceObject> listOfRooms = provider.getListOfLocalObject();
        Map<ServerClient, ServiceObject> listOfObservers = provider.getListOfObservers();

        ServiceObject chatRoom = listOfObservers.get(removeUser);
        if(chatRoom == null) {
            System.out.println("Could not find the user.");
            return "Could not find the user";
        }
        System.out.println(chatRoom.getClientsInThisArea().size());

        chatRoom.removeClientFromRoom(removeUser);
        listOfObservers.remove(removeUser, chatRoom);
        if(chatRoom.getClientsInThisArea().size() == 0) {
            listOfRooms.remove(chatRoom.getId());
            return "User removed and also room removed";
        }
        System.out.println(chatRoom.getClientsInThisArea().size());

        provider.setListOfLocalObject(listOfRooms);
        provider.setListOfObservers(listOfObservers);
        return "User removed only";

    }


    public ServerClient findClient (int clientID, ServiceInterface provider) {
        Map<ServerClient, ServiceObject> listOfObservers = provider.getListOfObservers();
        for(ServerClient serverClient : listOfObservers.keySet()) {
            if (clientID == serverClient.getId()) {
                return serverClient;
            }
        }
        return null;

    }

}
