

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class ServiceProviderStorage  implements ServiceInterface {



    private Map<Integer, ServiceObject> listOfAreas ;
    private Map<ServerClient, StorageArea> listOfObservers;

    private List<ClientThread> clientThreads;


    private int portNr ;



    boolean open ;


    private AtomicInteger id ;
    private AtomicInteger areaId ;
    private AtomicInteger objectId ;


    public ServiceProviderStorage(){
        listOfAreas = new HashMap<>();
        listOfObservers= new HashMap<>();
        clientThreads = new ArrayList<>();

        portNr = 1254;



        id = new AtomicInteger();
        areaId = new AtomicInteger();
        objectId = new AtomicInteger();
    }

    public static void main(String[] args) {
        new ServiceProviderStorage().startServer();
    }

    public void startServer() {
        open = true;

        try ( ServerSocket serverSocket = new ServerSocket(portNr) ) {
            System.out.println("Open on: " + InetAddress.getLocalHost() + " and port#: " + portNr);


            while(open) {

                Socket clientSocket = serverSocket.accept();
                System.out.println("A connection has been established: " + clientSocket.getInetAddress());

                ClientThread clientThread = new ClientThread(clientSocket, this);
                clientThreads.add(clientThread);
                clientThread.start();


                Thread.sleep(2000);
            }
        } catch (IOException e) {
            System.err.println(e);
        } catch (InterruptedException e) {
            System.err.println(e);
            Thread.currentThread().interrupt();
        }
        open = false;

    }

    public int returnId(){
        id = new AtomicInteger(id.get() + 1);
        return id.get();
    }

    public AtomicInteger returnAreaId() {
            areaId = new AtomicInteger(areaId.get() + 1);
            return areaId;

    }


    public AtomicInteger getObjectId() {
    objectId = new AtomicInteger(objectId.get()+1);
            return objectId;


    }

    //getObject recieves a key from parameter and returns the object from
    //Whether the object gets returned or not depends on size and network connection type
    public List<String> getObjectList(ServiceObject serviceObject){

            if (serviceObject == null) {
                System.out.println("Could not find room!");
                return null;
            }


            System.out.println("Found the room and returning it");
            List<String> namesToList = serviceObject.getAreaData().values().stream().map(i -> i.getObjectName()).collect(Collectors.toList());

            return namesToList;


    }


    //Here, the single object gets returned, like for example a single file in  a
    public CachebleObj getSingleObject(ServerClient client,String name){

        return listOfObservers.get(client).getObject(name);

    }

    //Here we store a data in our caching system.

    public void putObject(ServerClient client, CachebleObj obj) {
            if (obj == null) {
                System.out.println("No object to add");
                return;
            }

            StorageArea storageArea = listOfObservers.get(client);

            if (storageArea == null) {
                System.out.println("User is not subscribed to this service");
                return;
            }

            storageArea.addObjectToRoomData(obj);


    }

    //If the time of object on our caching system has expired, then we will
    //remove it from our caching system.
    public String removeObject(ServerClient cl, String fileName){
        StorageArea area = (StorageArea)listOfObservers.get(cl);
        if(area == null) {
            return "Could not find room";
        }

        if(area.removeObject(fileName) != -1 ) {
            return "Could not find file";
        }

        return "success";
    }



    //Adding client to the list of observers
    public void addClient(ServerClient newUser, ServiceObject area) {
            if(newUser == null || area == null){
                System.out.println("Not sufficient information to go from to add new user");
                return;
            }
            listOfAreas.put(area.getId(), area);
            listOfObservers.put(newUser, (StorageArea) area);
            System.out.println("Client with id: "+newUser.getId() + " added to area: " + area.getId());

    }

    //Removing client from the list of observers
    public String removeClient (ServerClient removeUser, Thread ct){

            StorageArea storageArea = listOfObservers.get(removeUser);
            if(storageArea == null) {
                System.out.println("Could not find the user.");
                return "Could not find the user";
            }

            storageArea.removeClientFromRoom(removeUser);
            listOfObservers.remove(removeUser, storageArea);
            listOfAreas.remove(storageArea.getId());
            removeClientSocket((ClientThread) ct);
            return "User and room is removed ";


    }

    public ServerClient findClient (int clientID) {
        for (ServerClient serverClient : listOfObservers.keySet()) {
                if (clientID == serverClient.getId()) {
                    return serverClient;
                }
            }
            return null;
    }

    public void removeClientSocket (ClientThread clientToRemove) {
        System.out.println("Removed client si");
        clientThreads.remove(clientToRemove);
    }




    class StorageArea implements ServiceObject{
        ReentrantLock lock = new ReentrantLock();


        Map<Integer, CachebleObj> areaData = new HashMap<>();
        ServerClient serverClient;

        private AtomicInteger id;

        public StorageArea(AtomicInteger id) {
            this.id = id;
            lock = new ReentrantLock();
        }
        public int getId() {
            return id.get();
        }

        public Map<Integer, CachebleObj> getAreaData() {
            return areaData;
        }

        public ServerClient getClientsInThisArea() {
            return serverClient;
        }

        public void addClientToRoom (ServerClient client) {
            lock.lock();
            try {
                if(serverClient == null) {
                    serverClient = client;
                } else {
                    System.out.println("This room is taken");
                }

            } finally {
                lock.unlock();
            }


        }

        public void removeClientFromRoom (ServerClient client) {
            lock.lock();
            try {
                if(serverClient == client) {
                    serverClient = null;
                } else {
                    System.out.println("This area does not belong to this users");
                }

            } finally {
                lock.unlock();
            }
        }

        public void addObjectToRoomData (CachebleObj objectToPut){
            lock.lock();
            try {
                if(areaData.containsValue(objectToPut)){
                    System.out.println("Already exists!");
                    return;
                }
                areaData.put(objectToPut.getId(), objectToPut);

            } finally {
                lock.unlock();
            }


        }

        public CachebleObj getObject(String name){
            lock.lock();
            try {
                for(Map.Entry<Integer, CachebleObj> singleObject: areaData.entrySet()) {
                    if(singleObject.getValue().getObjectName().equals(name)){
                        return singleObject.getValue();
                    }
                }
                return null;

            } finally {
                lock.unlock();
            }

        }

        public int removeObject (String name) {
            lock.lock();
            int tmpID=-1;

            try{
                for(Map.Entry<Integer, CachebleObj> pair : areaData.entrySet()) {
                    if (pair.getValue().getObjectName().toLowerCase().equals(name.toLowerCase()) ){
                        tmpID = pair.getValue().getId();
                        break;
                    }
                }
                if(tmpID!=-1){
                    areaData.remove(tmpID);
                } else {
                    System.out.println("Could find it");
                }
                return tmpID;
            } finally {
                lock.unlock();
            }
        }

    }

    class ClientThread extends Thread{
        Socket clientSocket ;
        ObjectInputStream fromClient;
        ObjectOutputStream toClient;
        ServiceProviderStorage service;

        int clientID;

        boolean open = true;

        ServerClient theClient;

        public ClientThread (Socket clientSocket, ServiceProviderStorage service) {
            this.clientSocket = clientSocket;
            this.service = service;
            clientID = 0;

            try {
                fromClient = new ObjectInputStream(clientSocket.getInputStream());
                toClient = new ObjectOutputStream(clientSocket.getOutputStream());
            } catch (IOException e) {
                System.err.println(e);
            }
        }

        public void setService (ServiceProviderStorage newService) {
            service = newService;
        }

        void addNewClientInNewLocalRoom() throws IOException{
            if (clientID == 0) {
                ServerClient sc = new ServerClient(returnId(), clientSocket.getInetAddress());
                theClient = sc;
                StorageArea area = new StorageArea(returnAreaId());

                addClient(sc, area);
                toClient.writeObject("1:" + sc.getId());


            } else {
                System.out.println("User already exist, returning area contect instead. ClientID: " + clientID);
                theClient = findClient(clientID);
                List<String> getObjectsInRoom = getObjectList(listOfObservers.get(clientID));
                toClient.writeObject(getObjectsInRoom);
            }

        }

        void getLocalObjectList () throws IOException{

            ServiceObject chatRoom = listOfObservers.get(findClient(clientID));

            List<String> getObjectsInRoom = getObjectList(chatRoom);
            toClient.writeObject("3");
            toClient.writeObject(getObjectsInRoom);

        }

        void getLocalObject(String[]  analyseMsg, ServerClient serverClient) throws IOException{

            String singleObject = analyseMsg[2];
            CachebleObj returnObj = getSingleObject(serverClient, singleObject);
            returnObj.setLatestAccessTime();
            toClient.writeObject("4");
            returnObj.sendObject(toClient);

        }

        void addLocalObject (ServerClient serverClient) throws IOException{
            CachebleObj objectToPut = new CachebleObj("", null);
            objectToPut.readObject(fromClient);
            objectToPut.setId(getObjectId().get());
            putObject(serverClient, objectToPut);
        }

        void removeObjectCmd (String fileN) throws IOException{
            if (removeObject(theClient, fileN).equals("success")) {
                toClient.writeObject("7:"+fileN);
            } else {
                toClient.writeObject("Error while removing a file");
            }
        }

        @Override
        public void run() {
            try{
                while (open) {
                    String whatToDo = (String) fromClient.readObject();
                    String[] analyseMsg = whatToDo.split(":");


                    if (analyseMsg.length < 1 || analyseMsg.length > 3) {
                        System.out.println("Wrong message length");
                        toClient.writeObject("Error! Could not understand the command");
                        continue;
                    }
                    clientID = Integer.parseInt(analyseMsg[1]);

                    switch (analyseMsg[0]) {
                        case "1":
                        case "2":
                            addNewClientInNewLocalRoom();
                            break;


                        case "3":
                            getLocalObjectList();
                            break;


                        case "4":
                            getLocalObject(analyseMsg, theClient);
                            break;

                        case "5":
                            addLocalObject(theClient);
                            break;

                        case "6":
                            toClient.writeObject(removeClient(theClient, this));
                            break;

                        case "7":


                        default:
                            System.out.println("Default");
                            break;
                    }


                }
            } catch (IOException e) {
                System.out.println("UIExceptio");
                removeClientSocket(this);

            } catch (ClassNotFoundException|NullPointerException e) {
                System.out.println("ClassNotfound");
            }
        }

        public ServerClient getTheClient() {
            return theClient;
        }
    }
}
