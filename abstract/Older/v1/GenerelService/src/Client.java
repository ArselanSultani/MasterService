import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

/**
 * Created by Arsa on 14.10.2017.
 */

interface Client{
    String getName();
    void getObject(ObjectOutputStream objectOutputStream, ObjectInputStream objectInputStream, String fileName);
    void getObjectList (ObjectOutputStream objectOutputStream,ObjectInputStream objectInputStream);
    void startClient();
    void removeAsClient (ObjectOutputStream objectOutputStream, ObjectInputStream objectInputStream);
    void notifyClient();
}
