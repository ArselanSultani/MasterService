

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

public class ChatService implements ServiceInterface {


    private Map<Integer, ServiceObject> listOfRooms ;
    private Map<ServerClient, ServiceObject> listOfObservers ;

    private List<ClientThread> clientThreads ;

    boolean openToClient = true;


    private int portNr ;

    private AtomicInteger id;
    private AtomicInteger chatRoomId;
    private AtomicInteger objectId;

    public ChatService(){
        listOfRooms = new HashMap<>();
        listOfObservers= new HashMap<>();
        clientThreads = new ArrayList<>();

        portNr = 1254;

        id = new AtomicInteger();
        chatRoomId = new AtomicInteger();
        objectId = new AtomicInteger();
    }

    public static void main(String[] args) {
        new ChatService().startServer();
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

    /**
     *
     * Getting a list of all the message in the chatroom
     * @param serviceObject     The chatroom which the messages are stored at
     * @return                  List of all the message in the chat room
     */
    public List<String> getObjectList(ServiceObject serviceObject){

            if(serviceObject == null) {
                System.out.println("Could not find room!");
                return Collections.emptyList();
            }


            System.out.println("Found the room and returning it");
            return serviceObject.getAreaData().values().stream().map(i -> i.getObjectName()).collect(Collectors.toList());


    }


    /**
     * Sending a message to chat room
     * @param client    Which client that adds it
     * @param obj       The object that is going to be added
     */
    public void putObject(ServerClient client, DataObject obj) {

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


    /**
     * Notify all clients in the room, except sender that an update is here
     * @param cl    The client that sent the message to the chat room
     * @param obj   The message that got sent, is now being sent to clients
     */
    public void notifyUsers(ServerClient cl,  DataObject obj) {
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

    /**
     * Adding client to a room
     * @param newUser   The user that is going to be added to the room.
     * @param chatRoom  The chatroom where the client is being added
     */
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

    /**
     * Removing the client from the chat room
     * @param removeUser    The client that is going to be removed
     * @param ct            The client thread that has to be stopped
     * @return
     */
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

    /**
     * Finding the client based on the client ID
     * @param clientID  The ID that client has, that needs to be found
     * @return          Returning the client that is being searched
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
     * Removing client socket for not getting notification anymore
     * @param clientToRemove    The clientthread to find the socket
     */
    public void removeClientSocket (ClientThread clientToRemove) {
        System.out.println("Removed client si");
        clientThreads.remove(clientToRemove);
    }



    class ChatRoom implements ServiceObject{
        ReentrantLock lock ;


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

        public Map<Integer, DataObject> getAreaData() {
            return roomData;
        }

        public List<ServerClient> getClientsInThisArea() {
            return clientsInThisRoom;
        }

        /**
         * Adding client to room. Not specified how many client who are allowed
         * @param client    The client that is being added
         */
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

        /**
         * Removing client from chat room
         * @param client    The client that is going to be removed.
         */
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

        /**
         * Adding a message to the chat room
         * @param objectToPut
         */
        public void addObjectToRoomData (DataObject objectToPut){
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
    }

    class ClientThread extends Thread{
        Socket clientSocket ;
        ObjectInputStream fromClient;
        ObjectOutputStream toClient;
        ChatService service;

        int clientID;

        boolean open = true;

        ServerClient theClient;

        public ClientThread (Socket clientSocket, ChatService service) {
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

        public void setService (ChatService newService) {
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

        /**
         * Adding client to a new room
         * @throws IOException
         */
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
                List<String> getObjectsInRoom = getObjectList(listOfObservers.get(clientID));
                toClient.writeObject(getObjectsInRoom);
            }


        }

        /**
         * Adding client to an already existing room.
         * @param serverClient  The client that is going to be added
         * @param analyseMsg    Message that needs to split to find room.
         * @throws IOException
         */
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

        /**
         * Getting list of all the messages from area
         * @throws IOException
         */
        void getLocalObjectList () throws IOException{

            ServiceObject chatRoom = listOfObservers.get(findClient(clientID));

            List<String> getObjectsInRoom = getObjectList(chatRoom);
            toClient.writeObject("3");
            toClient.writeObject(getObjectsInRoom);

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
            putObject(serverClient, objectToPut);
        }


    }
}
