package aos;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Client thread
 * @author Zeqing Li, zxl165030, The University of Texas at Dallas
 *
 */
public class ClientThread extends Thread{
	
	private ReentrantLock lock;
    private Configuration config = null;
    private DiscoveryProtocol proto = null;
    private Socket socket = null;
    private ObjectOutputStream oos = null;
    private ObjectInputStream ois = null;
    private boolean complete;
    private Queue<Node> queue;
    

    public ClientThread(Configuration config){
        super("ClientThread");
        this.config = config;
        this.queue = new LinkedList<>();
    }

	public void run(){
		proto = DiscoveryProtocol.getInstance();
		lock = proto.getLock();
		
		while(true){
			try {
				synchronized(lock){
					while(!proto.isBroadcastEnabled()){
						print("Wait for notify ...");
						lock.wait();
					}
				}
				print("Broadcast resume...");
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			

			print("Start Broadcasting...");
			complete = false;
			if(proto.isBroadcastEnabled()){
				while(!complete){
					sendToAllNeighbor();
				}
				proto.setBroadcastEnabled(false);
				print("Notifying waiting protocol that broadcast finished!!!!!!!! ");
				ReentrantLock lock = proto.getLock();
				synchronized(lock){
					lock.notifyAll();
				}
			}
			print("Broadcasting Complete...");
		}
		
    }
	
	
	public void sendToAllNeighbor(){
		queue.addAll(config.getNeighbors());
		
		while(!queue.isEmpty()){
			Node node = queue.poll();
			String hostName = node.getHostName();
			int portNumber = node.getPort();
			
			print(String.format("Broadcasting to node%d",node.getNodeId()));
			
			boolean retryEnabled = true;
			while(retryEnabled){
				try {
					//print("Preprate to setup");
					// Open connection to a server
					socket = new Socket();
					//print(String.format("Connecting to %s port %d ...", hostName, portNumber));
					socket.connect(new InetSocketAddress(hostName, portNumber), 5000);  // Timeout 5s
					if(socket.isConnected()){
					//	print("Connection setup complete");
					} else {
					//	print("Connection setup fail");
					}
					// Open IO stream
		            oos = new ObjectOutputStream(socket.getOutputStream());
		            ois = new ObjectInputStream(socket.getInputStream());
		            //print("Setup IO stream");
			        		
					// Prepare message
		            Message m = proto.generateMessage(node.getNodeId());
		            //print("Generating message " + m.toString());
		            
		            // Send message
		            oos.writeObject(m);
		            print(String.format("Send message to node%d %s", node.getNodeId(), m.toString()));
	
					// Confirm message reception 
		            Message response = (Message)ois.readObject();
					
					String rcvInfo = String.format("Receive response from node%d: %s", response.getSrcNode(), response.toString());
					print(rcvInfo);
		            retryEnabled = false;
	
		        } catch (UnknownHostException e) {
		        	printErr(String.format("Don't know about host %s", hostName));
		            System.exit(1);
		        } catch (SocketTimeoutException e) {
		        	printErr(String.format("Connection to host %s. Time out",hostName));
		        } catch (IOException e) {
		        	printErr(String.format("Couldn't get I/O for the connection to %s. Cause [%s]", hostName, e.getMessage())); 
		        	printErr("Retry connecting....");
		            if(e.getMessage().equals("Connection refused (Connection refused)") || e.getMessage().equals("Connection reset")){
		            	try {
							Thread.sleep(2000);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
		            }
		        } catch (ClassNotFoundException e){
		            printErr("Class Not Found");
		            System.exit(1);
		        } finally{
		        	if(socket != null){
		        		try {
							socket.close();
							socket = null; 
						} catch (IOException e) {
							e.printStackTrace();
						}
		        	}
		        	if(ois != null){
	        			try {
							ois.close();
							ois = null;
						} catch (IOException e) {
							e.printStackTrace();
						}
		        	}
		        	if(oos != null){
		        		try {
							oos.close();
							oos = null;
						} catch (IOException e) {
							e.printStackTrace();
						}
		        	}
		        }
			}
		}
		
		complete = true;
	}
	
	public void print(String str){
		//System.out.println(String.format("[node%d] Client: [Round %d] %s", config.getNodeId(), proto.getCurrentRound(), str));
	}
	
	public void printErr(String str){
		//System.err.println(String.format("[node%d] Client: [Round %d] %s", config.getNodeId(),proto.getCurrentRound(), str));
	}
}
