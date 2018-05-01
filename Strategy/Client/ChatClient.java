import java.io.*;
import java.net.Socket;
import java.util.List;

class ChatClient extends Client {

    Console console;
    boolean openClient;



    String serverIP = "";
    int portNr = 1254;
    int clientID;

    int serverRoomNr = 0;




    public ChatClient() {
        clientID = 3;
        console = System.console();
    }


    public int getClientID () {
        return clientID;
    }

    public void setClientID (int newId) {
        clientID = newId;
    }

    public void setServerRoomNr(int newRoomNr) {
        serverRoomNr = newRoomNr;
    }


    @Override
    public void startClient () {
        try (
                Socket serverSocket= new Socket(serverIP, portNr);
                ObjectOutputStream toServer = new ObjectOutputStream(serverSocket.getOutputStream())
        ){


            openClient = true;
            new FromService(serverSocket , this);
            BufferedReader bf = new BufferedReader(new InputStreamReader(System.in));
            toServer.writeObject("chat");

            if(clientID == 0) {
                toServer.writeObject("1:"+clientID);
            } else  {
                System.out.print("Area #: ");
                int roomNr = Integer.parseInt(bf.readLine());
                toServer.writeObject("2:"+clientID+":"+roomNr);
            }


            while(openClient) {
                Thread.sleep(1000);

                System.err.print(">");
                String msgFromUser = bf.readLine();

                switch (msgFromUser) {

                    case "3":
                        getObjectList(toServer, clientID);
                        break;

                    case "6":
                        removeAsClient(toServer, clientID);
                        openClient=false;
                        break;

                    default:
                        DataObject newObj = new DataObject(msgFromUser, msgFromUser);
                        addObject(toServer, newObj, clientID);
                        break;
                }
            }

        } catch (IOException e){
            System.err.println("Server down!");
        } catch (InterruptedException e) {
            System.err.println(e);
            Thread.currentThread().interrupt();
        }


    }

    /**
     * Printing the message from client
     * @param msg
     */
    public void msgReceived(String msg) {
        System.err.println("-"+msg);
    }


    class FromService extends Thread{
        Socket socket  = null;
        ChatClient chatClient = null;
        ObjectInputStream fromServer = null;
        Boolean open;

        public FromService(Socket socket, ChatClient chatClient) {
            this.socket = socket;
            this.chatClient = chatClient;
            open = true;

            try {
                fromServer = new ObjectInputStream(socket.getInputStream());
            } catch (IOException e) {
                System.err.println(e);
            }

            start();
        }

        /**
         * Added the client to new room, returned with new ID and room number
         * @param msg   The message than contains instuction number, client id and room nr which the client is subscribed to
         */
        void addedClientNewRoom (String msg) {
            String[] spl = msg.split(":");
            chatClient.setClientID(Integer.parseInt(spl[1]));
            System.err.println(msg);
        }

        /**
         * Added the client to an existing room, returned with same ID and room number
         * @param msg   The message than contains instuction number, client id and room nr which the client is subscribed to
         */
        void addedClientRoom (String msg) {
            String[] spl = msg.split(":");
            chatClient.setClientID(Integer.parseInt(spl[1]));
            chatClient.setServerRoomNr(Integer.parseInt(spl[2]));
            System.err.println(msg);
        }

        /**
         * Handling communication betwee client and service
         */
        @Override
        public void run() {
            while (open) {
                try {
                    String msgFromService = (String)fromServer.readObject();
                    if (msgFromService.charAt(0) == '1') {
                        addedClientNewRoom(msgFromService);

                    } else if (msgFromService.charAt(0) == '2'){
                        addedClientRoom(msgFromService);

                    } else if (msgFromService.equals("3")){
                        //Recieving list of objects from server
                        List<String> msg = (List<String>)fromServer.readObject();
                        chatClient.handleRecievingListFromServer(msg);
                    }  else {
                        chatClient.msgReceived(msgFromService);
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














