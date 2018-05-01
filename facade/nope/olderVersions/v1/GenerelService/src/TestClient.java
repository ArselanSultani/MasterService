import javax.naming.event.ObjectChangeListener;
import java.io.*;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;

class TestClient implements Client {
    String name;
    ServiceInterface service;
    Console console;


    String serverIP = "127.0.0.1";
    int portNr = 1254;
    int clientID;

    int serverRoomNr = 1;


    public static void main(String[] args) {
        new TestClient("test1").startClient();
    }



    public TestClient(String name) {
        this.name = name;
        clientID = 0;
        console = System.console();
    }


    public void startClient () {
        try (
                Socket serverSocket= new Socket(serverIP, portNr);
                ObjectOutputStream toServer = new ObjectOutputStream(serverSocket.getOutputStream());
                ){



            new Thread (new FromService(serverSocket , this));
            BufferedReader bf = new BufferedReader(new InputStreamReader(System.in));
            while(true) {
                Thread.sleep(1000);
                System.out.print(">");
                String msgFromUser = bf.readLine();
                if (msgFromUser.equals("3")) {
                    System.out.println("Getting the list from server");
                    toServer.writeObject("3:" + clientID);
                } else if (msgFromUser.equals("2")) {
                    System.out.print("RoomNr: ");
                    int roomNr = Integer.parseInt(bf.readLine());
                    toServer.writeObject("2:"+clientID+":"+roomNr);
                }else{
                    CachebleObj newObj = new CachebleObj(msgFromUser, msgFromUser, 0);
                    toServer.writeObject("5:"+clientID);
                    newObj.sendObject(toServer);
                }
            }





        } catch (IOException e){
            System.out.println("Server down!");
        } catch (InterruptedException e) {

        }


    }

    public String getName(){
        return name;
    }

    public void setClientID(int newID) {
        clientID = newID;
    }

    public void setServerRoomNr(int newRoomNr) {
        serverRoomNr = newRoomNr;
    }

    public void notifyClient() {
        System.out.println(name + " is notified that the state has been updated");
    }

    public void getObjectList (ObjectOutputStream objectOutputStream,ObjectInputStream objectInputStream) {
        try {
            objectOutputStream.writeObject("3:"+clientID);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void getObject(ObjectOutputStream objectOutputStream, ObjectInputStream objectInputStream, String fileName) {
        try {
            objectOutputStream.writeObject("4:"+clientID+":"+fileName);

        } catch (IOException e){
            e.printStackTrace();
        }

    }

    public void addObject (ObjectOutputStream objectOutputStream, CachebleObj cachebleObj) {
        try {
            objectOutputStream.writeObject("5:"+clientID);
            cachebleObj.sendObject(objectOutputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void removeAsClient (ObjectOutputStream objectOutputStream, ObjectInputStream objectInputStream) {
        try {
            objectOutputStream.writeObject("6:"+clientID);
        } catch (IOException e) {
            e.printStackTrace();
        }
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

        public FromService(Socket socket, TestClient testClient) {
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














