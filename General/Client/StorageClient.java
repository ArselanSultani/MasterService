import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.List;

class StorageClient implements Client {
    String name;
    
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
        serverIP = "";
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
                        DataObject dataObject = new DataObject(fileName, fileContent);

                        addObject(toServer, dataObject);
                        break;

                    case '6':

                        removeAsClient(toServer);
                        System.exit(1);
                        break;

                    case '7':
                        removeObject(toServer, msgFromUser);
                        break;

                    default:
                        //addObject(toServer, new DataObject(msgFromUser, msgFromUser));
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

    /**
     * Printing the instruction list for client
     */
    public void printOptions(){

        System.out.println("Options: ");
        System.out.println("3: getting list of objects");
        System.out.println("4: for getting a file");
        System.out.println("5: for adding file");
        System.out.println("6: Usubscribing from services");
        System.out.println("7: Removing an object");
    }


    /**
     * Sending a message to service for wanting to add client
     * @param toService Which service to subscribe too
     * @param bf        for getting a input from client
     * @throws IOException
     */
    public void addClient(ObjectOutputStream toService, BufferedReader bf) throws IOException{

        if(clientID == 0) {
            toService.writeObject("1:"+clientID);
        } else  {
            System.out.print("Area #: ");
            int roomNr = Integer.parseInt(bf.readLine());
            toService.writeObject("2:"+clientID+":"+roomNr);
        }
    }


    /**
     * Client wanting to add object to service
     * @param toService     The stream to send data through
     * @param dataObject    The object the gets sent
     * @throws IOException
     */
    public void addObject (ObjectOutputStream toService, DataObject dataObject) throws IOException{
        toService.writeObject("5:"+clientID);
        dataObject.sendObject(toService);
    }

    /**
     * Wanting to get list of all the filenames in clients area on service
     * @param toService     The stream to send data through to the service
     * @throws IOException
     */
    public void getObjectList (ObjectOutputStream toService) throws IOException{
        toService.writeObject("3:"+clientID);
    }

    /**
     * Wanting to remove self as client from service
     * @param toService     Which service to unsubscribe from
     * @throws IOException
     */
    public void removeAsClient (ObjectOutputStream toService) throws IOException {
        toService.writeObject("6:"+clientID);
    }

    /**
     * Wanting to remove file from area
     * @param toService     The stream to send data through to service
     * @param removeFile    The file to remove from service
     * @throws IOException
     */
    public void removeObject (ObjectOutputStream toService, String removeFile) throws IOException{
        toService.writeObject("7:"+clientID+":"+removeFile);
    }

    /**
     * Wanting to get object from service
     * @param toService     The stream to get data through to service
     * @param fileName      The name of the file to retrieve from service
     * @throws IOException
     */
    public void getObject(ObjectOutputStream toService, String fileName) throws IOException{
        toService.writeObject("4:"+clientID+":"+fileName);
    }


    public void handleServiceString (String msg) {
        System.out.println("-"+msg);
    }

    /**
     * Print a message that file has been received
     * @param recievedObject    The file that has been received
     */
    public  void handleRecievingObjectFromServer (DataObject recievedObject) {
        System.out.println("Recieved: " + recievedObject.getObjectName());
    }


    /**
     * After receving a list of file names in area, print them
     * @param listOfObj List of file names on the area
     */
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

        /**
         * Handling feedback on whether the client is added or not
         * @param msg      The message for decoding and retrieving client ID
         */
        public void addedToNewArea(String msg){
            String[] spl = msg.split(":");
            storageClient.setClientID(Integer.parseInt(spl[1]));
            System.out.println("Received area: " + spl[1]);
        }

        /**
         * Handling message that an object has been removed from client
         * @param msg   The message that is received from service regarding removing file
         */
        void removedFile(String msg) {
            String[] spl = msg.split(":");
            System.out.println("File: " + spl[1] + " is deleted");
        }

        /**
         * Handling receiving a file from the service.
         * @throws IOException
         * @throws ClassNotFoundException
         */
        public void acceptFile () throws IOException, ClassNotFoundException{
            String fileName = (String) fromServer.readObject();
            String fileContent = (String) fromServer.readObject();
            int id = (int)fromServer.readObject();
            LocalDateTime localDateTime = (LocalDateTime) fromServer.readObject();

            DataObject newCach = new DataObject(fileName, fileContent);
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




