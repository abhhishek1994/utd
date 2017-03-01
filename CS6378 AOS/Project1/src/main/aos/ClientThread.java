package aos;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Client thread
 * 
 * @author Zeqing Li, zxl165030, The University of Texas at Dallas
 *
 */
public class ClientThread extends Thread {

	private SystemProperty config = null;
	private DiscoveryProtocol proto = null;
	private DatagramSocket socket = null;
	
	private boolean broadcastFinished;

	// private Socket socket = null;

	public ClientThread() {
		super("ClientThread");
		config = SystemProperty.getInstance();
		this.proto = DiscoveryProtocol.getInstance();
	}

	public void run() {
		ReentrantLock lock = proto.getLock();

		while (true) {
			try {
				while (!proto.isBroadcastEnabled()) {
					print("Waiting for server complete ...");
					synchronized (lock) {
						lock.wait();
					}
				}
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}

			print("Start Broadcasting...");
			broadcastFinished = false;

			while (!broadcastFinished) {
				sendToAllNeighbor();
			}
			proto.setBroadcastEnabled(false);
			print("Broadcasting Complete. notifying waiting protocol that broadcast finished!!!!!!!! ");
			synchronized (lock) {
				lock.notifyAll();
			}
		}

	}
	
	/**
	 * Multiple UDP unicast to send request to all neighbor 
	 */
	public void sendToAllNeighbor() {
		Node node;
		int portNumber;
		String hostName = "";
		
		try {
			
			for(Map.Entry<Integer, Node> entry : config.getActiveNeighbor().entrySet()){
				node = entry.getValue();
				portNumber = node.getPort();
				hostName = node.getHostName();

				print(String.format("(1/3)Broadcasting to node%d", node.getNodeId()));

				boolean retryEnabled = true;
				while (retryEnabled) {

					// Get a datagram socket
					socket = new DatagramSocket();

					// Prepare message
					Message request = proto.generateMessage(node.getNodeId());
					byte[] buf = convertToBytes(request);

					// Send request
					SocketAddress address = new InetSocketAddress(hostName, portNumber);
					DatagramPacket packet = new DatagramPacket(buf, buf.length, address);
					socket.send(packet);
					print(String.format("(2/3)Sent message to node%d %s", node.getNodeId(), request.toString()));
					
					// Setup blocking timeout
					socket.setSoTimeout(50); // Wait for 2s

					// Get response
					buf = new byte[1024];
					packet = new DatagramPacket(buf, buf.length);
					socket.receive(packet);

					byte[] data = packet.getData();
					Message response = convertFromBytes(data);
					print(String.format("(3/3)Response received from node%d: %s", response.getSrcNode(),
							response.toString()));

					retryEnabled = false;

				}
			}

			broadcastFinished = true;
		} catch (UnknownHostException e) {
			printErr(String.format("Don't know about host %s", hostName));
			System.exit(1);
		} catch (SocketTimeoutException e) {
			printErr(String.format("Connection to host %s. time out. Retrying connecting...", hostName));
		} catch (IOException e) {
			printErr(String.format("Couldn't get I/O for the connection to %s. Cause [%s]", hostName, e.getMessage()));
			e.printStackTrace();
			System.exit(1);
			printErr("Retry connecting....");
		} catch (ClassNotFoundException e) {
			printErr("Class Not Found");
			System.exit(1);
		} finally {
			if (socket != null) {
				socket.close();
			}
		}
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
			return (Message) in.readObject();
		}
	}

	public void print(String str) {
		// System.out.println(String.format("[node%d] Client: [Round %d] %s",
		// config.getNodeId(), proto.getCurrentRound(), str));
	}

	public void printErr(String str) {
		 //System.err.println(String.format("[node%d] Client: [Round %d] %s",
		 //config.getNodeId(),proto.getCurrentRound(), str));
	}

}
