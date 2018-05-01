

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

public class StorageService implements ServiceInterface {



    private Map<Integer, ServiceObject> listOfAreas ;
    private Map<ServerClient, StorageArea> listOfObservers;

    private List<ClientThread> clientThreads;


    private int portNr ;



    boolean open ;


    private AtomicInteger id ;
    private AtomicInteger areaId ;
    private AtomicInteger objectId ;


    public StorageService(){
        listOfAreas = new HashMap<>();
        listOfObservers= new HashMap<>();
        clientThreads = new ArrayList<>();

        portNr = 1254;



        id = new AtomicInteger();
        areaId = new AtomicInteger();
        objectId = new AtomicInteger();
    }

    public static void main(String[] args) {
        new StorageService().startServer();
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

    /**
     *
     * Getting a list of all the names in the clients area
     * @param serviceObject     The area which the data is stored at
     * @return                  List of all the names of the clients area
     */
    public List<String> getObjectList(ServiceObject serviceObject){

            if (serviceObject == null) {
                System.out.println("Could not find room!");
                return null;
            }


            System.out.println("Found the room and returning it");
            List<String> namesToList = serviceObject.getAreaData().values().stream().map(i -> i.getObjectName()).collect(Collectors.toList());

            return namesToList;


    }


    /**
     * Receiving a single object, like a file in this case
     * @param client    Which client as asked for it, using it to find room.
     * @param name      Name of the file to return
     * @return          The file that the client has asked for
     */
    public DataObject getSingleObject(ServerClient client,String name){

        return listOfObservers.get(client).getObject(name);

    }

    /**
     * Adding object from the client to the users service area
     * @param client    The client that wants to add object
     * @param obj       The object to add
     */
    public void putObject(ServerClient client, DataObject obj) {
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

    /**
     * Removing an object from the clients private area.
     * @param cl        The client that wants to remove an object from its area
     * @param fileName  The name of the file that the client wants to remove
     * @return
     */
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


    /**
     * Adding client to a service area
     * @param newUser   The client that is going to be added
     * @param area      The area where the client is going to be added
     */
    public void addClient(ServerClient newUser, ServiceObject area) {
            if(newUser == null || area == null){
                System.out.println("Not sufficient information to go from to add new user");
                return;
            }
            listOfAreas.put(area.getId(), area);
            listOfObservers.put(newUser, (StorageArea) area);
            System.out.println("Client with id: "+newUser.getId() + " added to area: " + area.getId());

    }

    /**
     * Removing client from the service area
     * @param removeUser   The client to remove
     * @param ct            The client thread to remove from its area
     * @return              Return whether the client got removed or not.
     */
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

    /**
     * Finding client based on client ID
     * @param clientID  The client ID of the client that needs to be found
     * @return          The client that the client ID belongs to
     */
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


        Map<Integer, DataObject> areaData = new HashMap<>();
        ServerClient serverClient;

        private AtomicInteger id;

        public StorageArea(AtomicInteger id) {
            this.id = id;
            lock = new ReentrantLock();
        }
        public int getId() {
            return id.get();
        }

        public Map<Integer, DataObject> getAreaData() {
            return areaData;
        }

        public ServerClient getClientsInThisArea() {
            return serverClient;
        }

        /**
         * Adding client to area. If the area already has a client, then just return without adding
         * @param client      Client that is going to be added
         */
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


        /**
         * Removing client from service
         * @param client    The client that is going to be removed
         */
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


        /**
         * Adding object to area, in this service, file to clients private area
         * @param objectToPut   The object that is going to be added to the area
         */
        public void addObjectToRoomData (DataObject objectToPut){
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

        /**
         * After an object is added, the HashMap gets sorted
         */
        void sortArea(){
            List<DataObject> li = new ArrayList<>(areaData.values());
            Collections.sort(li, new Comparator<DataObject>() {
                @Override
                public int compare(DataObject t1, DataObject t2) {
                    return t1.getLatestAccessedTime().compareTo(t2.getLatestAccessedTime());
                }
            });

            areaData = li.stream().collect(Collectors.toMap(DataObject::getId, it -> it));
        }

        /**
         * Get the object that the client has asked for
         * @param name  Name of the object that the client has asked for
         * @return      The object that the client asked for
         */
        public DataObject getObject(String name){
            DataObject DataObject =null;
            lock.lock();
            try {
                for(Map.Entry<Integer, DataObject> singleObject: areaData.entrySet()) {
                    if(singleObject.getValue().getObjectName().equals(name)){
                        DataObject =  singleObject.getValue();
                        break;
                    }
                }
                sortArea();
                return DataObject;

            } finally {
                lock.unlock();
            }

        }


        /**
         * Remove object from client's area
         * @param name  Name of the object that is going to be removed
         * @return      Return an integer if the object gets removed or not.
         */
        public int removeObject (String name) {
            lock.lock();
            int tmpID=-1;

            try{
                for(Map.Entry<Integer, DataObject> pair : areaData.entrySet()) {
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
        StorageService service;

        int clientID;

        boolean open = true;

        ServerClient theClient;

        public ClientThread (Socket clientSocket, StorageService service) {
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

        public void setService (StorageService newService) {
            service = newService;
        }

        /**
         * Receiving the instruction that the client wants to be added to the service
         * @throws IOException  Incase something goes wrong with sending message back to client
         */
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

        /**
         * Recieving an instruction from client to get list of object names in area
         * @throws IOException
         */
        void getLocalObjectList () throws IOException{

            ServiceObject chatRoom = listOfObservers.get(findClient(clientID));

            List<String> getObjectsInRoom = getObjectList(chatRoom);
            toClient.writeObject("3");
            toClient.writeObject(getObjectsInRoom);

        }

        /**
         * Get a single object that the client has asked for
         * @param analyseMsg        Splitting message sent from client for finding the name of object
         * @param serverClient      Sending it to find the client
         * @throws IOException
         */
        void getLocalObject(String[]  analyseMsg, ServerClient serverClient) throws IOException{

            String singleObject = analyseMsg[2];
            DataObject returnObj = getSingleObject(serverClient, singleObject);
            returnObj.setLatestAccessTime();
            toClient.writeObject("4");
            returnObj.sendObject(toClient);

        }

        /**
         * Recieving an instruction from client to add an object to its area
         * @param serverClient      Client that has sent the message and uses this to find the client's area
         * @throws IOException
         */
        void addLocalObject (ServerClient serverClient) throws IOException{
            DataObject objectToPut = new DataObject("", null);
            objectToPut.readObject(fromClient);
            objectToPut.setId(getObjectId().get());
            putObject(serverClient, objectToPut);
        }

        /**
         * Removing an object from client's area
         * @param fileN     Name of the file that is going to be removed
         * @throws IOException
         */
        void removeObjectCmd (String fileN) throws IOException{
            if (removeObject(theClient, fileN).equals("success")) {
                toClient.writeObject("7:"+fileN);
            } else {
                toClient.writeObject("Error while removing a file");
            }
        }

        /**
         * Handling communication with the client, both receiving and sending
         */
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
                System.err.println(e);
            }
        }

        public ServerClient getTheClient() {
            return theClient;
        }
    }
}
