
import javax.xml.ws.Service;
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

    private static ServiceProviderChat serviceProviderChat = null;


    Map<Integer, ServiceObject> listOfRooms ;
    Map<ServerClient, ServiceObject> listOfObservers ;
    List<ServiceProviderChat.ClientThread> clientThreads ;




    int portNr ;

    AtomicInteger id ;
    AtomicInteger chatRoomId ;
    AtomicInteger objectId ;

    ServerSocket serverSocket;

    boolean open;


    private ServiceProviderChat(){
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

    static public ServiceProviderChat getChatService () {
        if(serviceProviderChat == null ){
            serviceProviderChat = new ServiceProviderChat();
        }
        return serviceProviderChat;
    }


    public void startServer(Socket clientSocket, ObjectInputStream fromClient, ObjectOutputStream toClient) {
        System.out.println("Starting server");

        ClientThread clientThread = new ClientThread(clientSocket, this, fromClient, toClient);
        clientThreads.add(clientThread);
        clientThread.start();


        /*try ( ServerSocket serverSocket = new ServerSocket(portNr) ) {
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
        }*/

    }

    public int returnId(){

            id = new AtomicInteger(id.get() +1);

        return id.get();
    }

    public AtomicInteger returnChatRoomId() {
            chatRoomId = new AtomicInteger(chatRoomId.get()+1);
            return chatRoomId;

    }

    public AtomicInteger getObjectId() {
            objectId = new AtomicInteger(objectId.get()+1);
            return objectId;

    }

    @Override
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

    @Override
    public void addClient(ServerClient newUser, ServiceObject serviceArea ) {
            if(listOfObservers.get(newUser) != null) {
                System.out.println("User already subscribed");
                return;
            }

            //IF room already exists
            if(listOfRooms.get(serviceArea) != null) {
                listOfObservers.put(newUser,  serviceArea);
                serviceArea.addClientToLocal(newUser);
                return;
            }


            //if the room does not exist

            serviceArea.addClientToLocal(newUser);
            listOfRooms.put(serviceArea.getId(), serviceArea);
            listOfObservers.put(newUser, serviceArea);

            setListOfObservers(listOfObservers);
            setListOfLocalObject(listOfRooms);


            System.out.println("Added client " + newUser.getId()  + " to observer list");

    }

    @Override
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

    public CachebleObj getSingleObject(ServerClient client,String name){
            return listOfObservers.get(client).getObjectLocal(name);

    }




    public ServerClient findClient (int clientID) {

            for(ServerClient serverClient : listOfObservers.keySet()) {
                if (clientID == serverClient.getId()) {
                    return serverClient;
                }
            }
            return null;

    }

    public void removeClientSocket (ClientThread clientToRemove) {
        System.out.println("Removed client from service");
        clientThreads.remove(clientToRemove);

    }


    class ChatRoom implements ServiceObject{
        private ReentrantLock lock ;


        Map<Integer, CachebleObj> roomData= new HashMap<>();

        List<ServerClient> clientsInThisRoom = new ArrayList<>();

        private AtomicInteger id;

        public ChatRoom (AtomicInteger id) {
            this.id = id;
            lock = new ReentrantLock();
        }
        public int getId() {
            return id.get();
        }

        public Map<Integer, CachebleObj> getLocalData() {
            return roomData;
        }

        public List<ServerClient> getClientsInThisLocal() {
            return clientsInThisRoom;
        }

        public void addClientToLocal (ServerClient client) {
            lock.lock();
            try{

                if(clientsInThisRoom.contains(client)) {
                    System.err.println(client.getId() + " is already in this room");
                    return;
                }
                clientsInThisRoom.add(client);

            } finally {
                lock.unlock();
            }

        }

        public void removeClientFromLocal (ServerClient client) {
            lock.lock();
            try{
                if(clientsInThisRoom.contains(client)) {
                    clientsInThisRoom.remove(client);
                    System.out.println(client.getId() + " is removed from this room");
                } else {
                    System.err.println(client.getId() + " cannot be found in this room");
                }

            } finally {
                lock.unlock();
            }
        }

        public void addObjectToLocalData (CachebleObj objectToPut){
            lock.lock();
            try{
                if(roomData.containsValue(objectToPut)){
                    System.err.println("Already exists!");
                    return;
                }
                roomData.put(objectToPut.getId(), objectToPut);

            } finally {
                lock.unlock();
            }


        }

        public CachebleObj getObjectLocal(String name){
            lock.lock();
            try{
                for(Map.Entry<Integer, CachebleObj> singleObject: roomData.entrySet()) {
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
        private ServiceProviderChat service;

        private boolean openClientThread = false;

        private ServerClient theClient;

        private int clientID;

        public ClientThread (Socket clientSocket, ServiceProviderChat service,  ObjectInputStream fromClient, ObjectOutputStream toClient) {
            this.clientSocket = clientSocket;
            this.service = service;
            this.fromClient = fromClient;
            this.toClient = toClient;
            clientID = 0;

            openConnection();

        }

        public void setService (ServiceProviderChat newService) {
            service = newService;
        }

        public void openConnection() {
            openClientThread = true;

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
                            toClient.writeObject(removeClient(theClient, service));
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

                    addClient(theClient, chatRoom);
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
                addClient(client, room);
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
                putObject(theClient, objectToPut, service);
            } catch (IOException e) {
                System.err.println(e);
            }
        }
    }
}
