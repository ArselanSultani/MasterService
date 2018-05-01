
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;


public class ChatService extends TemplateService {

    Map<Integer, ServiceObject> listOfRooms ;
    Map<ServerClient, ServiceObject> listOfObservers ;
    List<ChatService.ClientThread> clientThreads ;


    int portNr ;

    AtomicInteger id ;
    AtomicInteger chatRoomId ;
    AtomicInteger objectId ;

    ServerSocket serverSocket;

    boolean open;


    public ChatService(){
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
        new ChatService().startServer();
    }


    /**
     * Opening the ChatService for connection and when a Client is connected, starts it thread for sending and receiving
     */
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
                Thread t = new Thread(clientThread);
                t.start();

                Thread.sleep(1000);
            }
        } catch (IOException|InterruptedException e) {
            System.err.println(e);
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


    /**
     * Adding client to an room
     * @param newUser           The user that is getting added
     * @param serviceArea       The service chat room that the client is being added to.
     */
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

    /**
     * For notifying the clients that are part of the room
     * @param cl            The client who has added the message
     * @param obj           The message that is added to the room
     */
    public void notifyUsers(ServerClient cl, DataObject obj) {


        System.out.println("notifying...");
        ServiceObject cr = listOfObservers.get(cl);
        try {
            for (ChatService.ClientThread ct : clientThreads) {
                if (listOfObservers.get(ct.getTheClient()) == cr && ct.theClient != cl) {
                    ct.getToClient().writeObject("5:"+obj.getObjectName());
                }
            }
        } catch (IOException e){
            System.err.println(e);
        }
    }

    /**
     * Finding the client that has the specified client ID
     * @param clientID      The Client with this client ID is wanted
     * @return              Returning the object has the specified ID
     */
    public ServerClient findClient (int clientID) {

            for(ServerClient serverClient : listOfObservers.keySet()) {
                if (clientID == serverClient.getId()) {
                    return serverClient;
                }
            }
            return null;

    }


    /**
     * Removinf client socket from list
     * @param clientToRemove    The socket that has to be removed
     */
    public void removeClientSocket (ClientThread clientToRemove) {
        System.out.println("Removed client from service");
        clientThreads.remove(clientToRemove);

    }


    class ChatRoom implements ServiceObject{
        private ReentrantLock lock ;


        Map<Integer, DataObject> roomData= new HashMap<>();

        List<ServerClient> clientsInThisRoom = new ArrayList<>();

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
         * Adding client to chat room
         * @param client        The client that is being added
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
         * Removice client from the chat room
         * @param client        The client that has to be removed
         */
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

        /**
         * Adding message from client to the service
         * @param objectToPut   The message that is being added to the room
         */
        public void addObjectToLocalData (DataObject objectToPut){
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

        /**
         * Getting single message, not possible in this case
         * @param name      Name of the object that needs to be removed
         * @return      The object that has that specified name
         */
        public DataObject getObjectLocal(String name){
            System.out.println("Not possible");
            return null;
        }


    }

    class ClientThread extends ServiceObjectTemplate implements Runnable{
        private Socket clientSocket ;
        private ObjectInputStream fromClient;
        private ObjectOutputStream toClient;
        private ChatService service;

        private boolean openClientThread = false;

        private ServerClient theClient;

        private int clientID;


        private String instruction = "";
        private String returnString = "";
        private List<String> returnAreaData = Collections.EMPTY_LIST;


        public ClientThread (Socket clientSocket, ChatService service) {
            this.clientSocket = clientSocket;
            this.service = service;
            clientID = 0;

        }

        public ServerClient getTheClient() {
            return theClient;
        }

        public void setService (ChatService newService) {
            service = newService;
        }

        /**
         * Opening the input and output stream for communicating with the client
         */
        void openStreams() {
            try {
                fromClient = new ObjectInputStream(clientSocket.getInputStream());
                toClient = new ObjectOutputStream(clientSocket.getOutputStream());
                openClientThread = true;
            } catch (IOException e) {
                System.err.println(e);
            }
        }

        boolean isOpenService() {
            return openClientThread;
        }

        public ObjectOutputStream getToClient() {
            return toClient;
        }

        /**
         * Waiting for receiving instruction from client
         */
        void getInstruction() {
            try {
                instruction = (String) fromClient.readObject();
            } catch (ClassNotFoundException|IOException e) {
                System.err.println(e);
            }
        }

        /**
         * After having received an instruction, decode it and invoke the right methods
         */
        void decodeAndExecuteInstruction() {
            String [] inSplit = instruction.split(":");

            switch (inSplit[0]) {
                case "1":
                case "2":
                    handlingClientAddition(inSplit);
                    break;

                case "3":
                    getLocalObjectList();
                    break;

                case "5":
                    addLocalObject();
                    break;

                case "6":
                    removeClient(theClient, service);
                    break;

                default:
                    System.out.println("def");
                    break;

            }


        }

        /**
         * Creating a reply message to the client
         */
        void createReplyMsg() {
            char firstIns= instruction.charAt(0);
            returnString = firstIns+"";
            if(firstIns == '1' || firstIns == '2'){
                returnString = returnString+":"+clientID+":"+getListOfObservers().get(theClient).getId();
            }
        }

        /**
         * Replying the message that got created in the last step
         */
        void replyClient(){
            try {
                toClient.writeObject(returnString);

                if(instruction.charAt(0) == '3'){
                    toClient.writeObject(returnAreaData);
                }
            } catch (IOException e) {
                System.err.println(e);
            }

            instruction = "";
            returnString = "";
            returnAreaData = Collections.EMPTY_LIST;
        }

        /**
         * The streams is getting closed in this methods after clients instructions
         */
        void closeStreams() {
            try {
                fromClient.close();
                toClient.close();
                openClientThread = false;
            } catch (IOException e) {
                System.err.println(e);
            }
        }

        /**
         * Handling client addition, whether to add to new room or existing room
         * @param msg   The message for decoding depended on what the client wants
         */
        void handlingClientAddition(String[] msg){
            if(msg.length == 2) {
                addNewClientInNewLocalRoom();
            } else {
                addNewClientInExistingLocalRoom(msg);
            }
        }

        /**
         * Adding the client in a new room
         */
        void addNewClientInNewLocalRoom () {

            theClient = new ServerClient(returnId(), clientSocket.getInetAddress());
            ChatRoom chatRoom = new ChatRoom(returnChatRoomId());

            addClient(theClient, chatRoom);
            clientID = theClient.getId();

        }

        /**
         * Adding client to an existing room
         * @param analyseMsg        Decoding the message from client for finding the room the client wants to subscribe to.
         */
        void addNewClientInExistingLocalRoom ( String[] analyseMsg) {
            if (clientID == 0) {
                clientID = Integer.parseInt(analyseMsg[1]);
            }

            int roomAreaID = Integer.parseInt(analyseMsg[2]);
            ChatRoom room = (ChatRoom) service.getListOfLocalObject().get(roomAreaID);

            ServerClient client = findClient(clientID);
            if (client == null) {
                client = new ServerClient(returnId(), clientSocket.getInetAddress());
            }
            addClient(client, room);
            theClient = client;
            clientID = theClient.getId();

        }

        /**
         * Client has sent instruction for getting list of messages in the room, which the client will get sent back
         */
        void getLocalObjectList (){
                ServiceObject chatRoom = listOfObservers.get(findClient(clientID));
                returnAreaData= service.getObjectList(chatRoom);
        }

        /**
         * Client wants to add a new message to its chatroom that i t is subscribed to.
         */
        void addLocalObject () {

            DataObject objectToPut = new DataObject("", "");
            objectToPut.readObject(fromClient);
            objectToPut.setId(getObjectId().get());
            putObject(theClient, objectToPut, service);
            service.notifyUsers(theClient, objectToPut);

        }
    }
}
