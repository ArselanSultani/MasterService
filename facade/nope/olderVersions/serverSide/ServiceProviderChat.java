
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;


public class ServiceProviderChat  extends ServiceInterface {

    Map<Integer, ServiceObject> listOfRooms ;
    Map<ServerClient, ServiceObject> listOfObservers ;
    List<ServiceProviderChat.ClientThread> clientThreads ;


    int portNr ;
    private ReentrantLock lock ;

    AtomicInteger id ;
    AtomicInteger chatRoomId ;
    AtomicInteger objectId ;

    ServerSocket serverSocket;

    boolean open;


    public ServiceProviderChat(){
        lock = new ReentrantLock();
        listOfRooms = new HashMap<>();
        listOfObservers = new HashMap<>();


        clientThreads = new ArrayList<>();


        portNr = 1254;
        System.out.println("Port nr: " + portNr);

        id = new AtomicInteger();
        chatRoomId = new AtomicInteger();
        objectId = new AtomicInteger();

        open = true;
    }




    public static void main(String[] args) {
        new ServiceProviderChat().startServer();
    }

    public void startServer() {
        System.out.println("Starting server");
        System.setProperty("java.net.preferIPv4Stack", "true");

        try ( ServerSocket serverSocket = new ServerSocket(portNr) ) {
            System.out.println("Open on IP: " + InetAddress.getLocalHost() + " - port: " + portNr);

            while(open) {

                Socket clientSocket = serverSocket.accept();
                System.out.println("A connection has been established: " + clientSocket.getInetAddress());

                ClientThread clientThread = new ClientThread(clientSocket, this);
                clientThreads.add(clientThread);
                clientThread.start();

                Thread.sleep(1000);
            }
        } catch (IOException|InterruptedException e) {
            System.err.println(e);
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

    public Map<Integer, ServiceObject> getListOfLocalObject() {
        return listOfRooms;
    }

    @Override
    public Map<ServerClient, ServiceObject> getListOfObservers() {
        return listOfObservers;
    }

    @Override
    public void setListOfLocalObject(Map<Integer, ServiceObject> listOfRooms) {
        this.listOfRooms = listOfRooms;
    }

    @Override
    public void setListOfObservers(Map<ServerClient, ServiceObject> listOfObservers) {
        this.listOfObservers = listOfObservers;
    }



    public CachebleObj getSingleObject(ServerClient client,String name){
        lock.lock();
        try {
            return listOfObservers.get(client).getObject(name);
        } finally {
            lock.unlock();
        }
    }



    public void syncPutObject(ServerClient client, CachebleObj obj) {
        lock.lock();
        try{

            putObject (client, obj, this);
        } finally {
            lock.unlock();
        }
    }

    public void syncRemoveObject (Map.Entry<Integer, ServiceObject> pair) {
        lock.lock();
        try {
            removeObject(pair, this);
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
        System.out.println("Removed client from service");
        clientThreads.remove(clientToRemove);

    }

    public void notifyUsers(ServerClient cl, CachebleObj obj) {


        ServiceObject cr = listOfObservers.get(cl);
        try {
            for (ServiceProviderChat.ClientThread ct : clientThreads) {
                if (listOfObservers.get(ct.getTheClient()) == cr && ct.theClient != cl) {
                    ct.toClient.writeObject(obj.getObjectName());
                }
            }
        } catch (IOException e){
            System.err.println(e);
        }
    }





    class ChatRoom implements ServiceObject{

        Map<Integer, CachebleObj> roomData= new HashMap<>();

        List<ServerClient> clientsInThisRoom = new ArrayList<>();

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
                System.err.println(client.getId() + " is already in this room");
                return;
            }
            clientsInThisRoom.add(client);
        }

        public void removeClientFromRoom (ServerClient client) {
            if(clientsInThisRoom.contains(client)) {
                clientsInThisRoom.remove(client);
                System.out.println(client.getId() + " is removed from this room");
            } else {
                System.err.println(client.getId() + " cannot be found in this room");
            }
        }

        public void addObjectToRoomData (CachebleObj objectToPut){
            if(roomData.containsValue(objectToPut)){
                System.err.println("Already exists!");
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
        private Socket clientSocket ;
        private ObjectInputStream fromClient;
        private ObjectOutputStream toClient;
        private ServiceProviderChat service;

        private boolean openClientThread = false;

        private ServerClient theClient;

        private int clientID;

        public ClientThread (Socket clientSocket, ServiceProviderChat service) {
            this.clientSocket = clientSocket;
            this.service = service;
            clientID = 0;

            openConnection();

        }

        public void setService (ServiceProviderChat newService) {
            service = newService;
        }

        public void openConnection() {
            try {
                fromClient = new ObjectInputStream(clientSocket.getInputStream());
                toClient = new ObjectOutputStream(clientSocket.getOutputStream());
                openClientThread = true;
            } catch (IOException e) {
                System.err.println(e);
            }
        }

        public void closeConnection() {
            try {
                fromClient.close();
                toClient.close();
            } catch (IOException e) {
                System.err.println(e);
            }
        }

        @Override
        public void run() {
            try{
                while (openClientThread) {
                    String whatToDo = (String) fromClient.readObject();
                    String[] analyseMsg = whatToDo.split(":");

                    if (analyseMsg.length < 1 || analyseMsg.length > 3) {
                        toClient.writeObject("Error! Could not understand the command");
                        continue;
                    }

                    switch (analyseMsg[0]) {
                        case "1":
                        case "2":
                            handlingClientAddition(analyseMsg);
                            break;

                        case "3":
                        case "4":
                            handleClientRequest(analyseMsg);
                            break;

                        case "5":
                            addLocalObject ();
                            break;

                        case "6":
                            toClient.writeObject(syncRemoveClient(theClient));
                            break;

                        default:
                            System.err.println("Default");
                            break;
                    }
                }
            } catch (IOException e) {
                System.err.println("UIExceptio");
                removeClientSocket(this);
            } catch (ClassNotFoundException|NullPointerException e) {
                System.err.println("ClassNotfound");
            } finally {
                closeConnection();
            }


        }

        public ServerClient getTheClient() {
            return theClient;
        }


        void handlingClientAddition(String[] msg){
            if(msg.length == 2) {
                addNewClientInNewLocalRoom();
            } else {
                addNewClientInExistingLocalRoom(msg);
            }
        }

        void handleClientRequest(String[] msg) {
            if(msg.length == 2) {
                getLocalObjectList();
            } else {
                getLocalObject(msg);
            }
        }

        void addNewClientInNewLocalRoom () {

            try {
                if (clientID == 0) {
                    theClient = new ServerClient(returnId(), clientSocket.getInetAddress());
                    ChatRoom chatRoom = new ChatRoom(returnChatRoomId());

                    syncAddClient(theClient, chatRoom);
                    System.out.println("New user, create new ID:" + theClient.getId() + " on room: " + chatRoom.getId());

                    toClient.writeObject("1:" + theClient.getId());
                } else {
                    System.out.println("User already exist, returning area contect instead. ClientID: " + clientID);
                    getLocalObjectList();
                }
            } catch (IOException e){
                System.err.println(e);
            }

        }

        void addNewClientInExistingLocalRoom ( String[] analyseMsg) {
            if (clientID == 0) {
                clientID = Integer.parseInt(analyseMsg[1]);
            }

            int roomAreaID = Integer.parseInt(analyseMsg[2]);
            ChatRoom room = (ChatRoom) service.getListOfLocalObject().get(roomAreaID);
            try {

                if (room == null) {
                    System.err.println("Room " + roomAreaID + " could not be found");
                    toClient.writeObject("Could not find room");
                    return;
                }
                ServerClient client = findClient(clientID);

                if (client == null) {
                    client = new ServerClient(returnId(), clientSocket.getInetAddress());
                }
                syncAddClient(client, room);
                theClient = client;
                toClient.writeObject("2:"+client.getId()+":"+room.getId());
                toClient.writeObject("Client added to room:" + room.getId());
            } catch (IOException e) {
                System.err.println(e);
            }
        }

        void getLocalObjectList (){
            try {

                ServiceObject chatRoom = listOfObservers.get(findClient(clientID));

                List<String> getObjectsInRoom = service.getObject(chatRoom);
                toClient.writeObject("3");
                toClient.writeObject(getObjectsInRoom);
            } catch (IOException e) {
                System.err.println(e);
            }
        }

        void getLocalObject(String[]  analyseMsg) {
            String singleObject = analyseMsg[2];
            CachebleObj returnObj = getSingleObject(theClient, singleObject);
            try{
                toClient.writeObject("4");
                returnObj.sendObject(toClient);
            } catch (IOException e ){
                System.err.println("Could not send object back to client:\n" + e);
            }
        }

        void addLocalObject () {
            try {
                CachebleObj objectToPut = new CachebleObj("", null, 0);
                objectToPut.readObject(fromClient);
                objectToPut.setId(getObjectId().get());
                syncPutObject(theClient, objectToPut);
            } catch (IOException e) {
                System.err.println(e);
            }
        }
    }
}
