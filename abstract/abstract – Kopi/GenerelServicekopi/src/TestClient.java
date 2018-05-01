import java.io.*;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;

class TestClient extends Client {
    String name;
    Console console;
    boolean openClient;



    String serverIP = "193.157.189.139";
    int portNr = 3213;
    int clientID;

    int serverRoomNr = 0;


    public static void main(String[] args) {
        new TestClient("test1").startClient();
    }



    public TestClient(String name) {
        this.name = name;
        clientID = 0;
        console = System.console();
    }

    public String getName() {
        return name;
    }

    public int getClientID () {
        return clientID;
    }

    public void setClientID (int newId) {
        clientID = newId;
    }



    @Override
    public void startClient () {
        try (
                Socket serverSocket= new Socket(serverIP, portNr);
                ObjectOutputStream toServer = new ObjectOutputStream(serverSocket.getOutputStream())
        ){


            openClient = true;
            new Thread (new FromService(serverSocket , this));
            BufferedReader bf = new BufferedReader(new InputStreamReader(System.in));

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
                        break;

                    default:
                        CachebleObj newObj = new CachebleObj(msgFromUser, msgFromUser, 0);
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

    @Override
    public void notifyClient() {
        System.err.println(name + " is notified that the state has been updated");
    }


    public void setServerRoomNr(int newRoomNr) {
        serverRoomNr = newRoomNr;
    }


    public void handleServiceString (String msg) {
        System.err.println("-"+msg);
    }

    public  void handleRecievingObjectFromServer (CachebleObj recievedObject) {
        System.err.println("Recieved: " + recievedObject.getObjectName());
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
        Socket socket  = null;
        TestClient testClient = null;
        ObjectInputStream fromServer = null;
        Boolean open;

        public FromService(Socket socket, TestClient testClient) {
            this.socket = socket;
            this.testClient = testClient;
            open = true;

            try {
                fromServer = new ObjectInputStream(socket.getInputStream());
            } catch (IOException e) {
                System.err.println(e);
            }

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
                        System.err.println(msgFromService);
                    } else if (msgFromService.charAt(0) == '2'){
                        String[] spl = msgFromService.split(":");
                        testClient.setClientID(Integer.parseInt(spl[1]));
                        testClient.setServerRoomNr(Integer.parseInt(spl[2]));
                        System.err.println(msgFromService);
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














