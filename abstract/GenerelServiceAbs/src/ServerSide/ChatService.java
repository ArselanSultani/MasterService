
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.net.InetAddress;



public class ChatService extends StrategyServiceAbs {
    private static ChatService thisService  = null;


    Map<Integer, ServiceObject> listOfRooms ;

    Map<ServerClient, ServiceObject> listOfObservers ;

    List<ClientThread> clientThreads ;


    int portNr ;

    AtomicInteger id ;

    AtomicInteger chatRoomId ;

    AtomicInteger objectId ;

    private boolean open;

    private  ChatService(){
        listOfRooms = new HashMap<Integer, ServiceObject>();
        listOfObservers = new HashMap<ServerClient, ServiceObject>();

        clientThreads = new ArrayList<>();


        portNr = 3213;


        System.out.println("Port nr: " + portNr);

        id = new AtomicInteger();
        chatRoomId = new AtomicInteger();
        objectId = new AtomicInteger();

        open = true;

    }

    public static ChatService getService () {
        if (thisService == null) {
            thisService = new ChatService();
        }
        return thisService;
    }

    void startService(Socket clientSocket, ObjectInputStream fromClient, ObjectOutputStream toClient) {
        System.out.println("Starting service");

        ClientThread clientThread = new ClientThread(clientSocket, this, fromClient, toClient);
        System.out.println("1");
        clientThreads.add(clientThread);
        clientThread.start();
        System.out.println("2");


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
    public Map<Integer, ServiceObject> getListOfLocal() {
        return listOfRooms;
    }

    @Override
    public Map<ServerClient, ServiceObject> getListOfObservers() {
        return listOfObservers;
    }

    @Override
    public void setListOfLocal(Map<Integer, ServiceObject> listOfRooms) {
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

            System.out.println("Added client " + newUser.getId()  + " to observer list");



    }


    //Removing client from the list of observers

    /**
     * Finding client based on client ID
     * @param clientID  The client ID of the client that needs to be found
     * @return          The client that the client ID belongs to
     */
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

    //Going through the user of the current service provider and notify them
    public void notifyUsers(ServerClient cl, DataObject obj) {


        ServiceObject cr = listOfObservers.get(cl);
        try {
            for (ChatService.ClientThread ct : clientThreads) {
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
        private ReentrantLock lock ;

        Map<Integer, DataObject> roomData= new HashMap<Integer, DataObject>();

        List<ServerClient> clientsInThisRoom = new ArrayList<ServerClient>();

        private AtomicInteger id;

        public ChatRoom (AtomicInteger id) {
            this.id = id;
            lock = new ReentrantLock();
        }
        public int getId() {
            return id.get();
        }

        public Map<Integer, DataObject> getLocalData() {
            return roomData;
        }

        public List<ServerClient> getClientsInThisLocal() {
            return clientsInThisRoom;
        }

        /**
         * Adding client to room. Not specified how many client who are allowed
         * @param client    The client that is being added
         */
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

        /**
         * Removing client from chat room
         * @param client    The client that is going to be removed.
         */
        public void removeClientFromLocal (ServerClient client) {
            lock.lock();
            try {

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


        /**
         * Adding a message to the chat room
         * @param objectToPut
         */
        public void addObjectToLocalData (DataObject objectToPut){

            lock.lock();
            try {

                if(roomData.containsValue(objectToPut)){
                    System.err.println("Already exists!");
                    return;
                }
                roomData.put(objectToPut.getId(), objectToPut);

            } finally {
                lock.unlock();
            }


        }

    }

    class ClientThread extends Thread{
        Socket clientSocket ;
        ObjectInputStream fromClient;
        ObjectOutputStream toClient;
        ChatService service;

        ServerClient theClient;

        private int clientID;

        public ClientThread (Socket clientSocket, ChatService service, ObjectInputStream fromClient, ObjectOutputStream toClient) {
            this.clientSocket = clientSocket;
            this.service = service;
            this.fromClient = fromClient;
            this.toClient = toClient;
            clientID = 0;
        }

        public void setService (ChatService newService) {
            service = newService;
        }


        public void run() {
            try{

                while (open) {
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

                        case "5":
                            addLocalObject (theClient);
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
                System.err.println(e);
            } catch (ClassNotFoundException|NullPointerException e) {
                System.err.println(e);
            }
        }

        public ServerClient getTheClient() {
            return theClient;
        }

        /**
         * Adding client to a new room
         * @throws IOException
         */
        void addNewClientInNewLocalRoom () {

            try {
                if (clientID == 0) {
                    ServerClient sc = new ServerClient(returnId(), clientSocket.getInetAddress());
                    theClient = sc;
                    ChatRoom storageArea = new ChatRoom(returnChatRoomId());

                    addClient(sc, storageArea);
                    System.out.println("New user, create new ID:" + sc.getId());

                    toClient.writeObject("1:" + sc.getId());
                } else {
                    System.out.println("User already exist, returning area contect instead. ClientID: " + clientID);
                    theClient = findClient(clientID);
                    List<String> getObjectsInRoom = getObjectList( listOfObservers.get(clientID));
                    toClient.writeObject(getObjectsInRoom);
                }
            } catch (IOException e){
                System.err.println(e);
            }

        }

        /**
         * Adding client to an already existing room.
         * @param serverClient  The client that is going to be added
         * @param analyseMsg    Message that needs to split to find room.
         */
        void addNewClientInExistingLocalRoom (ServerClient serverClient, String[] analyseMsg) {
            int roomAreaID = Integer.parseInt(analyseMsg[2]);
            ChatRoom room = (ChatRoom) service.getListOfLocal().get(roomAreaID);
            try {

                if (room == null) {
                    System.err.println("Room " + roomAreaID + " could not be found");
                    toClient.writeObject("Could not find room");
                    return;
                }
                ServerClient client = findClient(clientID);

                if (serverClient == null) {
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

        /**
         * Getting list of all the messages from area
         * @throws IOException
         */
        void getLocalObjectList (){
            try {

                ServiceObject chatRoom = listOfObservers.get(findClient(clientID));

                List<String> getObjectsInRoom = getObjectList(chatRoom);
                toClient.writeObject("3");
                toClient.writeObject(getObjectsInRoom);
            } catch (IOException e) {

            }
        }

        /**
         * Client wanting to send message to the room
         * @param serverClient  The client that wants to send the message
         * @throws IOException
         */
        void addLocalObject (ServerClient serverClient) throws IOException{
            DataObject objectToPut = new DataObject("", null);
            objectToPut.readObject(fromClient);
            objectToPut.setId(getObjectId().get());
            putObject(serverClient, objectToPut, service);
            notifyUsers(theClient, objectToPut);

        }
    }
}
