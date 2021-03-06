
import java.net.InetAddress;

@SuppressWarnings("ALL")
class ServerClient {
    private int id;
    private InetAddress address;

    public ServerClient(int id, InetAddress address) {
        this.id = id;
        this.address = address;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    /**
     * Return the IP address of the client
     * @return  clients IP address
     */
    public InetAddress  getAddress() {
        return address;
    }

    public void setAddress(InetAddress address) {
        this.address = address;
    }

}