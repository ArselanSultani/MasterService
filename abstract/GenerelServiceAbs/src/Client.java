import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;

/**
 * Created by Arsa on 14.10.2017.
 */

abstract class Client{


    /**
     * The client wants to add object to the service
     * @param objectOutputStream        The stream to the service the client wants to send data to.
     * @param dataObject                The object that is going to be sent
     * @param clientID                  The client ID of the client who wants to send the object
     */
    public void addObject (ObjectOutputStream objectOutputStream, DataObject dataObject, int clientID) {
        try {
            objectOutputStream.writeObject("5:"+clientID);
            dataObject.sendObject(objectOutputStream);
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    /**
     * Getting a list of object , either messages or filenames from service
     * @param objectOutputStream        The stream to the service the client wants to send data to.
     * @param clientID                  The client ID of the object
     */
    public void getObjectList (ObjectOutputStream objectOutputStream, int clientID) {
        try {
            objectOutputStream.writeObject("3:"+clientID);
        } catch (IOException e) {
            System.err.println(e);

        }
    }

    /**
     * For removing client for service it is subscribed to
     * @param objectOutputStream        The stream to the service the client wants to send data to.
     * @param clientID                  The client ID of the object
     */
    public void removeAsClient (ObjectOutputStream objectOutputStream, int clientID) {
        try {
            objectOutputStream.writeObject("6:"+clientID);
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    /**
     * Handling receiving list, for printing them out
     * @param listOfObj             The list that is going to be printed
     */
    public void  handleRecievingListFromServer (List<String> listOfObj) {
        //noinspection SimplifyStreamApiCallChains
        listOfObj.stream().forEach(System.out::println);
    }

    /**
     * A method for starting the client, connecting to service and starting threads, instruction from client and decoding
     */
    abstract void startClient();

}
