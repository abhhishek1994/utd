package aos;

/**
 * 
 * @author Zeqing Li, zxl165030, The University of Texas at Dallas
 *
 */
public class Node{
    private int nodeId;
    private String host;
    private String port;
    
    public Node(int index, String hostName, String port){
        this.nodeId = index;
        this.host = hostName;
        this.port = port;
    }

    public int getNodeId(){
        return nodeId;
    }
    
    public String getHostName(){
        return host;
    }
    
    public int getPort(){
        return Integer.parseInt(port);
    }
    
    @Override
    public String toString(){
        return String.format("[index = %d] [host = %s] [port = %s]", nodeId, host, port);
    }
}