import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.net.InetAddress;



public class ServiceProviderChat  extends ServiceInterface {
    ReentrantLock lock ;
    Map<Integer, ServiceObject> listOfRooms ;

    Map<ServerClient, ServiceObject> listOfObservers ;

    List<ServiceProviderChat.ClientThread> clientThreads ;


    int portNr ;

    AtomicInteger id ;

    AtomicInteger chatRoomId ;

    AtomicInteger objectId ;

    public ServiceProviderChat(){
        lock = new ReentrantLock();
        listOfRooms = new HashMap<Integer, ServiceObject>();
        listOfObservers = new HashMap<ServerClient, ServiceObject>();

        clientThreads = new ArrayList<>();


        portNr = 3213;


        System.out.println("Port nr: " + portNr);

        id = new AtomicInteger();
        chatRoomId = new AtomicInteger();
        objectId = new AtomicInteger();
    }




    public static void main(String[] args) {
        new ServiceProviderChat().startServer();
    }

    public void startServer() {
        System.out.println("Starting server");
        System.setProperty("java.net.preferIPv4Stack", "true");

        try ( ServerSocket serverSocket = new ServerSocket(portNr) ) {
            System.out.println("Open on IP: " + InetAddress.getLocalHost() + " - port: " + portNr);


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

    public AtomicInteger returnChatRoomId() {
        lock.lock();
        try {
            chatRoomId = new AtomicInteger(chatRoomId.get()+1);
            return chatRoomId;
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

    @Override
    public Map<Integer, ServiceObject> getListOfRooms() {
        return listOfRooms;
    }

    @Override
    public Map<ServerClient, ServiceObject> getListOfObservers() {
        return listOfObservers;
    }

    @Override
    public void setListOfRooms(Map<Integer, ServiceObject> listOfRooms) {
        this.listOfRooms = listOfRooms;
    }

    @Override
    public void setListOfObservers(Map<ServerClient, ServiceObject> listOfObservers) {
        this.listOfObservers = listOfObservers;
    }



    //getObject recieves a key from parameter and returns the object from
    //Whether the object gets returned or not depends on size and network connection type



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



    //If the time of object on our caching system has expired, then we will
    //remove it from our caching system.


    public void debugPrint(){
        /*for(Map.Entry<Integer, CachebleObj> pair: listOfRooms.entrySet()) {
            System.out.println(pair.getKey()+ " - " + pair.getValue().getNa());
        }*/
    }


    public void syncPutObject(ServerClient client, CachebleObj obj) {
        lock.lock();
        try{

            putObject (client, obj, this);
        } finally {
            lock.unlock();
        }
    }

    public void syncRemoveObject (Map.Entry<Integer, ServiceObject> pair, ServerClient cl) {
        lock.lock();
        try {
            removeObject(pair, cl, this);
        } finally {
            lock.unlock();
        }
    }

    public void syncAddClient (ServerClient newUser, ServiceObject area) {

        lock.lock();
        try {
            addClient(newUser, area, this);
        } finally {
            lock.unlock();

        }
    }

    public String syncRemoveClient(ServerClient removeUser) {
        lock.lock();

        try{
            return removeClient(removeUser, this);
        } finally {
            lock.unlock();
        }
    }


    //Removing client from the list of observers

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
    }

    //Going through the user of the current service provider and notify them
    public void notifyUsers(ServerClient cl, CachebleObj obj) {

        System.out.println("CAlling notification");

        ServiceObject cr = listOfObservers.get(cl);
        try {
            for (ServiceProviderChat.ClientThread ct : clientThreads) {
                if (listOfObservers.get(ct.getTheClient()) == cr && ct.theClient != cl) {
                    ct.toClient.writeObject(obj.getObjectName());
                }
            }
        } catch (IOException e){

        }
    }


    /****************************************************************************************************************
     * **************************************************************************************************************
     * **************************************************************************************************************
     * **************************************************************************************************************
     */



    class ChatRoom implements ServiceObject{

        Map<Integer, CachebleObj> roomData= new HashMap<Integer, CachebleObj>();

        List<ServerClient> clientsInThisRoom = new ArrayList<ServerClient>();

        private AtomicInteger id;

        public ChatRoom (AtomicInteger id) {
            this.id = id;
        }
        public int getId() {
            return id.get();
        }

        public Map<Integer, CachebleObj> getAreaData() {
            return roomData;
        }

        public List<ServerClient> getClientsInThisArea() {
            return clientsInThisRoom;
        }

        public void addClientToRoom (ServerClient client) {
            if(clientsInThisRoom.contains(client)) {
                System.out.println(client.getId() + " is already in this room");
                return;
            }
            clientsInThisRoom.add(client);
        }

        public void removeClientFromRoom (ServerClient client) {
            if(clientsInThisRoom.contains(client)) {
                clientsInThisRoom.remove(client);
                System.out.println(client.getId() + " is removed from this room");
            } else {
                System.out.println(client.getId() + " cannot be found in this room");
            }
        }

        public void addObjectToRoomData (CachebleObj objectToPut){
            if(roomData.containsValue(objectToPut)){
                System.out.println("Already exists!");
                return;
            }
            roomData.put(objectToPut.getId(), objectToPut);


        }

        public CachebleObj getObject(String name){
            for(Map.Entry<Integer, CachebleObj> singleObject: roomData.entrySet()) {
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
        ServiceInterface service;

        ServerClient theClient;

        private int clientID;

        public ClientThread (Socket clientSocket, ServiceInterface service) {
            this.clientSocket = clientSocket;
            this.service = service;
            clientID = 0;

            try {
                fromClient = new ObjectInputStream(clientSocket.getInputStream());
                toClient = new ObjectOutputStream(clientSocket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void setService (ServiceProviderChat newService) {
            service = newService;
        }


        public void run() {
            try{
                while (true) {
                    String whatToDo = (String) fromClient.readObject();
                    String[] analyseMsg = whatToDo.split(":");

                    if (analyseMsg.length < 1 || analyseMsg.length > 3) {
                        toClient.writeObject("Error! Could not understand the command");
                        continue;
                    }

                    clientID = Integer.parseInt(analyseMsg[1]);

                    switch (analyseMsg[0]) {
                        case "1":
                            addNewClientInNewLocalRoom();
                            break;

                        case "2":
                            addNewClientInExistingLocalRoom (theClient, analyseMsg);
                            break;

                        case "3":
                            getLocalObjectList();
                            break;

                        case "4":
                            getLocalObject(analyseMsg, theClient);
                            break;

                        case "5":
                            addLocalObject (theClient);
                            break;

                        case "6":
                            toClient.writeObject(syncRemoveClient(theClient));
                            break;

                        default:
                            System.out.println("Default");
                            break;
                    }
                }
            } catch (IOException e) {
                System.out.println("UIExceptio");
                removeClientSocket(this);
            } catch (ClassNotFoundException e) {
                System.out.println("ClassNotfound");
            } catch (NullPointerException e) {
                
            }
        }

        public ServerClient getTheClient() {
            return theClient;
        }

        void addNewClientInNewLocalRoom () {
            System.out.println("Choosing to add a new client to a new room");

            try {
                if (clientID == 0) {
                    ServerClient sc = new ServerClient(returnId(), clientSocket.getInetAddress());
                    theClient = sc;
                    ChatRoom storageArea = new ChatRoom(returnChatRoomId());

                    syncAddClient(sc, storageArea);
                    System.out.println("New user, create new ID:" + sc.getId());

                    toClient.writeObject("1:" + sc.getId());
                } else {
                    System.out.println("User already exist, returning area contect instead. ClientID: " + clientID);
                    theClient = findClient(clientID);
                    List<String> getObjectsInRoom = getObject( listOfObservers.get(clientID));
                    toClient.writeObject(getObjectsInRoom);
                }
            } catch (IOException e){

            }

        }

        void addNewClientInExistingLocalRoom (ServerClient serverClient, String[] analyseMsg) {
            int roomAreaID = Integer.parseInt(analyseMsg[2]);
            ChatRoom room = (ChatRoom) service.getListOfRooms().get(roomAreaID);
            try {

                if (room == null) {
                    System.out.println("Room " + roomAreaID + " could not be found");
                    toClient.writeObject("Could not find room");
                    return;
                }
                ServerClient client = findClient(clientID);

                if (serverClient == null) {
                    if(clientID == 0) {
                        client = new ServerClient(returnId(), clientSocket.getInetAddress());
                    }
                }
                syncAddClient(client, room);
                theClient = client;
                toClient.writeObject("2:"+client.getId()+":"+room.getId());
                toClient.writeObject("Client added to room:" + room.getId());
            } catch (IOException e) {

            }
        }

        void getLocalObjectList (){
            try {

                ServiceObject chatRoom = listOfObservers.get(findClient(clientID));

                List<String> getObjectsInRoom = getObject(chatRoom);
                toClient.writeObject("3");
                toClient.writeObject(getObjectsInRoom);
            } catch (IOException e) {

            }
        }

        void getLocalObject(String[]  analyseMsg, ServerClient serverClient) {

            try{

                String singleObject = analyseMsg[2];
                CachebleObj returnObj = getSingleObject(serverClient, singleObject);
                toClient.writeObject("4");
                toClient.writeObject(returnObj);
            } catch (IOException e ){

            }
        }

        void addLocalObject (ServerClient serverClient) {
            try {

                CachebleObj objectToPut = new CachebleObj("", null, 0);
                objectToPut.readObject(fromClient);
                objectToPut.setId(getObjectId().get());
                syncPutObject(serverClient, objectToPut);
            } catch (IOException e) {

            }
        }
    }
}
