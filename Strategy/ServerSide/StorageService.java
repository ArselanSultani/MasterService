
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

public class StorageService extends StrategyServiceAbs {

    private static StorageService thisStorage = null;

    private Map<Integer, ServiceObject> listOfAreas ;
    private Map<ServerClient, ServiceObject> listOfObservers ;

    private List<ClientThread> clientThreads ;
    private boolean openService ;

    private int portNr;

    private AtomicInteger id ;
    private AtomicInteger areaId ;
    private AtomicInteger objectId ;

    /*public static void main(String[] args) {
        new StorageService().startServer();
    }*/

    private StorageService () {
        listOfAreas = new HashMap<>();
        listOfObservers = new HashMap<>();
        clientThreads = new ArrayList<>();
        openService = true;

        portNr = 3213;

        id = new AtomicInteger();
        areaId = new AtomicInteger();
        objectId = new AtomicInteger();
    }

    static StorageService getStorageService () {
        if(thisStorage == null ) {
            thisStorage = new StorageService();
        }

        return thisStorage;
    }




    public void startService(Socket clientSocket, ObjectInputStream fromClient, ObjectOutputStream toClient) {
        System.out.println("Starting server");
        System.setProperty("java.net.preferIPv4Stack", "true");

        ClientThread clientThread = new ClientThread(clientSocket, this, fromClient, toClient);
        clientThreads.add(clientThread);
        clientThread.start();
    }

    public int returnId(){
        id = new AtomicInteger(id.get() +1);
        return id.get();
    }

    public AtomicInteger returnAreaId() {

            areaId = new AtomicInteger(areaId.get()+1);
            return areaId;

    }

    public AtomicInteger getObjectId() {
            objectId = new AtomicInteger(objectId.get()+1);
            return objectId;

    }

    @Override
    public void setListOfLocal(Map<Integer, ServiceObject> listOfAreas) {
            this.listOfAreas = listOfAreas;

    }

    @Override
    public void setListOfObservers(Map<ServerClient, ServiceObject> listOfObservers){
            this.listOfObservers = listOfObservers;

    }

    @Override
    public Map<Integer, ServiceObject> getListOfLocal(){
        return listOfAreas;
    }

    @Override
    public Map<ServerClient, ServiceObject> getListOfObservers() {
        return listOfObservers;
    }


    @Override
    public void addClient(ServerClient newUser, ServiceObject serviceArea ) {
            serviceArea.addClientToLocal(newUser);
            if(listOfAreas.get(serviceArea.getId()) == null) {
                listOfAreas.put(serviceArea.getId(), serviceArea);
            }
            listOfObservers.put(newUser, serviceArea);


    }


    /**
     * Getting a single file from StorageArea
     * @param client    The client that wants to retrieve something
     * @param name      The name of the file that is wanted
     * @return          Return the object that is wanted
     */
    public DataObject getSingleObject(ServerClient client, String name){
        StorageArea ar = (StorageArea) listOfObservers.get(client);
        return ar.getObjectLocal(name);
    }


    /**
     * Removing an object from StorageArea
     * @param pair          The object that is going to  be removed
     * @param provider      The provider from which the service is at.
     */
    public void removeObject(Map.Entry<Integer, ServiceObject> pair, StorageService provider){
        Map<Integer, ServiceObject> listOfRooms = provider.getListOfLocal();
        listOfRooms.remove(pair.getKey(), pair.getValue());
        provider.setListOfLocal(listOfRooms);
    }


    /**
     * Finding an area without a client, in case the system has failed to remove it
     * @return
     */
    ServiceObject findEmptyArea() {
            for(Map.Entry<Integer, ServiceObject> room: listOfAreas.entrySet()) {
                if(room.getValue().getClientsInThisLocal().size() == 0) {
                    return room.getValue();
                }
            }

        return null;
    }


    public void removeClientSocket (ClientThread clientToRemove) {
        clientThreads.remove(clientToRemove);
    }




    class StorageArea implements ServiceObject{

        ReentrantLock lock = new ReentrantLock();
        Map<Integer, DataObject> areaData = new HashMap<>();

        ServerClient areaClient;

        List<ServerClient> clientsInThisArea = new ArrayList<>();

        private AtomicInteger id;

        public StorageArea(AtomicInteger id) {
            this.id = id;
        }
        public int getId() {
            return id.get();
        }

        public Map<Integer, DataObject> getLocalData() {
            return areaData;
        }

        public List<ServerClient> getClientsInThisLocal(){
            return clientsInThisArea;
        }

        /**
         * Adding client to Area
         * @param client    The client that is added and is now owner of the area
         */
        public void addClientToLocal (ServerClient client) {
            lock.lock();
            try{

                if(areaClient != null) {
                    System.out.println("There are already client in this room");
                    return;
                }

                areaClient = client;
                clientsInThisArea.add(areaClient);
            } finally {
                lock.unlock();
            }
        }

