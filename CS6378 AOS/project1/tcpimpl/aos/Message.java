package aos;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * 
 * @author Zeqing Li, zxl165030, The University of Texas at Dallas
 *
 */
public class Message implements Comparable<Message>, Serializable{

	private static final long serialVersionUID = 1L;
	
	private int round;
    private int srcNode;
    private int dstNode;
    private String status;
    
    private HashSet<Integer> knownHost;
    
    public Message(){
        this.round = -1;
        this.srcNode = -1;
        this.dstNode = -1;
        this.status = "";
    }
    
    public Message(int round, int src, int dst, Set<Integer> knownHost) {
        this.round = round;
        this.srcNode = src;
        this.dstNode = dst;
        this.knownHost = new HashSet<>(knownHost);
        this.status = "";
    }
    
    
    
    public int getSrcNode() {
		return srcNode;
	}

	public void setSrcNode(int srcNode) {
		this.srcNode = srcNode;
	}

	public void setRound(int round) {
		this.round = round;
	}

	public int getRound(){
        return round;
    }
	
    
    public String getStatus() {
		return status;
	}

	public void setStatus(String message) {
		this.status = message;
	}

    
    public HashSet<Integer> getKnownHost() {
		return knownHost;
	}

	public void setKnownHost(HashSet<Integer> knownHost) {
		this.knownHost = knownHost;
	}

	@Override 
    public String toString(){
        String s = String.format("[Round %d] SOURCE = %d DST = %d [Content: %s] [Msg: \"%s\"]", 
        		this.round, this.srcNode, this.dstNode, knownHost.toString(), status);
        return s;
            
    }

    @Override
    public int compareTo(Message other){
        return Integer.compare(this.round, other.getRound());
    }

}