
import java.io.*;
import java.net.Socket;
import java.util.List;


class ChatClient extends Client {
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
        new ChatClient("test1").templateMethod();
    }

    public void setClientID (int newId) {
        clientID = newId;
    }


    public ChatClient(String name) {
        this.name = name;
        clientID = 4;
        openClient = false;


        serverIP = "193.157.187.9";
        serverRoomNr = 0;

    }
    public void setServerRoomNr(int newRoomNr) {
        serverRoomNr = newRoomNr;
    }

    /**
     * Finding if the client wants to disconnect from service or not
     * @return
     */
    public boolean isOpen() {
        return openClient;
    }

    /**
     * Opening the connection for socket and streams and handling adding client for first time
     */
    public void openConnection() {
        System.out.println("Trying to establish connection...");
        try {
            serverSocket = new Socket(serverIP, portNr);
            toServer = new ObjectOutputStream(serverSocket.getOutputStream());
            fromServer = new ObjectInputStream(serverSocket.getInputStream());
            openClient = true;

            bf = new BufferedReader(new InputStreamReader(System.in));

            if (clientID == 0) {
                toServer.writeObject("1:" + clientID);
            } else {
                System.out.print("Area #: ");
                int roomNr = Integer.parseInt(bf.readLine());
                toServer.writeObject("2:" + clientID + ":" + roomNr);
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    /**
     * Handing the creation of thread for handling receiving from service.
     */
    void createClientReceiveThread(){
        System.out.println("Preparing for recieving massage...");
        new FromService(serverSocket, this);
    }

    /**
     * Getting instruction from client
     */
    void getInstructions() {
        try {
            System.out.print(">");
            ins = bf.readLine();
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    /**
     * Creating an fully service understandable instruction message
     */
    void createFullInstruction(){
        if (ins.equals("3") || ins.equals("6") ) {
            fullInstruction = ins+":"+clientID;
        } else {
            fullInstruction = "5:"+clientID;
        }

    }

    /**
     * Sending the instruction message and necessary data
     */
    void sendInstruction() {
        try{
            toServer.writeObject(fullInstruction);

            if (fullInstruction.charAt(0) =='5' && fullInstruction.charAt(1) ==':') {
                new DataObject(ins, ins).sendObject(toServer);
            } else if (ins.equals("6")){
                openClient=false;
            }

            ins = "";
            fullInstruction = "";
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    /**
     * Closing the connection with service.
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




    public void handleServiceString (String msg) {
       System.out.println(msg.substring(1));
    }

    public  void handleRecievingObjectFromServer (DataObject recievedObject) {
        System.out.println("Recieved: " + recievedObject.getObjectName());
    }


    class FromService extends Thread{
        Socket socket;
        ChatClient chatClient;
        Boolean open;

        public FromService(Socket socket, ChatClient chatClient) {
            this.socket = socket;
            this.chatClient = chatClient;
            open = true;

            start();
        }

        /**
         * Handing receiving messages and data from service.
         */
        @Override
        public void run() {
            while (open) {
                try {
                    String msgFromService = (String)fromServer.readObject();
                   if (msgFromService.equals("3")){
                        //Recieving list of objects from server
                        List<String> msg = (List<String>)fromServer.readObject();
                        chatClient.handleRecievingListFromServer(msg);

                    } else if (msgFromService.charAt(0) == '1') {
                        String[] spl = msgFromService.split(":");
                        chatClient.setClientID(Integer.parseInt(spl[1]));
                        System.out.println(msgFromService);

                   } else if (msgFromService.charAt(0) == '2'){
                        String[] spl = msgFromService.split(":");
                        chatClient.setClientID(Integer.parseInt(spl[1]));
                        chatClient.setServerRoomNr(Integer.parseInt(spl[2]));
                        System.out.println(msgFromService);

                    } else {
                        chatClient.handleServiceString(msgFromService);
                    }

                    Thread.sleep(1000);
                } catch (ClassNotFoundException|IOException e) {
                    System.err.println(e);
                } catch (InterruptedException e) {
                    System.err.println(e);
                    Thread.currentThread().interrupt();
                }

            }
        }
    }
}














