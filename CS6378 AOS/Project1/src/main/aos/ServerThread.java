package aos;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;

/**
 * 
 * @author Zeqing Li, zxl165030, The University of Texas at Dallas
 *
 */
public class ServerThread extends Thread {

	private DiscoveryProtocol proto = null;
	
	private DatagramPacket packet = null;
	private DatagramSocket socket = null;

	public ServerThread(DatagramSocket socket, DatagramPacket packet) {
		super("ServerThread");
		this.packet = packet;
		this.socket = socket;

		proto = DiscoveryProtocol.getInstance();
	}

	public void run(){
		Message request, response;		
    	
		try{
			// Parse packet
			byte[] data = packet.getData();
			request = (Message)convertFromBytes(data);
			response = (Message)convertFromBytes(data);
			
			handleResponse(response);
	    	data = convertToBytes(response);
			
			// Send back response.
            SocketAddress address = packet.getSocketAddress();
	    	DatagramPacket packet = new DatagramPacket(data, data.length, address);
	    	socket.send(packet);
	    	
	    	// Process message
	    	proto.processInput(request);
	    	proto.roundSyncrhonization();
	    	
    	} catch (EOFException e){
        	printErr("EOF reached");
        } catch (ClassNotFoundException e){
        	printErr("Class Not Found");
			e.printStackTrace();
        } catch (IOException e) {
			e.printStackTrace();
		} 
	}
	
	
	
	
	private void handleResponse(Message response){
    	print(String.format("Receive message from node %d: %s", response.getSrcNode(), response.toString()));
    	int dstNode = response.getSrcNode();
    	int srcNode = response.getDstNode();
    	response.setSrcNode(srcNode);
    	response.setDstNode(dstNode);
    	response.setStatus(MessageStatus.RESPOND);
    	print(String.format("Response message to node %d: %s", response.getDstNode(), response.toString()));
	}
	
	private byte[] convertToBytes(Message message) throws IOException {
	    try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
	    	ObjectOutputStream out = new ObjectOutputStream(bos)) {
	        out.writeObject(message);
	        return bos.toByteArray();
	    } 
	}
	
	private Message convertFromBytes(byte[] bytes) throws IOException, ClassNotFoundException {
	    try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
	    	ObjectInputStream in = new ObjectInputStream(bis)) {
	        return (Message)in.readObject();
	    } 
	}

	public void print(String str) {
		//SystemProperty config = SystemProperty.getInstance();
		//System.out.println(String.format("[node%d] Server: %s", config.getNodeId(), str));
	}

	public void printErr(String str) {
		//SystemProperty config = SystemProperty.getInstance();
		//System.err.println(String.format("[node%d] Server: %s", config.getNodeId(), str));
	}

}