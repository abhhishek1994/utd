package aos;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A round based network discover protocol
 * @author Zeqing Li, zxl165030, The University of Texas at Dallas
 *
 */
public class DiscoveryProtocol {
	
	public static String STATUS_DONE = "Done.";
	public static String STATUS_RESPOND = "Respond.";
	public static String STATUS_SEND = "Send.";
	
	
	private static volatile DiscoveryProtocol instance = null; 
	
	/* Variable intialized in constructor*/
	private ReentrantLock lock;
	private boolean complete;
	private Set<Integer> knownHostNew;
	private Set<Integer> completeHost;           // Host already discover the network
	private HashMap<Integer, Integer> distanceTable;
    private int currentRound;
	private int receiveCount;
	private Set<Integer> receiveSet;    // Keep track of message senders
	private volatile boolean broadcastEnabled;
	private Queue<Message> buffer;
	private boolean finishLastNotify;
	
	/* Variables must set before init() */
	private int nodeId;	
	private int neighborCount;
	private ServerSocket serverSocket = null;

	private DiscoveryProtocol(){
		 lock = new ReentrantLock(true);
		 complete = false;
		 knownHostNew = new HashSet<>();
		 completeHost = new HashSet<>();           // Host already discover the network
		 distanceTable = new HashMap<>();
		 currentRound = 1;
		 receiveCount = 0;
		 receiveSet = new HashSet<>();
		 broadcastEnabled = true;
		 buffer = new LinkedList<>();
		 finishLastNotify = false;
	}
	
	public static DiscoveryProtocol getInstance() {
		// Double checking lock for thread safe.
		if(instance == null){
			synchronized (DiscoveryProtocol.class) {
				if(instance == null){
					instance = new DiscoveryProtocol();
				}
			}
		}
		return instance;
	}
	
	/**
	 * Pre: nodeId
	 * 
	 */
	public void init(){
		// Notify ClientThread can start
		synchronized(lock){
			lock.notifyAll();
		}
		distanceTable.put(nodeId, 0);
	}
	
	/**
	 * Process received message
	 * Assumption: Rm and Rc will not differ greater than 1
	 * Let R denotes round. Rm round count from message, Rc current round.
	 * 1. If Rm > Rc, buffer the message
	 * 2. If Rm == Rc, deliver the message
	 * 
	 * When a node has been broadcasted its vector to all its neighbor,
	 * and has received and processed message from its neighbor,
	 * then its safe to advance current round by 1.
	 * @param m Message 
	 * @return List of node that will be visited
	 */
    public synchronized Message processInput(Message m){
		int round = m.getRound();
    	Message response = new Message(round, nodeId, m.getSrcNode(), new HashSet<>());
    	response.setStatus(STATUS_RESPOND);
    	
    	print("Processing input from node" + m.getSrcNode() + "..............");
		if(currentRound < round){
			buffer.offer(m);
		} else {
			
			// Eliminate duplicate
			if(receiveSet.contains(m.getSrcNode())){
        		return response;
        	}

			receiveCount++;
			receiveSet.add(m.getSrcNode());
			
			if(m.getStatus().equals(STATUS_DONE)){
				HashSet<Integer> completeSets = m.getKnownHost();
	    		completeHost.add(m.getSrcNode());
	    		completeHost.addAll(completeSets);
	    	} else {
	    		// Union 
		    	knownHostNew.addAll(m.getKnownHost());
	    	}
			
		}
        print("Processing input from node" + m.getSrcNode() + " Complete!");
        return response;
    }
    
    /**
     * Generate broadcast message
     * @return
     */
    public Message generateMessage(int dstNode){
    	Message broadcastMessage;
    	if(complete){
    		completeHost.add(nodeId);
    		broadcastMessage = new Message(currentRound, nodeId, dstNode, completeHost); 
    		broadcastMessage.setStatus(DiscoveryProtocol.STATUS_DONE);
    	} else {
    		broadcastMessage = new Message(currentRound, nodeId, dstNode, distanceTable.keySet()); 
    		broadcastMessage.setStatus(DiscoveryProtocol.STATUS_SEND);
    	}
    	return broadcastMessage;
    }


	public boolean isBroadcastEnabled() {
		return broadcastEnabled;
	}


