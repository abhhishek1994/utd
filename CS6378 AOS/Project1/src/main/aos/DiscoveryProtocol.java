package aos;

import java.net.DatagramSocket;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
	
	
	private static volatile DiscoveryProtocol instance = null; 
	private SystemProperty config = null;
	private Profiler profiler = null;
	
	/* Variable initialized in constructor*/
	private ReentrantLock lock;
	private Set<Integer> knownHostsNew;
	private Set<Integer> receivedMessageSet;    
	private Map<Integer, Integer> distanceTable;
	
	private volatile boolean terminated;
	private volatile boolean broadcastEnabled;
	
	private Queue<Message> bufferQueue;
	private Queue<Integer> endedQueue;

	private int id;	
	private int neighborCount;
    private int currentRound;
	private int receiveCount;
	
	/* Variables must set before init() */
	private DatagramSocket serverSocket = null;

	private DiscoveryProtocol(){
		profiler = Profiler.getInstance();
		config = SystemProperty.getInstance(); 
		id = config.getId();
		neighborCount = config.getNeighborCount();
		
		lock = new ReentrantLock(true);
		knownHostsNew = new HashSet<>();
		distanceTable = new HashMap<>();
		
		currentRound = 1;
		receiveCount = 0;
		
		receivedMessageSet = new HashSet<>();
		bufferQueue = new ArrayDeque<>(neighborCount);
		endedQueue = new ArrayDeque<>(neighborCount);
		

		terminated = false;
		broadcastEnabled = true;

		profiler.startAnalyzing();
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

	public void init(){
		// Notify ClientThread can start
		synchronized(lock){
			lock.notifyAll();
		}
		distanceTable.put(id, 0);
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
    public synchronized void processInput(Message m){
    	print(String.format("Processing start (node%d) %s......", m.getSrcNode(), m.toString()));
    	
		if(currentRound < m.getRound()){
			print(String.format("Buffer message %s", m.toString()));
			bufferQueue.offer(m);
		} else if(receivedMessageSet.contains(m.getSrcNode())) {
			print(String.format("Duplcate message %s", m.toString()));
			
		} else {
			receiveCount++;
			receivedMessageSet.add(m.getSrcNode());
			
			if(m.getStatus().equals(MessageStatus.DONE)){
				endedQueue.offer(m.getSrcNode());
	    	} else {
		    	knownHostsNew.addAll(m.getKnownHost());
	    	}
			
		}
        print(String.format("Processing complete (node%d) %s. Progress: %d/%d. %s",
        		m.getSrcNode(), m.toString(), receiveCount, neighborCount, receivedMessageSet.toString()));
    }
    
    /**
     * Generate broadcast message
     * @return
     */
    public Message generateMessage(int dstNode){
    	Message broadcastMessage = new Message(currentRound, id, dstNode, distanceTable.keySet()); 
    	String status = terminated ?  MessageStatus.DONE : MessageStatus.SEND;
    	broadcastMessage.setStatus(status);
    	return broadcastMessage;
    }


	public synchronized void roundSyncrhonization() {
		print(String.format("Synchronization [recv = %d, active = %d]", receiveCount, neighborCount));
		if(receiveCount < neighborCount){
			return;
		} else {
			// Wait for broadcast task finishing
			synchronized(lock){
				try {
					while(broadcastEnabled){
		    			print("Waiting for broadcast complete.");
						lock.wait();
					} 
    			} catch (InterruptedException e) {
					e.printStackTrace();
				}
    		}
			
			detectTermination();
			handleMessage();
			resetCounters();
			
    		// Notify client thread to broadcast
    		synchronized(lock){
    			lock.notifyAll();
    		}
    		
    		print("Message process complete, notifying client to broadcast.");
    		
    		
    		// Deliver the message
    		while(!bufferQueue.isEmpty()){
    			processInput(bufferQueue.poll());
    			roundSyncrhonization();
    		}
    	}		
	}
	
	public synchronized void detectTermination(){
		Map<Integer,Node> active = config.getActiveNeighbor();
		while(!endedQueue.isEmpty()){
			active.remove(endedQueue.poll());
		}
	}
	
	public synchronized void handleMessage(){
		Set<Integer> knownHostOld = distanceTable.keySet();
		Set<Integer> difference = new HashSet<>(knownHostsNew);
		difference.removeAll(knownHostOld);
		print(String.format("%s - %s = %s", 
				knownHostsNew.toString(), knownHostOld.toString(), difference.toString()));
		
		for(Integer i : difference){	distanceTable.put(i, currentRound); 	}
		
		currentRound++;
		print("Advanced to ROUND " + currentRound);
		
		/*
		 *  Global Termination
		 *  1) Leader termination: Set terminateEnable from previous Round
		 *  2) Follower termination: No active neighbor after receiving message from current Round.
		 */
		// 
		if(terminated || config.getActiveNeighbor().isEmpty()){
			shutdown();
		}
		
		if(difference.isEmpty()){
			print("Stopping at this round");
			// Change state to complete, broadcasting complete message
			terminated = true;
		} 
	}
	
	public void resetCounters(){
		receiveCount = 0;
		receivedMessageSet.clear();
		knownHostsNew.clear();
		broadcastEnabled = true;
		neighborCount = config.getActiveNeighbor().size();
	}
	
	public void shutdown(){
		printDistance();
		profiler.endAnalyzing();
		profiler.displayAnalysisResult();
		
    	while(!serverSocket.isClosed()){
    		print("Closing socket...");
    		serverSocket.close();
    	}
    	System.exit(0);
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
		
		synchronized(this){
			StringBuilder sb = new StringBuilder();
			sb.append(String.format("******************************* Output for node%d ***********************************\n", id));
			for(int i = 1; i <= max; i++){
				sb.append((String.format("--- %d Hops => %s\n", i, Arrays.toString(res.get(i).toArray()))));
			}
			sb.append("*******************************Program terminated***********************************");
			System.out.println(sb.toString());
		}
		
	}
	
	public boolean isBroadcastEnabled() {
		return broadcastEnabled;
	}

	public void setBroadcastEnabled(boolean broadcastEnabled) {
		this.broadcastEnabled = broadcastEnabled;
	}

	public DatagramSocket getSocket() {
		return serverSocket;
	}
	
	public void setSocket(DatagramSocket socket) {
		this.serverSocket = socket;
	}
	
	public ReentrantLock getLock() {
		return lock;
	}

	public void setLock(ReentrantLock lock) {
		this.lock = lock;
	}


	
	public void print(String str){
		//System.out.println(String.format("[node%d] Server: [Round %d] %s", nodeId, currentRound, str));
	}
	
	public void printErr(String str){
		//System.err.println(String.format("[node%d] Server: [Round %d] %s", nodeId, currentRound, str));
	}
	
}