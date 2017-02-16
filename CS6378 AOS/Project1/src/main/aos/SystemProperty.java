package aos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SystemProperty {

	private static volatile SystemProperty instance = null; 
	
	private int id;
    private int networkSize;
    
    private List<Node> hosts;
    private List<Node> neighbors;
    private Map<Integer,Node> activeNeighbor;
    
    private SystemProperty(){
    	neighbors = new ArrayList<>();
    	activeNeighbor = new HashMap<>();
    }
	public static SystemProperty getInstance() {
		// Double checking lock for thread safe.
		if(instance == null){
			synchronized (FileLoader.class) {
				if(instance == null){
					instance = new SystemProperty();
				}
			}
		}
		return instance;
	}

    
    public void initActiveNeighbor(){
    	activeNeighbor.clear();
    	for(Node node : neighbors){
    		activeNeighbor.put(node.getNodeId(), node);
    	}
    }
    
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	
	public int getNetworkSize() {
		return networkSize;
	}
	public void setNetworkSize(int networkSize) {
		this.networkSize = networkSize;
	}
	public List<Node> getHosts() {
		return hosts;
	}
	public void setHosts(List<Node> hosts) {
		this.hosts = hosts;
	}
	public List<Node> getNeighbors() {
		return neighbors;
	}
	public void setNeighbors(List<Node> neighbors) {
		this.neighbors = neighbors;
	}
	
	public int getNeighborCount(){
		return neighbors.size();
	}
	
	public Map<Integer, Node> getActiveNeighbor() {
		return activeNeighbor;
	}
	public void setActiveNeighbor(Map<Integer, Node> activeNeighbor) {
		this.activeNeighbor = activeNeighbor;
	}
	
	public int getActiveNeighborCount(){
		return activeNeighbor.size();
	}
	
    public void displayConfiguration(){
		System.out.println(String.format("===== Node %d Configuration =====", id));
        
		System.out.println("----- Neighbor List -----");
        // Print neighbors
        for(Node node : neighbors){
            System.out.println(node.toString());
        }
		System.out.println("===== End of Configuration =====");
    }
	
}
