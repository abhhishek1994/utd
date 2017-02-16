package aos;

/**
 * 
 * @author Zeqing Li, zxl165030, The University of Texas at Dallas
 *
 */
public class Node{
    private int id;
    private String hostName;
    private String port;
    
    public Node(int index, String hostName, String port){
        this.id = index;
        this.hostName = hostName;
        this.port = port;
    }

    public int getNodeId(){
        return id;
    }
    
    public String getHostName(){
        return hostName;
    }
    
    public int getPort(){
        return Integer.parseInt(port);
    }
    
    @Override
    public String toString(){
        return String.format("[index = %d] [host = %s] [port = %s]", id, hostName, port);
    }
}