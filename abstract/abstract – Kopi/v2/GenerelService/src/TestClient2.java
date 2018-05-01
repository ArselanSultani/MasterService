import javax.naming.event.ObjectChangeListener;
import java.io.*;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;

class TestClient2 extends Client {
    String name;
    Console console;


    String serverIP = "193.157.165.171";
    int portNr = 3213;
    int clientID;

    int serverRoomNr = 1;


    public static void main(String[] args) {
        new TestClient2("test1").startClient();
    }



    public TestClient2(String name) {
        this.name = name;
        clientID = 0;
        console = System.console();
    }

    @Override
    public void startClient () {
        try (
                Socket serverSocket= new Socket(serverIP, portNr);
                ObjectOutputStream toServer = new ObjectOutputStream(serverSocket.getOutputStream());
        ){

            if(clientID == 0) {
                //First time "greeting" the server with clientID as zero.
                toServer.writeObject("1:" + clientID);



            } else {
                toServer.writeObject("2:"+clientID+serverRoomNr);
                //getObjectList(toServer, fromServer);
                //Return the list of object
            }

            new Thread (new FromService(serverSocket , this));
            BufferedReader bf = new BufferedReader(new InputStreamReader(System.in));
            while(true) {
                Thread.sleep(1000);
                System.out.print(">");
                String msgFromUser = bf.readLine();
                if(msgFromUser.equals("3")){
                    System.out.println("Getting the list from server");
                    toServer.writeObject("3:"+clientID);
                } else {
                    CachebleObj newObj = new CachebleObj(msgFromUser, msgFromUser, 0);
                    toServer.writeObject("5:"+clientID);
                    newObj.sendObject(toServer);
                }
            }

            /*getObjectList(toServer, fromServer);


            System.out.println("**********************************\n");
            CachebleObj c1 = new CachebleObj("fourth", new String("fourth"), 4);
            toServer.writeObject("5:"+clientID);
            c1.sendObject(toServer);

            System.out.println("**********************************\n");
            //To check whether the new object has been added
            getObjectList(toServer, fromServer);

            System.out.println("**********************************\n");
            CachebleObj returnObj = getObject(toServer, fromServer, "fifth");
            if(returnObj != null) {
                System.out.println("returned: " + returnObj.getObjectName());
            }

            System.out.println("**********************************\n");
            removeAsClient(toServer, fromServer);

            /*

            System.out.println("Sending");
            toServer.writeObject("1:4:1");
            System.out.println("Sent");

            String rec = (String)fromServer.readObject();
            System.out.println(rec);
            System.out.println("Recieved");*/





        } catch (IOException e){
            System.out.println("Server down!");
        } catch (InterruptedException e) {

        }

        /*service = newService;
        service.addClient(this, 0);*/

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
        Socket socket  = null;
        TestClient2 testClient = null;
        ObjectInputStream fromServer = null;

        public FromService(Socket socket, TestClient2 testClient) {
            this.socket = socket;
            this.testClient = testClient;

            try {
                fromServer = new ObjectInputStream(socket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }

            start();
        }


        public void run() {
            while (true) {
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
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


            }
        }
    }
}














