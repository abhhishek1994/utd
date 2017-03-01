package aos;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * 
 * @author Zeqing Li, zxl165030, The University of Texas at Dallas
 *
 */
public class Server {

	private static int nodeId;
	private static int port;
	private static DiscoveryProtocol proto;

	public Server() {
	}

	/**
	 * @param args
	 *            args[0] - port, args[1] - node id, args[2] - file
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		if (args.length != 3) {
			System.err.println("Usage: java Server <port> <node id> <config file>");
			System.exit(1);
		}

		port = Integer.parseInt(args[0]);
		nodeId = Integer.parseInt(args[1]);
		

		// Init Configuration file
		FileLoader.loadConfig(args[2], nodeId);

		// Set up DiscoveryProtocol
		proto = DiscoveryProtocol.getInstance();
		proto.init();

		Server node = new Server();
		node.startServer();
		node.startClient();
	}

	public void startClient() {
		(new ClientThread()).start();
	}

	public void startServer() {
		Runnable task = () -> {
			DatagramSocket socket = null;
			try{
				socket = new DatagramSocket(port);
				proto.setSocket(socket);
				
				while(true){
					byte[] buf = new byte[1024];
					
					// accept request					
					DatagramPacket packet = new DatagramPacket(buf, buf.length);
					socket.receive(packet);
					
					(new ServerThread(socket, packet)).start();;
				}
				
			} catch (IOException e){
				System.out.println(e.getMessage());
	        	if(e.getMessage().trim().equals("Socket closed")){
	        		
	        	} else{
		            System.out.println("Exception caught when trying to listen on port "
		                + port + " or listening for a connection");
		            e.printStackTrace();
	            }
	    	} finally{
	    		if(socket != null){
	    			socket.close();
	    		}
        		System.exit(0);
	    	}
    	};
    	
    	(new Thread(task)).start();
	}
	

}
