import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;

/**
 * Created by Arsa on 14.10.2017.
 */

abstract class Client{
    private String name;
    private int clientID;

    public String getName() {
        return name;
    }

    public int getClientID () {
        return clientID;
    }


    public void  handleRecievingListFromServer (List<String> listOfObj) {
        System.out.println(listOfObj.size() + " files:");
        //noinspection SimplifyStreamApiCallChains
        for (String s: listOfObj) {
            System.out.println("- " + s);
        }
    }

    /**
     * The template method for clients to follow
     */
    public void templateMethod () {
        openConnection();
        createClientReceiveThread();
        while (isOpen()) {
            getInstructions();
            createFullInstruction();
            sendInstruction();
        }
        closeConnection();
    }

    abstract boolean isOpen();
    abstract void openConnection();
    abstract void createClientReceiveThread();
    abstract void getInstructions();
    abstract void createFullInstruction();
    abstract void sendInstruction();
    abstract void closeConnection();

}
