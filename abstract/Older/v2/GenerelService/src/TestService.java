import java.util.HashMap;
import java.util.Map;

/**
 * Created by Arsa on 14.10.2017.

class TestService {
    public static void main(String [] args) {

        Service ser = new ServiceProviderChat();
        Service ser1 = new ServiceProviderChat();

        CachebleObj c1 = new CachebleObj("first", new String("1."), 1 );
        CachebleObj c2 = new CachebleObj( "second",new Integer(2), 1 );
        CachebleObj c3 = new CachebleObj( "third", new String("3."), 1 );
        CachebleObj c4 = new CachebleObj( "fourth", new String("4."), 1 );
        CachebleObj c5 = new CachebleObj( "fifth", new String("5."), 1 );
        CachebleObj c6 = new CachebleObj( "sixth", new String("6."), 1 );


        Client t1 = new TestClient("first Client");
        Client t2= new TestClient("Second Client");
        Client t3 = new TestClient("third Client");
        Client t4 = new TestClient("fourth Client");

        System.out.println("*****************************************************");
        System.out.println("Adding clients to service.");

        t1.addService(ser);
        t2.addService(ser);
        t3.addService(ser);
        t4.addService(ser);


        System.out.println("\n*****************************************************");
        System.out.println("Adding object to service to check if they get added and other clients of" +
                " the service gets updated.");
        t1.addObject(ser, c1);


        System.out.println("\n*****************************************************");
        System.out.println("Removing client and see if they still get notification when not client anymore");
        t2.removeAsClient(ser);
        t3.addObject(ser, c2);

        System.out.println("************************************************");
        System.out.println("************************************************");
        ser.debugPrint();

        System.out.println("\n*****************************************************");
        System.out.println("See if objects get updated");
        c1.setNa("first first");
        t1.addObject(ser,c1);

        System.out.println("************************************************");
        System.out.println("************************************************");
        ser.debugPrint();

        System.out.println("*************************************************");
        System.out.println("Retrieving object");
        if(t1.getObject(ser, 1) != null ){
            System.out.println("Found the object");
        }

        System.out.println("*************************************************");
        System.out.println("Checking whether caching removing system works (2 already in there)");

        t3.addObject(ser, c3);
        t1.addObject(ser, c4);
        t3.addObject(ser, c5);

        t1.getObject(ser, 3);
        t3.getObject(ser, 1);
        System.out.println("************************************************");
        System.out.println("The cache now: ");
        ser.debugPrint();
        System.out.println("\nNew object gets added, the least recently used gets removed: " +
                "which has id: " + c2.getObjectName());

        t1.addObject(ser, c6);
        System.out.println("\nThe cache now");
        ser.debugPrint();




    }
}
*/