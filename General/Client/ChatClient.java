import java.io.*;
import java.net.Socket;
import java.util.List;

class ChatClient implements Client {
    String name;
    ServiceInterface service;
    Console console;


    String serverIP ;
    int portNr ;
    int clientID;

    boolean openClient = true;

    int serverRoomNr = 1;



    public static void main(String[] args) {
        new ChatClient("test1").startClient();
    }



    public ChatClient(String name) {
        this.name = name;
        clientID = 5;
        console = System.console();
        serverIP = "193.157.251.13";
        portNr = 1254;
    }


    public void startClient () {
        try (
                Socket serverSocket= new Socket(serverIP, portNr);
                ObjectOutputStream toServer = new ObjectOutputStream(serverSocket.getOutputStream());
                ){

            new FromService(serverSocket , this);
            BufferedReader bf = new BufferedReader(new InputStreamReader(System.in));

            addClient(toServer, bf);

            while(openClient) {
                Thread.sleep(1000);
                System.out.print(">");
                String msgFromUser = bf.readLine();
                if (msgFromUser.equals("3")) {
                    getObjectList(toServer);

                } else if (msgFromUser.equals("6")) {
                    removeAsClient(toServer);
                    break;

                } else{
                    DataObject newObj = new DataObject(msgFromUser, msgFromUser);
                    addObject(toServer,newObj);
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
     * Receiving notification that new message is added
     */
    public void notifyClient() {
        System.out.println(name + " is notified that the state has been updated");
    }

    /**
     * Wanting to be added to a service, whether in a new room or existing one
     * @param toServer  The stream to send data through to Chat service
     * @param bf        Receiving input from client, when wanting to be added to already existing room
     * @throws IOException
     */
    public void addClient(ObjectOutputStream toServer, BufferedReader bf) throws IOException{

        if(clientID == 0) {
            toServer.writeObject("1:"+clientID);
        } else  {
            System.out.print("Area #: ");
            int roomNr = Integer.parseInt(bf.readLine());
            toServer.writeObject("2:"+clientID+":"+roomNr);
        }
    }

    /**
     * Wanting to send a message to the chatroom
     * @param objectOutputStream    The stream to send data through to Chat service
     * @param dataObject            The message that is going to be sent through
     * @throws IOException
     */
    public void addObject (ObjectOutputStream objectOutputStream, DataObject dataObject) throws IOException{
        objectOutputStream.writeObject("5:"+clientID);
        dataObject.sendObject(objectOutputStream);
    }

    /**
     * wanting to get list of all the messages in the chatroom
     * @param objectOutputStream    The stream to send data through to Chat service
     * @throws IOException
     */
    public void getObjectList (ObjectOutputStream objectOutputStream) throws IOException{
        objectOutputStream.writeObject("3:"+clientID);
    }

    /**
     * Wanting to be removed as a client to the service and removed from chat room
     * @param objectOutputStream    The stream to send data through to Chat service
     * @throws IOException
     */
    public void removeAsClient (ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.writeObject("6:"+clientID);
    }

    public void handleServiceString (String msg) {
        System.out.println("-"+msg);
    }

    /**
     * Printing the message received from client
     * @param recievedObject
     */
    public  void handleRecievingObjectFromServer (DataObject recievedObject) {
        System.out.println("Recieved: " + recievedObject.getObjectName());
    }

    /**
     * Handling printing all the messages on the chat room
     * @param listOfObj List of messages on the chat room
     */
    public void  handleRecievingListFromServer (List<String> listOfObj) {
        listOfObj.stream().forEach(System.out::println);
    }


    class FromService extends Thread{
        Socket socket  = null;
        ChatClient chatClient = null;
        ObjectInputStream fromServer = null;

        public FromService(Socket socket, ChatClient chatClient) {
            this.socket = socket;
            this.chatClient = chatClient;

            try {
                fromServer = new ObjectInputStream(socket.getInputStream());
            } catch (IOException e) {
                System.err.println(e);
            }

            start();
        }

        /**
         * Wanting to be added to the room
         * @param msg   The message for being split to find client ID and room number
         */
        public void addedToNewRoom(String msg){
            String[] spl = msg.split(":");
            chatClient.setClientID(Integer.parseInt(spl[1]));

            System.out.println("Added to room: " + spl[1]);
        }

        /**
         * Wanting to be added to already existing room and getting the client ID and room number
         * @param msg The message to be split and decoded
         */
        public void addedToExistingRoom(String msg){
            String[] spl = msg.split(":");
            chatClient.setClientID(Integer.parseInt(spl[1]));
            chatClient.setServerRoomNr(Integer.parseInt(spl[2]));
            System.out.println("Added to room: " + spl[1]);
        }


        /**
         * For handling receiving messages from client
         */
        @Override
        public void run() {
            while (openClient) {
                try {
                    String msgFromService = (String)fromServer.readObject();
                    if (msgFromService.charAt(0) == '1') {
                        addedToNewRoom(msgFromService);
                    } else if (msgFromService.charAt(0) == '2') {
                        addedToExistingRoom(msgFromService);
                    }else  if (msgFromService.equals("3")){
                        //Recieving list of objects from server
                        List<String> msg = (List<String>)fromServer.readObject();
                        chatClient.handleRecievingListFromServer(msg);
                    } else {
                        chatClient.handleServiceString(msgFromService);
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














