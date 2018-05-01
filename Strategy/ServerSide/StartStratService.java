
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class StartStratService {
    private int portNr = 1254;
    private boolean open = true;
    StrategyServiceAbs si;

    public static void main(String[] args) {
        new StartStratService().initService();
    }

    /**
     * The method that starts the service and depending on what service the client wants, it redirects that client that way
     */
    public void initService(){
        System.out.println("Starting server");
        System.setProperty("java.net.preferIPv4Stack", "true");

        try ( ServerSocket serverSocket = new ServerSocket(portNr)) {
            System.out.println("Open on IP: " + InetAddress.getLocalHost() + " - port: " + portNr);

            while(open) {

                Socket clientSocket = serverSocket.accept();
                System.out.println("A connection has been established: " + clientSocket.getInetAddress());

                ObjectInputStream fromClient = new ObjectInputStream(clientSocket.getInputStream());
                ObjectOutputStream toClient = new ObjectOutputStream(clientSocket.getOutputStream());

                String serviceType = (String)fromClient.readObject();

                if(serviceType.toLowerCase().equals("chat")){
                    si = ChatService.getService();
                    si.startService(clientSocket, fromClient, toClient);

                } else if(serviceType.toLowerCase().equals("storage")) {
                    si = StorageService.getStorageService();
                    si.startService(clientSocket, fromClient, toClient);

                } else {
                    System.out.println("Wrong type");
                }

                Thread.sleep(1000);
            }
        } catch (IOException | InterruptedException | ClassNotFoundException e) {
            System.err.println(e);
        }
    }
}
