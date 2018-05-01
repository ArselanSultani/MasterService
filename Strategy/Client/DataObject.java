import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;


class DataObject implements Serializable {
    private static final long serialVersionUID = 1L;
    private Object object;
    private LocalDateTime expTime = null;
    private String objectName;

    private int id ;

    public DataObject(String objectName, Object object) {
        this.object = object;
        this.objectName = objectName;
        this.id = 0;
        this.expTime = LocalDateTime.now();

    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setNa(String newName){
        objectName = newName;
    }

    public Object getObject(){
        return object;
    }

    public LocalDateTime getLatestAccessedTime() {
        return expTime;
    }

    public void setLatestAccessTime () {
        expTime = LocalDateTime.now();
    }


    /**
     * Sending data object over socket
     * @param objectOutputStream    Sending the object through the output stream that is given
     * @throws IOException          If something goes wrong with sending the object, IOException is thrown
     */

    public void sendObject (ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.writeObject(objectName);
        objectOutputStream.writeObject(object);
        objectOutputStream.writeObject(id);
        objectOutputStream.writeObject(expTime);
    }


    /**
     * Reading object from service
     * @param objectInputStream     The input stream to expect data from
     * @throws IOException
     */
    public void readObject(ObjectInputStream objectInputStream) throws IOException {
        try {
            objectName = (String)objectInputStream.readObject();
            object = objectInputStream.readObject();
            id = (int)objectInputStream.readObject();
            expTime = (LocalDateTime) objectInputStream.readObject();

        } catch (ClassNotFoundException e) {
            System.out.println(e);
        }
    }
}
