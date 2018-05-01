import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.List;


class StorageClient extends Client {
    private String name;
    private boolean openClient;


    private String serverIP ;
    private int portNr = 1254;
    private int clientID;

    int serverRoomNr;

    private Socket serverSocket;
    private ObjectOutputStream toServer;
    private ObjectInputStream fromServer ;

    BufferedReader bf;
    String ins = "";
    String fullInstruction="";



    public static void main(String[] args) {
        new StorageClient("test1").templateMethod();
    }

    public void setClientID (int newId) {
        clientID = newId;
    }


    public StorageClient(String name) {
        this.name = name;
        clientID = 0;
        openClient = false;


        serverIP = "193.157.187.9";
        serverRoomNr = 0;

    }
    public void setServerRoomNr(int newRoomNr) {
        serverRoomNr = newRoomNr;
    }

    public boolean isOpen() {
        return openClient;
    }

    /**
     * Opening socket for connecting to client and the streams for communicating with the service.
     */
    public void openConnection() {
        System.out.println("Trying to establish connection...");
        try {
            serverSocket = new Socket(serverIP, portNr);
            toServer = new ObjectOutputStream(serverSocket.getOutputStream());
            fromServer = new ObjectInputStream(serverSocket.getInputStream());
            openClient = true;

            toServer.writeObject("1:" + clientID);

            bf = new BufferedReader(new InputStreamReader(System.in));

        } catch (IOException e) {
            System.err.println(e);
        }
    }

    /**
     * Create a thread for handling receiving messages and data from the service
     */
    void createClientReceiveThread(){
        System.out.println("Preparing for recieving massage...");
        new FromService(serverSocket, this);
    }

    /**
     * Getting instruction from client, about what it wants to do.
     */
    void getInstructions() {
        printOptions();
        try {
            System.out.print(">");
            ins = bf.readLine();
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    /**
     * Creating a fully message with necessary information for the service to know who it is from and what does the client want
     * like file name that is retrieved or removed
     */
    void createFullInstruction(){
        if (ins.equals("3") || ins.equals("6") ) {
            //Getting list or unsub
            fullInstruction = ins+":"+clientID;
        } else if (ins.equals("4") || ins.equals("7") ) {
            //Getting or removing file
            String fn="";
            try {
                fn = bf.readLine();
            } catch (IOException e) {
                System.err.println(e);
            }
            fullInstruction = ins+":"+clientID+":"+fn;

        }else{
            fullInstruction = "5:"+clientID;
        }

    }

    /**
     * Sending the message along with other data to service, like a file
     */
    void sendInstruction() {
        try{
            if(ins.equals("5")) {
                System.out.print("Filename: " );
                String newFileName = bf.readLine();
                System.out.print("Filecontent:\n>");
                String newFileContent = bf.readLine();
                toServer.writeObject(fullInstruction);
                new DataObject(newFileName, newFileContent).sendObject(toServer);
            } else {
                toServer.writeObject(fullInstruction);
            }

            if(ins.equals("6"))
                openClient = false;

            ins = "";
            fullInstruction = "";
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    /**
     * Closing connection after clients wishes, closes socket, streams and console.
     */
    void closeConnection() {
        try{
            serverSocket.close();
            toServer.close();
            fromServer.close();
            System.out.println("Exit succesful");
            System.exit(0);
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    /**
     * A list of options for clients to use
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
     * Printing the received message from client
     * @param msg       The message from client
     */
    public void handleServiceString (String msg) {
        System.out.println("-"+msg);
    }

    /**
     * Handling receiving a list of filenames from client
     * @param recievedObject
     */
    public  void handleRecievingObjectFromServer (DataObject recievedObject) {
        System.out.println("Recieved: " + recievedObject.getObjectName());
    }


    class FromService extends Thread{
        Socket socket;
        StorageClient storageClient;
        Boolean open;

        public FromService(Socket socket, StorageClient storageClient) {
            this.socket = socket;
            this.storageClient = storageClient;
            open = true;

            start();
        }

        /**
         * Added the client to a new area, information needs to retracted from message
         * @param msg       The message with information that the client needs like the area ID and new clientID
         */
        public void addedToNewArea(String msg){
            String[] spl = msg.split(":");
            storageClient.setClientID(Integer.parseInt(spl[1]));
            System.out.println("Received area: " + spl[1]);
        }


        /**
         * For receiving a file from service
         * @throws IOException                  For connection, in case it gets lost, or received a null object
         * @throws ClassNotFoundException       Class of object received is not the same as the object expected
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
                        System.out.println("File: is deleted");
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














