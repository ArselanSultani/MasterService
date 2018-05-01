import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.List;

class StorageClient implements Client {
    String name;
    ServiceInterface service;
    Console console;


    String serverIP ;
    int portNr ;
    int clientID;

    boolean openClient = true;

    int serverRoomNr = 1;



    public static void main(String[] args) {
        new StorageClient("test1").startClient();
    }



    public StorageClient(String name) {
        this.name = name;
        clientID = 0;
        console = System.console();
        serverIP = "193.157.174.41";
        portNr = 1254;
    }


    public void startClient () {
        String fileName;
        try (
                Socket serverSocket= new Socket(serverIP, portNr);
                ObjectOutputStream toServer = new ObjectOutputStream(serverSocket.getOutputStream());
        ){



            new FromService(serverSocket , this);
            BufferedReader bf = new BufferedReader(new InputStreamReader(System.in));

            addClient(toServer, bf);
            printOptions();


            while(openClient) {
                Thread.sleep(1000);
                System.out.print(">");
                String msgFromUser = bf.readLine();

                switch (msgFromUser.charAt(0)){
                    case '3':
                        getObjectList(toServer);
                        break;

                    case '4':
                        System.out.print("Filename: ");
                        fileName = bf.readLine();
                        getObject(toServer, fileName);
                        break;

                    case '5':
                        System.out.print("Filename: ");
                        fileName = bf.readLine();
                        System.out.print("FileContect:");
                        String fileContent = bf.readLine();
                        CachebleObj cachebleObj = new CachebleObj(fileName, fileContent);

                        addObject(toServer, cachebleObj);
                        break;

                    case '6':

                        removeAsClient(toServer);
                        System.exit(1);
                        break;

                    case '7':
                        removeObject(toServer, msgFromUser);
                        break;

                    default:
                        //addObject(toServer, new CachebleObj(msgFromUser, msgFromUser));
                        System.out.println("Command not understood. This are the options");


                }

            }
        } catch (IOException e){
            System.out.println("Server down!");
        } catch (InterruptedException e) {
            System.err.println(e);
            Thread.currentThread().interrupt();
        }
    }

    public void setClientID(int newID) {
        clientID = newID;
    }

    public void setServerRoomNr(int newRoomNr) {
        serverRoomNr = newRoomNr;
    }

    public void printOptions(){

        System.out.println("Options: ");
        System.out.println("3: getting list of objects");
        System.out.println("4: for getting a file");
        System.out.println("5: for adding file");
        System.out.println("6: Usubscribing from services");
        System.out.println("7: Removing an object");
    }


    public void addClient(ObjectOutputStream toService, BufferedReader bf) throws IOException{

        if(clientID == 0) {
            toService.writeObject("1:"+clientID);
        } else  {
            System.out.print("Area #: ");
            int roomNr = Integer.parseInt(bf.readLine());
            toService.writeObject("2:"+clientID+":"+roomNr);
        }
    }

    public void addObject (ObjectOutputStream toService, CachebleObj cachebleObj) throws IOException{
        toService.writeObject("5:"+clientID);
        cachebleObj.sendObject(toService);
    }

    public void getObjectList (ObjectOutputStream toService) throws IOException{
        toService.writeObject("3:"+clientID);
    }

    public void removeAsClient (ObjectOutputStream toService) throws IOException {
        toService.writeObject("6:"+clientID);
    }

    public void removeObject (ObjectOutputStream toService, String removeFile) throws IOException{
        toService.writeObject("7:"+clientID+":"+removeFile);
    }


    public void getObject(ObjectOutputStream toService, String fileName) throws IOException{
        toService.writeObject("4:"+clientID+":"+fileName);
    }


    public void handleServiceString (String msg) {
        System.out.println("-"+msg);
    }

    public  void handleRecievingObjectFromServer (CachebleObj recievedObject) {
        System.out.println("Recieved: " + recievedObject.getObjectName());
    }

    public void  handleRecievingListFromServer (List<String> listOfObj) {
        listOfObj.stream().forEach(System.out::println);
    }


    class FromService extends Thread{
        Socket socket  = null;
        StorageClient storageClient = null;
        ObjectInputStream fromServer = null;

        public FromService(Socket socket, StorageClient storageClient) {
            this.socket = socket;
            this.storageClient = storageClient;

            try {
                fromServer = new ObjectInputStream(socket.getInputStream());
            } catch (IOException e) {
                System.err.println(e);
            }

            start();
        }

        public void addedToNewArea(String msg){
            String[] spl = msg.split(":");
            storageClient.setClientID(Integer.parseInt(spl[1]));
            System.out.println("Received area: " + spl[1]);
        }

        public void addedToExistingArea(String msg){
            String[] spl = msg.split(":");
            storageClient.setClientID(Integer.parseInt(spl[1]));
            storageClient.setServerRoomNr(Integer.parseInt(spl[2]));
            System.out.println(msg);
        }

        void removedFile(String msg) {
            String[] spl = msg.split(":");
            System.out.println("File: " + spl[1] + " is deleted");
        }

        public void acceptFile () throws IOException, ClassNotFoundException{
            String fileName = (String) fromServer.readObject();
            String fileContent = (String) fromServer.readObject();
            int id = (int)fromServer.readObject();
            LocalDateTime localDateTime = (LocalDateTime) fromServer.readObject();

            CachebleObj newCach = new CachebleObj(fileName, fileContent);
            newCach.setId(id);

            System.out.println("name: " + fileName);
            System.out.println("content: " + fileContent);

        }

        @Override
        public void run() {
            while (openClient) {
                try {
                    String msgFromService = (String) fromServer.readObject();

                    if (msgFromService.charAt(0) == '1') {
                        addedToNewArea(msgFromService);
                    } else if (msgFromService.equals("3")) {
                        //Recieving list of objects from server
                        List<String> msg = (List<String>) fromServer.readObject();
                        storageClient.handleRecievingListFromServer(msg);
                    } else if (msgFromService.charAt(0) == '7'){
                        removedFile(msgFromService);
                    } else if (msgFromService.equals("4")){
                        acceptFile();
                    }else {
                        storageClient.handleServiceString(msgFromService);
                    }

                    Thread.sleep(1000);
                } catch (IOException|ClassNotFoundException e) {
                    System.err.println(e);
                } catch (InterruptedException e) {
                    System.err.println(e);
                    Thread.currentThread().interrupt();

                }

            }
        }
    }
}




