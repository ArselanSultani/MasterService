import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

/**
 * Created by Arsa on 14.10.2017.
 */

abstract class Client{


    public void addObject (ObjectOutputStream objectOutputStream, CachebleObj cachebleObj, int clientID) {
        try {
            objectOutputStream.writeObject("5:"+clientID);
            cachebleObj.sendObject(objectOutputStream);
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    public void getObjectList (ObjectOutputStream objectOutputStream, int clientID) {
        try {
            objectOutputStream.writeObject("3:"+clientID);
        } catch (IOException e) {
            System.err.println(e);

        }
    }

    public void getObject(ObjectOutputStream objectOutputStream, String fileName, int clientID) {
        try {
            objectOutputStream.writeObject("4:"+clientID+":"+fileName);
        } catch (IOException e){
            System.err.println(e);

        }
    }

    public void removeAsClient (ObjectOutputStream objectOutputStream, int clientID) {
        try {
            objectOutputStream.writeObject("6:"+clientID);
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    public void  handleRecievingListFromServer (List<String> listOfObj) {
        //noinspection SimplifyStreamApiCallChains
        listOfObj.stream().forEach(System.out::println);
    }


    abstract void startClient();
    abstract void notifyClient();

}
