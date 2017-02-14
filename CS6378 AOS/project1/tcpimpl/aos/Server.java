package aos;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * 
 * @author Zeqing Li, zxl165030, The University of Texas at Dallas
 *
 */
public class Server {
    
    private static int nodeId;
    private static int portNumber;
    private Configuration config;
    private DiscoveryProtocol proto;
    
    public Server(Configuration config){
    	this.config = config;
    }

    /**
     * @param args args[0] - port, args[1] - node id, args[2] - file
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        
        if (args.length != 3) {
            System.err.println("Usage: java Server <port> <node id> <config file>");
            System.exit(1);
        }
        
        portNumber = Integer.parseInt(args[0]);
        nodeId = Integer.parseInt(args[1]);
        
        //System.out.println(String.format("[node%d] start ................................................", nodeId));
        
        // Parse Configuration file
        Configuration config = new Configuration(args[2], nodeId);
        
        // Set up DiscoveryProtocol
    	DiscoveryProtocol proto = DiscoveryProtocol.getInstance();
    	proto.setNodeId(config.getNodeId());
    	proto.setNeighborCount(config.getNeighbors().size());
    	proto.init();
    	
    	Server node = new Server(config);
    	node.setProto(proto);
    	node.startServer();
    	node.startClient();
    }
    
    
    /**
     * Open a socket and listen to the given port
     */
    public void listen(int port){
        try (ServerSocket serverSocket = new ServerSocket(port)){
        	proto.setSocket(serverSocket);
        	
            while(true){
                new ServerThread(serverSocket.accept(), config).start();
            }
            
        } catch (IOException e) {
        	if(e.getMessage().equals("Socket closed")){
        		System.exit(0);
        	}
            System.out.println("Exception caught when trying to listen on port "
                + portNumber + " or listening for a connection");
            System.out.println(e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        } 
    }
    
    public void startClient(){
    	(new ClientThread(config)).start();
    }
    
    public void startServer(){
    	(new Thread(){
    		@Override 
    		public void run(){
    	        listen(portNumber);
    		}
    	}).start();
    }
    
    public void setProto(DiscoveryProtocol proto){
    	this.proto = proto;
    }
}
