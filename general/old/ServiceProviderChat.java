

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

public class ServiceProviderChat  implements ServiceInterface {


    private Map<Integer, ServiceObject> listOfRooms ;
    private Map<ServerClient, ServiceObject> listOfObservers ;

    private List<ClientThread> clientThreads ;

    boolean openToClient = true;


    private int portNr ;

    private AtomicInteger id;
    private AtomicInteger chatRoomId;
    private AtomicInteger objectId;

    public ServiceProviderChat(){
        listOfRooms = new HashMap<>();
        listOfObservers= new HashMap<>();
        clientThreads = new ArrayList<>();

        portNr = 1254;

        id = new AtomicInteger();
        chatRoomId = new AtomicInteger();
        objectId = new AtomicInteger();
    }

    public static void main(String[] args) {
        new ServiceProviderChat().startServer();
    }

    public void startServer() {

        try ( ServerSocket serverSocket = new ServerSocket(portNr) ) {
            System.out.println("Open on: " + InetAddress.getLocalHost() + " and port#: " + portNr);

            while(openToClient) {

                Socket clientSocket = serverSocket.accept();
                System.out.println("A connection has been established: " + clientSocket.getInetAddress());

                ClientThread clientThread = new ClientThread(clientSocket, this);
                //Thread singleClientThread = clientThread;

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

    //getObject recieves a key from parameter and returns the object from
    //Whether the object gets returned or not depends on size and network connection type
    public List<String> getObject(ServiceObject serviceObject){

            if(serviceObject == null) {
                System.out.println("Could not find room!");
                return Collections.emptyList();
            }


            System.out.println("Found the room and returning it");
            return serviceObject.getAreaData().values().stream().map(i -> i.getObjectName()).collect(Collectors.toList());


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

            ServiceObject chatRoom = listOfObservers.get(client);

            if(chatRoom == null) {
                System.out.println("User is not subscribed to this service");
                return;
            }

            chatRoom.addObjectToRoomData(obj);

            notifyUsers(client, obj);


    }

    //If the time of object on our caching system has expired, then we will
    //remove it from our caching system.
    public void removeObject( Map.Entry<Integer, ServiceObject> pair, ServerClient cl){
        listOfRooms.remove(pair.getKey(), pair.getValue());

    }



    public void notifyUsers(ServerClient cl,  CachebleObj obj) {
        ServiceObject cr = listOfObservers.get(cl);
        try {
            for (ClientThread ct : clientThreads) {
                if (listOfObservers.get(ct.getTheClient()) == cr && ct.theClient != cl) {
                    ct.toClient.writeObject(obj.getObjectName());
                }
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    //Adding client to the list of observers
    public void addClient(ServerClient newUser, ServiceObject chatRoom) {

            if(listOfObservers.get(newUser) != null) {
                System.out.println("User already subscribed");
                return;
            }

            //IF room already exists
            if(listOfRooms.get(chatRoom) != null) {
                System.out.println("Found Existing room to add client to");
                listOfObservers.put(newUser, (ChatRoom) chatRoom);
                chatRoom.addClientToRoom(newUser);
                return;
            }


            //if the room does not exist

            listOfRooms.put(chatRoom.getId(), chatRoom);
            listOfObservers.put(newUser, (ChatRoom) chatRoom);
            chatRoom.addClientToRoom(newUser);

            System.out.println("Added client " + newUser.getId()  + " to observer list");

    }

    //Removing client from the list of observers
    public String removeClient (ServerClient removeUser, Thread ct){


            ChatRoom chatRoom = (ChatRoom) listOfObservers.get(removeUser);
            if(chatRoom == null) {
                System.out.println("Could not find the user.");
                return "Could not find the user";
            }
            chatRoom.removeClientFromRoom(removeUser);
            listOfObservers.remove(removeUser, chatRoom);
            if(chatRoom.getClientsInThisArea().isEmpty()) {
                listOfRooms.remove(chatRoom.getId());
                return "User removed and also room removed";
            }
            System.out.println(chatRoom.getClientsInThisArea().size());
            removeClientSocket((ClientThread) ct);
            return "User removed only";

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
        System.out.println("Removed client si");
        clientThreads.remove(clientToRemove);
    }



    class ChatRoom implements ServiceObject{
        ReentrantLock lock ;


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

        public Map<Integer, CachebleObj> getAreaData() {
            return roomData;
        }

        public List<ServerClient> getClientsInThisArea() {
            return clientsInThisRoom;
        }

        public void addClientToRoom (ServerClient client) {

            lock.lock();
            try {

                if(clientsInThisRoom.contains(client)) {
                    System.out.println(client.getId() + " is already in this room");
                    return;
                }
                clientsInThisRoom.add(client);

            } finally {
                lock.unlock();
            }

        }

        public void removeClientFromRoom (ServerClient client) {
            lock.lock();
            try {
                if(clientsInThisRoom.contains(client)) {
                    clientsInThisRoom.remove(client);
                    System.out.println(client.getId() + " is removed from this room");
                } else {
                    System.out.println(client.getId() + " cannot be found in this room");
                }

            } finally {
                lock.unlock();
            }


        }

        public void addObjectToRoomData (CachebleObj objectToPut){
            lock.lock();
            try {
                if(roomData.containsValue(objectToPut)){
                    System.out.println("Already exists!");
                    return;
                }
                roomData.put(objectToPut.getId(), objectToPut);


            } finally {
                lock.unlock();
            }


        }

        public CachebleObj getObject(String name){
            lock.lock();
            try {
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
        Socket clientSocket ;
        ObjectInputStream fromClient;
        ObjectOutputStream toClient;
        ServiceProviderChat service;

        int clientID;

        boolean open = true;

        ServerClient theClient;

        public ClientThread (Socket clientSocket, ServiceProviderChat service) {
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

        public void setService (ServiceProviderChat newService) {
            service = newService;
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
                            System.out.println("Choosing to add a new client to a new room");
                            addNewClientInNewLocalRoom();
                            break;


                        case "2":
                            addNewClientInExistingLocalRoom(theClient, analyseMsg);
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


                        default:
                            System.out.println("Default");
                            break;
                    }


                }
            } catch (IOException e) {
                removeClientSocket(this);
                System.err.println(e);
            } catch (ClassNotFoundException|NullPointerException e) {
                System.err.println(e);
            }
        }

        public ServerClient getTheClient() {
            return theClient;
        }
        void addNewClientInNewLocalRoom() throws IOException{
            if (clientID == 0) {
                ServerClient sc = new ServerClient(returnId(), clientSocket.getInetAddress());
                theClient = sc;
                ChatRoom chatRoom = new ChatRoom(returnChatRoomId());


                addClient(sc, chatRoom);
                toClient.writeObject("1:" + sc.getId());


            } else {
                System.out.println("User already exist, returning area contect instead. ClientID: " + clientID);
                theClient = findClient(clientID);
                List<String> getObjectsInRoom = getObject(listOfObservers.get(clientID));
                toClient.writeObject(getObjectsInRoom);
            }


        }

        void addNewClientInExistingLocalRoom (ServerClient serverClient, String[] analyseMsg) throws IOException{
            int roomAreaID = Integer.parseInt(analyseMsg[2]);
            ChatRoom room = (ChatRoom) service.listOfRooms.get(roomAreaID);
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
            service.addClient(client, room);
            theClient = client;
            toClient.writeObject("2:"+client.getId()+":"+room.getId());
            toClient.writeObject("Client added to room:" + room.getId());


        }

        void getLocalObjectList () throws IOException{

            ServiceObject chatRoom = listOfObservers.get(findClient(clientID));

            List<String> getObjectsInRoom = getObject(chatRoom);
            toClient.writeObject("3");
            toClient.writeObject(getObjectsInRoom);

        }

        void getLocalObject(String[]  analyseMsg, ServerClient serverClient) throws IOException{
            String singleObject = analyseMsg[2];
            CachebleObj returnObj = getSingleObject(serverClient, singleObject);
            toClient.writeObject("4");
            returnObj.sendObject(toClient);
        }

        void addLocalObject (ServerClient serverClient) throws IOException{
            CachebleObj objectToPut = new CachebleObj("", null, 0);
            objectToPut.readObject(fromClient);
            objectToPut.setId(getObjectId().get());
            putObject(serverClient, objectToPut);
        }


    }
}
