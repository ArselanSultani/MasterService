import java.io.IOException;
import java.io.ObjectInputStream;
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
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    public void removeAsClient (ObjectOutputStream objectOutputStream, ObjectInputStream objectInputStream) {
        try {
            objectOutputStream.writeObject("6:"+clientID);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void  handleRecievingListFromServer (List<String> listOfObj) {
        listOfObj.stream().forEach(System.out::println);
    }


    abstract void startClient();
    abstract void notifyClient();

}
