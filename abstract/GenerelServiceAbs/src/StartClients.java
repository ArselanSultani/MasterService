import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class StartClients {
    public static void main (String[] args) {
        new StartClients().initClient();
    }

    /**
     * Starting the client, with asking the client which platform it wants.
     */
    public void initClient() {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Choose service:\n1 for Chat\n2 for Storage");
        Client context;
        try {

            boolean success = false;
            while (!success) {

                int ins = Integer.parseInt(br.readLine().trim());

                switch (ins) {
                    case 1:
                        success = true;
                        context = new ChatClient();
                        context.startClient();
                        break;

                    case 2:
                        success = true;
                        context = new StorageClient();
                        context.startClient();
                        break;

                    default:
                        System.out.println("Could not understand command, please try again.");
                        break;
                }

            }

        } catch (IOException e) {
            System.err.println(e);
        }
    }


}