	public void setBroadcastEnabled(boolean broadcastEnabled) {
		this.broadcastEnabled = broadcastEnabled;
	}

	public boolean isComplete() {
		return complete;
	}

	public void setComplete(boolean complete) {
		this.complete = complete;
	}

	public ServerSocket getSocket() {
		return serverSocket;
	}


	public void setSocket(ServerSocket socket) {
		this.serverSocket = socket;
	}
	
	

	public int getCurrentRound() {
		return currentRound;
	}

	public void setCurrentRound(int currentRound) {
		this.currentRound = currentRound;
	}

	public int getNodeId() {
		return nodeId;
	}
	
	public void setNodeId(int nodeId) {
		this.nodeId = nodeId;
	}

	public int getNeighborCount() {
		return neighborCount;
	}

	public void setNeighborCount(int neighborCount) {
		this.neighborCount = neighborCount;
	}
	
	public ReentrantLock getLock() {
		return lock;
	}

	public void setLock(ReentrantLock lock) {
		this.lock = lock;
	}


	public synchronized void roundSyncrhonization() {
		if(receiveCount < neighborCount){
			return;
		} else {
			// Wait for broadcast finish
			synchronized(lock){
	    		while(broadcastEnabled){
	    			print("Waiting for broadcast complete.");
    				try {
						lock.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
    			}
    		}
			
			if(!complete){
				// Its time to do calculation
				Set<Integer> knownHostOld = distanceTable.keySet();
				Set<Integer> difference = new HashSet<>(knownHostNew);
				print("KnownHostOld = " + knownHostOld.toString());
				print("knownHostNew = " + knownHostNew.toString());
				difference.removeAll(knownHostOld);
				
				print("Diff = " + difference.toString());
				if(difference.isEmpty()){
					// Change state to complete, broadcasting complete message
					complete = true;
				} else {
					// Add to distanceTable
					print("Update distance table.....");
					for(Integer i : difference){
						distanceTable.put(i, currentRound);
					}
				}
			}
			
			currentRound++;
    		print("Advanced to ROUND " + currentRound);
    		
    		if(currentRound == distanceTable.keySet().size() * 2 + 2){
			
				printDistance();
    			print("*******************************Program terminated***********************************");
    			
    			try {
					serverSocket.close();
					System.exit(0);
				} catch (IOException e) {
					e.printStackTrace();
				}

    		}
    		
    		
    		// Global Termination
    		// Pre: receiveCount equals neighborCount and Broadcast finished
    		Set<Integer> knownHost = distanceTable.keySet();
			print("KnownHostOld = " + knownHost.toString());
			print("CompleteHost = " + completeHost.toString());

    		
    		receiveCount = 0;
    		receiveSet.clear();
    		knownHostNew.clear();
    		broadcastEnabled = true;
    		
    		// Notify client thread to broadcast
    		synchronized(lock){
    			lock.notifyAll();
    		}
    		print("Wake up client thread!!!!!!!!!");
    		
    		
    		// Deliver the message
    		while(!buffer.isEmpty()){
    			processInput(buffer.poll());
    		}
    	}		
	}
	
	public void printDistance(){
		
		
		
		int max = 0;
		for(Map.Entry<Integer, Integer> entry : distanceTable.entrySet()){
			max = Math.max(max, entry.getValue());
		}
		
		List<List<Integer>> res = new ArrayList<>(max + 1);
		for(int i = 0; i < max + 1; i++){
			res.add(new ArrayList<Integer>());
		}
		
		for(Map.Entry<Integer, Integer> entry : distanceTable.entrySet()){
			res.get(entry.getValue()).add(entry.getKey());
		}
		
		for(int i = 0; i < max + 1; i++){
			Collections.sort(res.get(i));
		}
		
		System.out.println("Output for node" + nodeId);
		for(int i = 1; i <= max; i++){
			System.out.println(String.format("--- %d Hops => %s", i, Arrays.toString(res.get(i).toArray())));
		}
	}
	
	public void print(String str){
		//System.out.println(String.format("[node%d] Server: [Round %d] %s", nodeId, currentRound, str));
	}
	
	public void printErr(String str){
		//System.err.println(String.format("[node%d] Server: [Round %d] %s", nodeId, currentRound, str));
	}
	
}