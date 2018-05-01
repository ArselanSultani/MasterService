
import javax.xml.ws.Service;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class StartServiceStrategy {


    private int portNr = 1254;
    private boolean open = true;
    ServiceInterface si;

    public static void main(String[] args) {
        new StartServiceStrategy().initService();
    }

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
                    si = ServiceProviderChat.getChatService();
                    si.startServer(clientSocket, fromClient, toClient);
                } else {
                    System.out.println("Wrong type");
                }
                fromClient.close();
                toClient.close();
                clientSocket.close();
                //new ServiceProviderStorage();

                Thread.sleep(1000);
            }
        } catch (IOException | InterruptedException | ClassNotFoundException e) {
            System.err.println(e);
        }
    }


}
