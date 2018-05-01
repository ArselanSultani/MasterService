import com.sun.security.ntlm.Server;

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

public class ServiceProviderStorage  extends ServiceInterface {

    private Map<Integer, ServiceObject> listOfAreas = new HashMap<>();
    private Map<ServerClient, ServiceObject> listOfObservers = new HashMap<>();

    private List<ClientThread> clientThreads = new ArrayList<>();
    private boolean openService = true;

    private int portNr = 1254;

    private final int maxComponents = 8 ;

    private AtomicInteger id = new AtomicInteger();
    private AtomicInteger areaId = new AtomicInteger();
    private AtomicInteger objectId = new AtomicInteger();

    public static void main(String[] args) {
        new ServiceProviderStorage().startServer();
    }

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
                clientThread.start();


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

    @Override
    public void setListOfLocalObject(Map<Integer, ServiceObject> listOfAreas) {
            this.listOfAreas = listOfAreas;

    }

    @Override
    public void setListOfObservers(Map<ServerClient, ServiceObject> listOfObservers){
            this.listOfObservers = listOfObservers;

    }

    @Override
    public Map<Integer, ServiceObject> getListOfLocalObject(){
        return listOfAreas;
    }

    @Override
    public Map<ServerClient, ServiceObject> getListOfObservers() {
        return listOfObservers;
    }

    @Override
    public void notifyUsers(ServerClient cl, CachebleObj obj) {
        ServiceObject cr = listOfObservers.get(cl);
        try {
            for (ServiceProviderStorage.ClientThread ct : clientThreads) {
                if (listOfObservers.get(ct.getTheClient()) == cr && ct.theClient != cl) {
                    ct.toClient.writeObject("Area has been updated");
                }
            }
        } catch (IOException e){
            System.err.println(e);
        }
    }

    @Override
    public void addClient(ServerClient newUser, ServiceObject serviceArea ) {
            serviceArea.addClientToLocal(newUser);
            if(listOfAreas.get(serviceArea.getId()) == null) {
                listOfAreas.put(serviceArea.getId(), serviceArea);
            }
            listOfObservers.put(newUser, serviceArea);


    }

    ServiceObject findEmptyArea() {
            for(Map.Entry<Integer, ServiceObject> room: listOfAreas.entrySet()) {
                if(room.getValue().getClientsInThisLocal().size() == 0) {
                    return room.getValue();
                }
            }

        return null;
    }

    //getObject recieves a key from parameter and returns the object from
    //Whether the object gets returned or not depends on size and network connection type
    public List<String> getObject(ServiceObject serviceObject){

            if(serviceObject == null) {
                System.out.println("Could not find room!");
                return null;
            }
            return serviceObject.getLocalData().values().stream().map(i -> i.getObjectName()).collect(Collectors.toList());

    }


    //Here, the single object gets returned, like for example a single file in  a
    public CachebleObj getSingleObject(ServerClient client,String name){
            return listOfObservers.get(client).getObjectLocal(name);
    }

    //Here we store a data in our caching system.





    public ServerClient findClient (int clientID) {
        for(ServerClient serverClient : listOfObservers.keySet()) {
            if (clientID == serverClient.getId()) {
                return serverClient;
            }
        }
        return null;


    }

    public void removeClientSocket (ClientThread clientToRemove) {
        clientThreads.remove(clientToRemove);
    }




    class StorageArea implements ServiceObject{
        ReentrantLock lock = new ReentrantLock();

        Map<Integer, CachebleObj> areaData = new HashMap<>();

        ServerClient areaClient;

        List<ServerClient> clientsInThisArea = new ArrayList<>();

        private AtomicInteger id;

        public StorageArea(AtomicInteger id) {
            this.id = id;
        }
        public int getId() {
            return id.get();
        }

        public Map<Integer, CachebleObj> getLocalData() {
            return areaData;
        }

        public List<ServerClient> getClientsInThisLocal(){
            return clientsInThisArea;
        }

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

        public void addObjectToLocalData (CachebleObj objectToPut){

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

        public CachebleObj getObjectLocal(String name){

            lock.lock();
            try{

                for(Map.Entry<Integer, CachebleObj> singleObject: areaData.entrySet()) {
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
        private ServiceProviderStorage service;

        boolean openStorageClientThread;

        ServerClient theClient;

        public ClientThread (Socket clientSocket, ServiceProviderStorage service) {
            this.clientSocket = clientSocket;
            this.service = service;
            openConnection();
        }


        private int clientId;

        public void setService (ServiceProviderStorage newService) {
            service = newService;
        }


        public ServerClient getTheClient() {
            return theClient;
        }

        void openConnection() {
            try {
                fromClient = new ObjectInputStream(clientSocket.getInputStream());
                toClient = new ObjectOutputStream(clientSocket.getOutputStream());
                openStorageClientThread = true;
            } catch (IOException e) {
                System.err.println(e);
            }
        }

        void closeConnection () {
            try {
                fromClient.close();
                toClient.close();
                openStorageClientThread = false;
            } catch (IOException e) {
                System.err.println(e);
            }
        }

        @Override
        public void run() {
            try {
                while (openStorageClientThread) {
                    String instruction = (String) fromClient.readObject();
                    String[] inSplit = instruction.split(":");

                    if(inSplit.length < 1 || inSplit.length > 3) {
                        toClient.writeObject("Error! Could not understand the command");
                        continue;
                    }

                    switch (inSplit[0]){
                        case "1":
                        case "2":
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
                            toClient.writeObject(removeClient(theClient, service));
                            break;

                        default:
                            break;

                    }
                }
            } catch (IOException|ClassNotFoundException e) {
                System.err.println(e);
            }
        }

        void addClientToNewRoom () {
            try {
                if(clientId == 0) {
                    theClient = new ServerClient(returnId(), clientSocket.getInetAddress());
                    StorageArea storageArea = new StorageArea(returnAreaId());

                    addClient(theClient, storageArea);

                    System.out.println("New used with id: "+ theClient.getId() + " on area: " + storageArea.getId());
                    toClient.writeObject("1:" + theClient.getId());

                } else {
                    System.out.println("User already exists, return area object list instead") ;
                    getAreaContents();
                }
            } catch (IOException e) {
                System.err.println(e);
            }
        }



        void getContent(String[] instructions) {
            if(instructions.length == 2) {
                getAreaContents();
            } else {
                getSingleContent(instructions);
            }
        }

        void getAreaContents() {
            try{
                ServiceObject areaToCollect = listOfObservers.get(theClient);
                List<String> contents = getObject(areaToCollect);

                toClient.writeObject("3");
                toClient.writeObject(contents);
            } catch (IOException e) {
                System.err.println(e);
            }
        }

        void getSingleContent (String[] instructions) {
            String fileName = instructions[2];
            CachebleObj getObjectVar = getSingleObject(theClient, fileName);
            try {
                toClient.writeObject("4");
                getObjectVar.sendObject(toClient);
            } catch (IOException e) {
                System.err.println(e);
            }
        }

        void addFileInArea(){
            try {

                CachebleObj newObject = new CachebleObj("",null);
                newObject.readObject(fromClient);
                newObject.setId(getObjectId().get());
                putObject(theClient, newObject, service);
            } catch (IOException e) {
                System.err.println(e);
            }

        }

    }
}
