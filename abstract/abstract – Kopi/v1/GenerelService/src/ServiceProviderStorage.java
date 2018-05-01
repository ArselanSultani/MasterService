

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
/*
public class ServiceProviderStorage  extends ServiceInterface {
    ReentrantLock lock = new ReentrantLock();

    private Map<Integer, ServiceObject> listOfAreas = new HashMap<Integer, ServiceObject>();
    private Map<ServerClient, StorageArea> listOfObservers = new HashMap<ServerClient, StorageArea>();

    private List<ClientThread> clientThreads = new ArrayList<>();


    private int portNr = 1254;

    private final int maxComponents = 8 ;

    private AtomicInteger id = new AtomicInteger();
    private AtomicInteger areaId = new AtomicInteger();
    private AtomicInteger objectId = new AtomicInteger();

    public static void main(String[] args) {
        new ServiceProviderStorage().startServer();
    }

    public void startServer() {

        try ( ServerSocket serverSocket = new ServerSocket(portNr) ) {
            while(true) {

                Socket clientSocket = serverSocket.accept();
                System.out.println("A connection has been established: " + clientSocket.getInetAddress());

                ClientThread clientThread = new ClientThread(clientSocket, this);
                Thread singleClientThread = new Thread(clientThread);

                clientThreads.add(clientThread);
                singleClientThread.start();


                Thread.sleep(2000);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public int returnId(){
        lock.lock();
        try{
            id = new AtomicInteger(id.get() +1);
        } finally {
            lock.unlock();
        }
        return id.get();
    }

    public AtomicInteger returnAreaId() {
        lock.lock();
        try {
            areaId = new AtomicInteger(areaId.get()+1);
            return areaId;
        } finally {
            lock.unlock();
        }
    }

    public AtomicInteger getObjectId() {
        lock.lock();

        try {
            objectId = new AtomicInteger(objectId.get()+1);
            return objectId;
        } finally {
            lock.unlock();
        }
    }

    //getObject recieves a key from parameter and returns the object from
    //Whether the object gets returned or not depends on size and network connection type
    public List<String> getObject(ServiceObject serviceObject){

        lock.lock();
        try {
            if(serviceObject == null) {
                System.out.println("Could not find room!");
                return null;
            }


            System.out.println("Found the room and returning it");
            List<String> namesToList = serviceObject.getAreaData().values().stream().map(i -> i.getObjectName()).collect(Collectors.toList());

            return namesToList;
        } finally {
            lock.unlock();
        }

    }


    //Here, the single object gets returned, like for example a single file in  a
    public CachebleObj getSingleObject(ServerClient client,String name){
        lock.lock();
        try {
            return listOfObservers.get(client).getObject(name);
        } finally {
            lock.unlock();
        }
    }

    //Here we store a data in our caching system.

    public void putObject(ServerClient client, CachebleObj obj) {
        lock.lock();

        try {
            if (obj == null) {
                System.out.println("No object to add");
                return;
            }

            StorageArea storageArea = listOfObservers.get(client);

            if(storageArea == null) {
                System.out.println("User is not subscribed to this service");
                return;
            }

            storageArea.addObjectToRoomData(obj);

            notifyUsers(client, obj);
        } finally {
            lock.unlock();
        }

    }

    //If the time of object on our caching system has expired, then we will
    //remove it from our caching system.
    public void removeObject( Map.Entry<Integer, ServiceObject> pair, ServerClient cl){
        //System.out.println("Remove " + pair.getValue().getNa() + " from cache");
        //caching.remove(pair.getKey(), pair.getValue());

        lock.lock();
        try {
            listOfAreas.remove(pair.getKey(), pair.getValue());
        } finally {
            lock.unlock();
        }
    }


    public void debugPrint(){
        /*for(Map.Entry<Integer, CachebleObj> pair: listOfAreas.entrySet()) {
            System.out.println(pair.getKey()+ " - " + pair.getValue().getNa());
        }
    }
        */

    /*public void notifyUsers(ServerClient cl,  CachebleObj obj) {
        lock.lock();
        StorageArea cr = listOfObservers.get(cl);
        try {
            for (ClientThread ct : clientThreads) {
                if (listOfObservers.get(ct.getTheClient()) == cr) {
                    ct.toClient.writeObject("Room has been updated");
                }
            }
        } catch (IOException e) {

        }   finally {
            lock.unlock();
        }
    }

    //Adding client to the list of observers
    public void addClient(ServerClient newUser, ServiceObject chatRoom) {
        lock.lock();

        try {
            if(listOfObservers.get(newUser) != null) {
                System.out.println("User already subscribed");
                return;
            }

            //IF room already exists
            if(listOfAreas.get(chatRoom) != null) {
                System.out.println("Found Existing room to add client to");
                listOfObservers.put(newUser, (StorageArea) chatRoom);
                chatRoom.addClientToRoom(newUser);
                return;
            }


            //if the room does not exist

            listOfAreas.put(chatRoom.getId(), chatRoom);
            listOfObservers.put(newUser, (StorageArea) chatRoom);
            chatRoom.addClientToRoom(newUser);

            System.out.println("Added client " + newUser.getId()  + " to observer list");
        } finally {
            lock.unlock();
        }
    }

    //Removing client from the list of observers
    public String removeClient (ServerClient removeUser){

        lock.lock();

        try {

            StorageArea storageArea = listOfObservers.get(removeUser);
            if(storageArea == null) {
                System.out.println("Could not find the user.");
                return "Could not find the user";
            }
            System.out.println(storageArea.getClientsInThisArea().size());

            //storageArea.removeClientFromRoom(removeUser);
            storageArea.removeClientFromRoom(removeUser);
            listOfObservers.remove(removeUser, storageArea);
            if(storageArea.getClientsInThisArea().size() == 0) {
                listOfAreas.remove(storageArea.getId());
                return "User removed and also room removed";
            }
            System.out.println(storageArea.getClientsInThisArea().size());
            return "User removed only";
        /*if (listOfObservers.remove(removeUser))
            System.out.println("Removed " + removeUser.getName() + " from subscribers to this service");
        else
            System.out.println(removeUser.getName() +" is not subscribed to this service");
        } finally {
            lock.unlock();
        }
    }


    public ServerClient findClient (int clientID) {

        lock.lock();

        try {
            for(ServerClient serverClient : listOfObservers.keySet()) {
                if (clientID == serverClient.getId()) {
                    return serverClient;
                }
            }
            return null;
        } finally {
            lock.unlock();
        }
    }

    public void removeClientSocket (ClientThread clientToRemove) {
        System.out.println("Removed client si");
        clientThreads.remove(clientToRemove);
    }*/


    /****************************************************************************************************************
     * **************************************************************************************************************
     * **************************************************************************************************************
     * **************************************************************************************************************
     */

   /* public void fillChatRoomData(ServiceObject serviceObject){



        CachebleObj c1 = new CachebleObj("first", new String("first"), 4);
        CachebleObj c2 = new CachebleObj("second", new String("second"), 1);
        CachebleObj c3 = new CachebleObj("third", new String("third"), 3);
        c2.setId(getObjectId().get());
        c3.setId(getObjectId().get());
        serviceObject.addObjectToRoomData(c1);
        serviceObject.addObjectToRoomData(c2);
        serviceObject.addObjectToRoomData(c3);
    }


    class StorageArea implements ServiceObject{

        Map<Integer, CachebleObj> areaData = new HashMap<Integer, CachebleObj>();

        List<ServerClient> clientsInThisArea = new ArrayList<ServerClient>();

        private AtomicInteger id;

        public StorageArea(AtomicInteger id) {
            this.id = id;
        }
        public int getId() {
            return id.get();
        }

        public Map<Integer, CachebleObj> getAreaData() {
            return areaData;
        }

        public List<ServerClient> getClientsInThisArea() {
            return clientsInThisArea;
        }

        public void addClientToRoom (ServerClient client) {
            if(clientsInThisArea.contains(client)) {
                System.out.println(client.getId() + " is already in this room");
                return;
            }
            clientsInThisArea.add(client);
        }

        public void removeClientFromRoom (ServerClient client) {
            if(clientsInThisArea.contains(client)) {
                clientsInThisArea.remove(client);
                System.out.println(client.getId() + " is removed from this room");
            } else {
                System.out.println(client.getId() + " cannot be found in this room");
            }
        }

        public void addObjectToRoomData (CachebleObj objectToPut){
            if(areaData.containsValue(objectToPut)){
                System.out.println("Already exists!");
                return;
            }
            areaData.put(objectToPut.getId(), objectToPut);


        }

        public CachebleObj getObject(String name){
            for(Map.Entry<Integer, CachebleObj> singleObject: areaData.entrySet()) {
                if(singleObject.getValue().getObjectName().equals(name)){
                    return singleObject.getValue();
                }
            }
            return null;

        }


    }

    class ClientThread extends Thread{
        Socket clientSocket ;
        ObjectInputStream fromClient;
        ObjectOutputStream toClient;
        ServiceProviderStorage service;

        ServerClient theClient;

        public ClientThread (Socket clientSocket, ServiceProviderStorage service) {
            this.clientSocket = clientSocket;
            this.service = service;

            try {
                fromClient = new ObjectInputStream(clientSocket.getInputStream());
                toClient = new ObjectOutputStream(clientSocket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void setService (ServiceProviderStorage newService) {
            service = newService;
        }


        public void run() {
            try{
                System.out.println("\n\nReady round");

                while (true) {


                    String whatToDo = (String) fromClient.readObject();
                    String[] analyseMsg = whatToDo.split(":");
                    System.out.println("Msg: " + whatToDo);


                    if (analyseMsg.length < 1 || analyseMsg.length > 3) {
                        System.out.println("Wrong message length");
                        toClient.writeObject("Error! Could not understand the command");
                    } else {
                        int clientID = Integer.parseInt(analyseMsg[1]);
                        ServerClient serverClient = service.findClient(clientID);

                        switch (analyseMsg[0]) {
                            case "1":
                                System.out.println("Choosing to add a new client to a new room");

                                if (clientID == 0) {
                                    ServerClient sc = new ServerClient(returnId(), clientSocket.getInetAddress());
                                    theClient = sc;
                                    StorageArea storageArea = new StorageArea(returnAreaId());
                                    fillChatRoomData(storageArea);

                                    addClient(sc, storageArea);
                                    System.out.println("New user, create new ID:" + sc.getId());

                                    toClient.writeObject("1:" + sc.getId());
                                } else {
                                    System.out.println("User already exist, returning area contect instead. ClientID: " + clientID);
                                    theClient = findClient(clientID);
                                    List<String> getObjectsInRoom = getObject(listOfObservers.get(clientID));
                                    toClient.writeObject(getObjectsInRoom);
                                }

                                break;

                            case "2":
                                //System.out.println("Adding a new client to existing room");

                                int roomAreaID = Integer.parseInt(analyseMsg[2]);
                                StorageArea room = (StorageArea) service.listOfAreas.get(roomAreaID);
                                if (room == null) {
                                    System.out.println("Room " + roomAreaID + " could not be found");
                                    toClient.writeObject("Could not find room");
                                    break;
                                }
                                ServerClient client = findClient(clientID);

                                if (serverClient == null) {
                                    if(clientID == 0) {
                                        client = new ServerClient(returnId(), clientSocket.getInetAddress());
                                    }
                                }
                                service.addClient(client, room);
                                theClient = client;
                                toClient.writeObject("2:"+client.getId()+":"+room.getId());
                                toClient.writeObject("Client added to room:" + room.getId());
                                break;

                            case "3":
                                //System.out.println("Get a list of products available");
                                //System.out.println("clientID: " + clientID);
                                StorageArea storageArea = listOfObservers.get(findClient(clientID));

                                List<String> getObjectsInRoom = getObject(storageArea);
                                toClient.writeObject("3");
                                toClient.writeObject(getObjectsInRoom);
                                break;

                            case "4":
                                //System.out.println("Get the content of a file or object");
                                String singleObject = analyseMsg[2];
                                CachebleObj returnObj = getSingleObject(serverClient, singleObject);
                                toClient.writeObject("4");
                                toClient.writeObject(returnObj);
                                break;

                            case "5":
                                //System.out.println("Adding objects");

                                //oClient.writeObject("Server respond!" + whatToDo);
                                CachebleObj objectToPut = new CachebleObj("", null, 0);
                                objectToPut.readObject(fromClient);
                                objectToPut.setId(getObjectId().get());
                                putObject(serverClient, objectToPut);
                                break;

                            case "6":
                                //System.out.println("Unsubscribe from service");
                                toClient.writeObject(removeClient(serverClient));
                                break;


                            default:
                                System.out.println("Default");
                                break;
                        }
                    }

                }
            } catch (IOException e) {
                System.out.println("Client went down");
                removeClientSocket(this);

            } catch (ClassNotFoundException e) {
                System.out.println("Could not find proper type");

            } catch (NullPointerException e) {
                System.out.println("Client went down");
            }
        }

        public ServerClient getTheClient() {
            return theClient;
        }
    }
}
*/