        /**
         * Removing client from area
         * @param client     The client that is removed
         */
        public void removeClientFromLocal (ServerClient client) {
            lock.lock();
            try{

                if(clientsInThisArea.contains(client)) {
                    clientsInThisArea.remove(client);
                    System.out.println(client.getId() + " is removed from this room");
                } else {
                    System.out.println(client.getId() + " cannot be found in this room");
                }
            } finally {
                lock.unlock();
            }
        }

        /**
         * Adding file to area
         * @param objectToPut   File to add in storagearea
         */
        public void addObjectToLocalData (DataObject objectToPut){
            lock.lock();

            try{

                if(areaData.containsValue(objectToPut)){
                    System.out.println("Already exists!");
                    return;
                }
                areaData.put(objectToPut.getId(), objectToPut);
            } finally {
                lock.unlock();
            }


        }

        /**
         * Get file from object.
         * @param name      Name of the file that is getting retrieved
         * @return          return the file
         */
        public DataObject getObjectLocal(String name){

            lock.lock();
            try{

                for(Map.Entry<Integer, DataObject> singleObject: areaData.entrySet()) {
                    if(singleObject.getValue().getObjectName().equals(name)){
                        return singleObject.getValue();
                    }
                }
                return null;
            } finally {
                lock.unlock();
            }

        }


    }

    class ClientThread extends Thread{
        private Socket clientSocket ;
        private ObjectInputStream fromClient;
        private ObjectOutputStream toClient;
        private StorageService service;



        boolean openStorageClientThread;

        private ServerClient theClient;
        private int clientId;

        public ClientThread (Socket clientSocket, StorageService service, ObjectInputStream fromClient, ObjectOutputStream toClient) {
            this.clientSocket = clientSocket;
            this.service = service;
            this.fromClient = fromClient;
            this.toClient = toClient;

            clientId = 0;

            openStorageClientThread = true;
        }

        public void setService (StorageService newService) {
            service = newService;
        }


        public ServerClient getTheClient() {
            return theClient;
        }


        @Override
        public void run() {
            try {
                while (openStorageClientThread) {
                    openStorageClientThread = true;
                    String instruction = (String) fromClient.readObject();
                    String[] inSplit = instruction.split(":");

                    if(inSplit.length < 1 || inSplit.length > 3) {
                        toClient.writeObject("Error! Could not understand the command");
                        continue;
                    }
                    clientId = Integer.parseInt(inSplit[1]);

                    switch (inSplit[0]){
                        case "1":
                        case "2":
                            addClientToNewRoom();
                            break;

                        case "3":
                            getAreaContents();
                            break;

                        case "4":
                            getSingleContent(inSplit);
                            break;

                        case "5":
                            addFileInArea();
                            break;

                        case "6":
                            toClient.writeObject(removeClient(theClient, service));
                            removeClientSocket(this);
                            openStorageClientThread = false;
                            break;

                        default:
                            break;

                    }
                }
            } catch (IOException|ClassNotFoundException e) {
                System.err.println(e);
            }
        }

        /**
         * Adding client to a new room
         */
        void addClientToNewRoom () {
            try {
                theClient = new ServerClient(returnId(), clientSocket.getInetAddress());
                StorageArea storageArea = new StorageArea(returnAreaId());

                addClient(theClient, storageArea);

                System.out.println("New used with id: "+ theClient.getId() + " on area: " + storageArea.getId());
                toClient.writeObject("1:" + theClient.getId());

            } catch (IOException e) {
                System.err.println(e);
            }
        }

        /**
         * Getting area content, name of all the files and sending to client
         */
        void getAreaContents() {
            try{
                ServiceObject areaToCollect = listOfObservers.get(theClient);
                List<String> contents = getObjectList(areaToCollect);

                toClient.writeObject("3");
                toClient.writeObject(contents);
            } catch (IOException e) {
                System.err.println(e);
            }
        }

        /**
         * Getting a file from service area
         * @param instructions      The instruction that is needed for decoding for finding the name of the file.
         */
        void getSingleContent (String[] instructions) {
            String fileName = instructions[2];
            DataObject getObjectVar = getSingleObject(theClient, fileName);
            try {
                toClient.writeObject("4");
                getObjectVar.sendObject(toClient);
            } catch (IOException e) {
                System.err.println(e);
            }
        }

        /**
         * adding file in area.
         * @throws IOException
         */
        void addFileInArea() throws IOException{
            DataObject newObject = new DataObject("",null);
            newObject.readObject(fromClient);
            newObject.setId(getObjectId().get());
            putObject(theClient, newObject,service);

        }

    }
}
