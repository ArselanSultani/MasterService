import java.io.BufferedReader;
import java.io.IOException;
import java.io.ObjectOutputStream;


/**
 * Created by Arsa on 14.10.2017.
 */

interface Client{
    void startClient();
    void addClient(ObjectOutputStream objectOutputStream, BufferedReader bf) throws IOException;
    void addObject (ObjectOutputStream objectOutputStream, DataObject dataObject) throws IOException;
    void getObjectList (ObjectOutputStream objectOutputStream) throws IOException;
    void removeAsClient (ObjectOutputStream objectOutputStream) throws IOException;
}
