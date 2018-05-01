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

    public void setClientID (int newId) {
        clientID = newId;
    }

    public void addObject (ObjectOutputStream objectOutputStream, CachebleObj cachebleObj) {
        try {
            objectOutputStream.writeObject("5:"+clientID);
            cachebleObj.sendObject(objectOutputStream);
            cachebleObj.sendObject(objectOutputStream);
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    public void getObjectList (ObjectOutputStream objectOutputStream) {
        try {
            objectOutputStream.writeObject("3:"+clientID);
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    public void getObject(ObjectOutputStream objectOutputStream, String fileName) {
        try {
            objectOutputStream.writeObject("4:"+clientID+":"+fileName);
        } catch (IOException e){
            System.err.println(e);
        }
    }

    public void removeAsClient (ObjectOutputStream objectOutputStream) {
        try {
            objectOutputStream.writeObject("6:"+clientID);
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    public void  handleRecievingListFromServer (List<String> listOfObj) {
        System.out.println(listOfObj.size());
        //noinspection SimplifyStreamApiCallChains
        for (String s: listOfObj) {
            System.out.println(s);
        }
    }


    abstract void startClient();
    abstract void notifyClient();

}
