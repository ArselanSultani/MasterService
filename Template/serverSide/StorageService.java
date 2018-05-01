

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class StorageService extends TemplateService {

    private Map<Integer, ServiceObject> listOfAreas = new HashMap<>();
    private Map<ServerClient, ServiceObject> listOfClients = new HashMap<>();

    private List<ClientThread> clientThreads = new ArrayList<>();
    private boolean openService = true;

    private int portNr = 1254;

    private final int maxComponents = 8 ;

    private AtomicInteger id = new AtomicInteger();
    private AtomicInteger areaId = new AtomicInteger();
    private AtomicInteger objectId = new AtomicInteger();

    public static void main(String[] args) {
        new StorageService().startServer();
    }

    /**
     * Receiving connection from client and starting the client's thread for communication
     */

    public void startServer() {

        try (ServerSocket serverSocket = new ServerSocket(portNr);
             ) {
            System.out.println("Open on: " + InetAddress.getLocalHost() + " and port#: " + portNr);
            while(openService) {

                Socket clientSocket = serverSocket.accept();
                System.out.println("A connection has been established: " + clientSocket.getInetAddress());

                ClientThread clientThread = new ClientThread(clientSocket, this);
                //Thread singleClientThread = clientThread;
                clientThreads.add(clientThread);
                Thread t = new Thread(clientThread);
                t.start();



                Thread.sleep(2000);
            }
        } catch (IOException e) {
            System.err.println(e);
        } catch (InterruptedException e) {
            System.err.println(e);
            Thread.currentThread().interrupt();
        }
        openService = false;
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

    /**
     * After the method in superclass has edited the the local area, it updates on StorageService
     * @param listOfAreas       The updated list of areas
     */
    @Override
    public void setListOfLocalObject(Map<Integer, ServiceObject> listOfAreas) {
            this.listOfAreas = listOfAreas;

    }

    /**
     * When list of observers are updated in the super class, it gets updated the local list on StorageService
     * @param listOfClients
     */
    public void setListOfObservers(Map<ServerClient, ServiceObject> listOfClients){
            this.listOfClients = listOfClients;

    }

    @Override
    public Map<Integer, ServiceObject> getListOfLocalObject(){
        return listOfAreas;
    }

    public Map<ServerClient, ServiceObject> getListOfObservers() {
        return listOfClients;
    }



    @Override
    public void addClient(ServerClient newUser, ServiceObject serviceArea ) {
            serviceArea.addClientToLocal(newUser);
            if(listOfAreas.get(serviceArea.getId()) == null) {
                listOfAreas.put(serviceArea.getId(), serviceArea);
            }
            listOfClients.put(newUser, serviceArea);


    }

    /**
     * When a client is being added, sometimes the system looks for empty rooms, in case the system has failed to remove an area without client
     * @return  the area without clients that the system may have failed to remove
     */
    ServiceObject findEmptyArea() {
            for(Map.Entry<Integer, ServiceObject> room: listOfAreas.entrySet()) {
                if(room.getValue().getClientsInThisLocal().size() == 0) {
                    return room.getValue();
                }
            }

        return null;
    }


    /**
     * Removing object from storage area, implemented in StorageService, because it is not possible to remove message
     * @param name          The name of the file that is being removed
     * @param client        The owner of the area
     */
    public void removeObject(String name, ServerClient client){
        StorageArea area = (StorageArea) listOfClients.get(client);
        area.removeAreaObject(name);


    }


    /**
     * Getting single file from StorageArea
     * @param client        The client that wants the object
     * @param name          The name of the file wanted
     * @return              Returning the object that the client has asked for
     */
    public DataObject getSingleObject(ServerClient client,String name){
            return listOfClients.get(client).getObjectLocal(name);
    }

    /**
     * Finding the cliend with specific ID
     * @param clientID          The ID of the client that is wanted
     * @return                  The Client with that client ID
     */
    public ServerClient findClient (int clientID) {
        for(ServerClient serverClient : listOfClients.keySet()) {
            if (clientID == serverClient.getId()) {
                return serverClient;
            }
        }
        return null;


    }

    /**
     * Removing a client from the service
     * @param clientToRemove        The client to remove from service area
     */
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
         * Adding client to storage
         * @param client    The client that is being removed
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
         * Removing the client in the StorageArea
         * @param client        The client that is being removed
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
         * Adding file to the StorageArea
         * @param objectToPut       The file that is being added
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
         * Returning the file that the client has asked for if it exists
         * @param name       Name of the file that is requested
         * @return          The file that is asked by the client
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


        /**
         * Removing files from the area
         * @param filename      The name of the file that is being removed
         */
        public void removeAreaObject (String filename) {
            int id = -1;

            for (DataObject obj : areaData.values()){
                if (obj.getObjectName().equals(filename)) {
                    id = obj.getId();
                    break;
                }
            }

            if(id != -1) {
                areaData.remove(id);
            }
        }




    }

    class ClientThread extends ServiceObjectTemplate  {
        private Socket clientSocket ;
        private ObjectInputStream fromClient;
        private ObjectOutputStream toClient;
        private StorageService service;

        boolean openStorageClientThread = false;

        ServerClient theClient;
        private int clientId;


        private String instruction = "";
        private String returnString = "";
        private List<String> returnAreaData = Collections.EMPTY_LIST;
        private DataObject returnObjectFile = null;


        public ClientThread (Socket clientSocket, StorageService service) {
            this.clientSocket = clientSocket;
            this.service = service;

        }

        public ServerClient getTheClient() {
            return theClient;
        }


        public void setService (StorageService newService) {
            service = newService;
        }

        void openStreams() {
            try {
                fromClient = new ObjectInputStream(clientSocket.getInputStream());
                toClient = new ObjectOutputStream(clientSocket.getOutputStream());
                openStorageClientThread = true;
            } catch (IOException e) {
                System.err.println(e);
            }
        }

        boolean isOpenService(){
            return openStorageClientThread;
        }


        /**
         * Receiving instruction from the client
         */
        void getInstruction() {
            try {
                instruction = (String) fromClient.readObject();
            } catch (ClassNotFoundException|IOException e) {
                System.err.println(e);
            }
        }


        /**
         * After having received a message, it is being decoded and the right methods are being invoked
         */
        void decodeAndExecuteInstruction() {
            String[] inSplit = instruction.split(":");

            switch (inSplit[0]){
                case "1":
                    addClientToNewRoom();
                    break;

                case "3":
                case "4":
                    getContent(inSplit);
                    break;

                case "5":
                    addFileInArea();
                    break;

                case "6":
                    //toClient.writeObject(removeClient(theClient, service));
                    System.out.println();
                    break;

                default:
                    break;

            }

        }

        /**
         * Creating a reply message for the client
         */
        void createReplyMsg(){
            returnString = instruction.charAt(0)+"";
            switch (instruction.charAt(0)){
                case '1':
                    returnString = returnString + ":"+ clientId+":"+ getListOfObservers().get(theClient).getId();
                    System.out.println(returnString);
                    break;

                case '7':
                    returnString = returnString+":file";
                    break;
            }
        }

        /**
         * Sending the reply message back to the client
         */
        void replyClient(){
            try {
                toClient.writeObject(returnString);

                if(instruction.charAt(0) == '3') {
                    toClient.writeObject(returnAreaData);
                    System.out.println("size:" + returnAreaData.size());
                } else if (instruction.charAt(0) == '4') {
                    returnObjectFile.sendObject(toClient);
                }

            } catch (IOException e) {
                System.err.println(e);
            }

            instruction = "";
            returnString = "";
            returnAreaData = Collections.EMPTY_LIST;
            returnObjectFile = null;

        }


        /**
         * When the client is done, the connection gets closed
         */
        void closeStreams () {
            try {
                fromClient.close();
                toClient.close();
                openStorageClientThread = false;
            } catch (IOException e) {
                System.err.println(e);
            }
        }


        /**
         * Adding new clients to a new area.
         */
        void addClientToNewRoom () {
            theClient = new ServerClient(returnId(), clientSocket.getInetAddress());
            StorageArea storageArea = new StorageArea(returnAreaId());

            addClient(theClient, storageArea);
            clientId = theClient.getId();

            System.out.println("New used with id: "+ theClient.getId() + " on area: " + storageArea.getId());
        }

        /**
         * The client wants to retrieve either a file or a file list
         * @param instructions  The instruction that is being splittet to find what the client really wants
         */
        void getContent(String[] instructions) {
            if(instructions.length == 2) {
                getAreaContents();
            } else {
                getSingleContent(instructions);
            }
        }

        /**
         * Getting list with names of the files that are available.
         */
        void getAreaContents() {
            ServiceObject areaToCollect = listOfClients.get(theClient);
            returnAreaData = getObjectList(areaToCollect);
            //toClient.writeObject(contents);
        }

        /**
         * Getting a single file from the service area.
         * @param instructions      The instruction that are splittet to get the right information
         */
        void getSingleContent (String[] instructions) {
            String fileName = instructions[2];
            returnObjectFile = getSingleObject(theClient, fileName);
            /*try {
                getObjectVar.sendObject(toClient);
            } catch (IOException e) {
                System.err.println(e);
            }*/
        }

        /**
         * Adding a file to the client's area
         */
        void addFileInArea(){
            DataObject newObject = new DataObject("",null);
            newObject.readObject(fromClient);
            newObject.setId(getObjectId().get());
            putObject(theClient, newObject, service);
        }

        /**
         * Removing area from the client's area
         * @param fileName      The filename that is going to be removed
         */
        void removeFileInArea (String fileName ){
            removeObject(fileName, theClient);
        }



    }
}
