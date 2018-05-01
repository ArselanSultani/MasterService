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

    void createClientReceiveThread(){
        System.out.println("Preparing for recieving massage...");
        new FromService(serverSocket, this);
    }
    //abstract void startClient();
    void getInstructions() {
        printOptions();
        try {
            System.out.print(">");
            ins = bf.readLine();
        } catch (IOException e) {
            System.err.println(e);
        }
    }

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

    void sendInstruction() {
        try{
            if(ins.equals("5")) {
                System.out.print("Filename: " );
                String newFileName = bf.readLine();
                System.out.print("Filecontent:\n>");
                String newFileContent = bf.readLine();
                toServer.writeObject(fullInstruction);
                new CachebleObj(newFileName, newFileContent).sendObject(toServer);
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


    public void printOptions(){

        System.out.println("Options: ");
        System.out.println("3: getting list of objects");
        System.out.println("4: for getting a file");
        System.out.println("5: for adding file");
        System.out.println("6: Usubscribing from services");
        System.out.println("7: Removing an object");
    }


    public void handleServiceString (String msg) {
        System.out.println("-"+msg);
    }

    public  void handleRecievingObjectFromServer (CachebleObj recievedObject) {
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














