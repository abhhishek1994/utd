package edu.utdallas.project3.server;

/**
 * The meta information for nodes
 * @author Zeqing Li, zxl165030, The University of Texas at Dallas
 *
 */
public class Node implements Comparable<Node>{
    /**
     * Id of node
     */
    private int id;
    
    /**
     * Host name 
     */
    private String hostName;
    
    /**
     * Port
     */
    private String port;
    
    public Node(int index, String hostName, String port){
        this.id = index;
        this.hostName = hostName;
        this.port = port;
    }
    
    public Node (int index){
        this.id = index;
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

    @Override
    public int compareTo(Node other) {
        return Integer.compare(id, other.getNodeId());
    }
}