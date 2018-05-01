import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
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


    abstract void setListOfLocal(Map<Integer, ServiceObject> listofLocal) ;
    abstract void setListOfObservers(Map<ServerClient, ServiceObject> listOfObservers);
    abstract Map<Integer, ServiceObject> getListOfLocal();
    abstract Map<ServerClient, ServiceObject> getListOfObservers();
    abstract void notifyUsers(ServerClient cl, CachebleObj obj);
    abstract void addClient(ServerClient newUser, ServiceObject serviceArea);

    //getObject recieves a key from parameter and returns the object from
    public List<String> getObject(ServiceObject serviceObject){
        if(serviceObject == null) {
            System.out.println("Could not find room!");
            return null;
        }


        System.out.println("Found the room and returning it");
        return serviceObject.getLocalData().values().stream().map(i -> i.getObjectName()).collect(Collectors.toList());

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

        objectArea.addObjectToLocalData(obj);

        notifyUsers(client, obj);
        provider.setListOfObservers(listOfObservers);


    }




    //If the time of object on our caching system has expired, then we will
    //remove it from our caching system.
    public void removeObject(Map.Entry<Integer, ServiceObject> pair, ServiceInterface provider){
        Map<Integer, ServiceObject> listOfRooms = provider.getListOfLocal();
        listOfRooms.remove(pair.getKey(), pair.getValue());
        provider.setListOfLocal(listOfRooms);
    }



    //Adding client to the list of observers


    //Removing client from the list of observers
    public String removeClient (ServerClient removeUser, ServiceInterface provider){

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
