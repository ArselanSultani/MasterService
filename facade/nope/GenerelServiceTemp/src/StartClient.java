import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class StartClient {

    public static void main(String[] args){
        new StartClient().initiateClient();
    }

    public void initiateClient(){
        BufferedReader bf = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Choose service:\n1:Chat\n2:Storage");
        Client context;
        try{
            int ins = Integer.parseInt(bf.readLine().trim());
            if(ins == 1) {
                context = new TestClient("ChatClient");
            } else {
                context = new TestClient("StorageClient");
            }

            context.startClient();
        } catch (IOException e) {
            System.err.println("Error when reading what kind of service the client wants");
        }
    }



}
