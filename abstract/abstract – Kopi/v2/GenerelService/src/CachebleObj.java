import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;


class CachebleObj implements Serializable {
    private static final long serialVersionUID = 1L;
    private Object object;
    private LocalDateTime expTime = null;
    private String objectName;

    private int id ;

    public CachebleObj( String objectName, Object object, int expireInMin) {
        this.object = object;
        this.objectName = objectName;
        this.id = 0;

        if(expireInMin > 0) {
            this.expTime = LocalDateTime.now();
        }
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

    public void setLatestAccessTime (LocalDateTime newTime) {
        expTime = newTime;
    }

    public void sendObject (ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.writeObject(objectName);
        objectOutputStream.writeObject(object);
        objectOutputStream.writeObject(id);
    }

    public void readObject(ObjectInputStream objectInputStream) throws IOException {
        try {
            objectName = (String)objectInputStream.readObject();
            object = (Object)objectInputStream.readObject();
            id = (int)objectInputStream.readObject();

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
 }
