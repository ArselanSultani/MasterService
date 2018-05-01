
import java.io.*;
import java.net.Socket;
import java.util.List;


class TestClient extends Client {
    private String name;
    private boolean openClient;


    private String serverIP ;
    private int portNr = 1254;
    private int clientID;

    int serverRoomNr;

    private Socket serverSocket;
    private ObjectOutputStream toServer;
    private ObjectInputStream fromServer ;


    public TestClient(String name) {
        this.name = name;
        clientID = 3;
        openClient = false;


        serverIP = "193.157.254.211";
        serverRoomNr = 0;

    }

    @Override
    public void startClient () {
        openConnection();
        new FromService(serverSocket, this);
        BufferedReader bf = new BufferedReader(new InputStreamReader(System.in));

        try {
            if (clientID == 0) {
                toServer.writeObject("1:" + clientID);
            } else {
                System.out.print("Area #: ");
                int roomNr = Integer.parseInt(bf.readLine());
                toServer.writeObject("2:" + clientID + ":" + roomNr);
            }
            while (openClient) {
                Thread.sleep(1000);
                System.err.print(">");
                String msgFromUser = bf.readLine();

                switch (msgFromUser) {

                    case "3":
                        toServer.writeObject("3:" + clientID);
                        break;

                    case "6":
                        System.out.println("Exiting");
                        toServer.writeObject("6:"+clientID);
                        closeConnection();
                        break;

                    default:
                        CachebleObj newObj = new CachebleObj(msgFromUser, msgFromUser, 0);
                        toServer.writeObject("5:" + clientID);
                        newObj.sendObject(toServer);
                        break;
                }
            }
        } catch (IOException e){
            System.out.println("Server down!\n" + e);
            System.err.println(e);

        } catch (InterruptedException e) {
            System.err.println(e);
            Thread.currentThread().interrupt();
        }
    if(openClient) closeConnection();


    }

    public void openConnection() {
        try {
            System.out.println("opening socket");
            serverSocket = new Socket(serverIP, portNr);
            toServer = new ObjectOutputStream(serverSocket.getOutputStream());
            fromServer = new ObjectInputStream(serverSocket.getInputStream());

            toServer.writeObject("chat");

            openClient = true;
        } catch (IOException e) {
            System.err.println(e);


        }
    }

    public void closeConnection() {
        try{
            serverSocket.close();
            toServer.close();
            fromServer.close();
            openClient = false;
            System.out.println("Exit succesful");
            System.exit(0);
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    @Override
    public void notifyClient() {
        System.out.println(name + " is notified that the state has been updated");
    }


    public void setServerRoomNr(int newRoomNr) {
        serverRoomNr = newRoomNr;
    }


    public void handleServiceString (String msg) {
        System.out.println("-"+msg);
    }

    public  void handleRecievingObjectFromServer (CachebleObj recievedObject) {
        System.out.println("Recieved: " + recievedObject.getObjectName());
    }


    class ClientService {

        private String ipAddress;
        private int portNr;

        public ClientService(String ipAddress, int portNr) {
            this.ipAddress = ipAddress;
            this.portNr = portNr;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public void setIpAddress(String ipAddress) {
            this.ipAddress = ipAddress;
        }

        public int getPortNr() {
            return portNr;
        }

        public void setPortNr(int portNr) {
            this.portNr = portNr;
        }
    }

    class FromService extends Thread{
        Socket socket;
        TestClient testClient;
        Boolean open;

        public FromService(Socket socket, TestClient testClient) {
            this.socket = socket;
            this.testClient = testClient;
            open = true;

            start();
        }

        @Override
        public void run() {
            while (open) {
                try {
                    String msgFromService = (String)fromServer.readObject();
                    if (msgFromService.equals("4")) {
                        //Recieving object from server
                        CachebleObj newObj = new CachebleObj("", null, 0);
                        newObj.readObject(fromServer);
                        testClient.handleRecievingObjectFromServer(newObj);

                    } else if (msgFromService.equals("3")){
                        //Recieving list of objects from server
                        List<String> msg = (List<String>)fromServer.readObject();
                        testClient.handleRecievingListFromServer(msg);
                    } else if (msgFromService.charAt(0) == '1') {
                        String[] spl = msgFromService.split(":");
                        testClient.setClientID(Integer.parseInt(spl[1]));
                        System.out.println(msgFromService);
                    } else if (msgFromService.charAt(0) == '2'){
                        String[] spl = msgFromService.split(":");
                        testClient.setClientID(Integer.parseInt(spl[1]));
                        testClient.setServerRoomNr(Integer.parseInt(spl[2]));
                        System.out.println(msgFromService);

                    } else {
                        testClient.handleServiceString(msgFromService);
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














