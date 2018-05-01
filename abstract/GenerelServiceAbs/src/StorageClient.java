import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.List;

class StorageClient extends Client {
    Console console;
    boolean openClient;

    String serverIP = "193.157.174.41";
    int portNr = 1254;
    int clientID;

    int serverRoomNr = 0;

    public StorageClient() {

        clientID = 0;
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
            toServer.writeObject("storage");

            toServer.writeObject("1:"+clientID);
            printOptions();

            while(openClient) {
                Thread.sleep(1000);

                System.err.print(">");
                String msgFromUser = bf.readLine();

                switch (msgFromUser.charAt(0)) {

                    case '3':
                        getObjectList(toServer, clientID);
                        break;

                    case '4':
                    case '7':
                        System.out.print("Filename: ");
                        String fileName = bf.readLine();
                        getOrRemoveObject(4,toServer, fileName);
                        break;

                    case '5':
                        System.out.print("Name: ");
                        String newFileName = bf.readLine();
                        System.out.print("Content: ");
                        String newFileContent = bf.readLine();
                        addObject(toServer, new DataObject(newFileName, newFileContent),clientID);
                        break;

                    case '6':
                        removeAsClient(toServer, clientID);
                        break;

                    default:
                        System.out.println("Could not understand command, try again");
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
     * Printing the options that are necessary for clients to know what to do.
     */
    void printOptions (){
        System.out.println("Options: ");
        System.out.println("3: getting list of objects");
        System.out.println("4: for getting a file");
        System.out.println("5: for adding file");
        System.out.println("6: Usubscribing from services");
        System.out.println("7: Removing an object");
    }

    /**
     * For getting instruction to service
     * @param ins           The instruction that is being done
     * @param toService     The service that it is getting sent to
     * @param fileName      The filename that is needed
     * @throws IOException
     */
    void getOrRemoveObject(int ins, ObjectOutputStream toService, String fileName) throws IOException{
        toService.writeObject(ins +":"+clientID+":"+fileName);
    }


    public void handleServiceString (String msg) {
        System.err.println("-"+msg);
    }


    class FromService extends Thread{
        Socket socket  = null;
        StorageClient storageClient = null;
        ObjectInputStream fromServer = null;
        Boolean open;

        public FromService(Socket socket, StorageClient storageClient) {
            this.socket = socket;
            this.storageClient = storageClient;
            open = true;

            try {
                fromServer = new ObjectInputStream(socket.getInputStream());
            } catch (IOException e) {
                System.err.println(e);
            }

            start();
        }

        /**
         * Added the client to new room,
         * @param msg       The message that contains new ClientID and area ID
         */
        public void addedToNewArea(String msg){
            String[] spl = msg.split(":");
            storageClient.setClientID(Integer.parseInt(spl[1]));
            System.out.println("Received area: " + spl[1]);
        }

        /**
         * Message for having received a message about file removed
         * @param msg
         */

        void removedFile(String msg) {
            String[] spl = msg.split(":");
            System.out.println("File: " + spl[1] + " is deleted");
        }

        /**
         * receiving file from service
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

        /**
         * Handling communication with service.
         */
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














