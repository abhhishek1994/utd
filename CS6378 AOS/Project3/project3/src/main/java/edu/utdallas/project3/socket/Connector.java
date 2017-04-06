package edu.utdallas.project3.socket;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.List;

import edu.utdallas.project3.server.Message;
import edu.utdallas.project3.server.Node;
import edu.utdallas.project3.server.Tag;


/**
 * A collection of accept sockets for maintaining channels
 * @author zeqing
 *
 */
public class Connector {
    ServerSocket listener;
    Socket[] link;
    
    private static volatile Connector instance = null;
    
    private Connector(){
    }
    
    public static Connector getInstance() {
        // Double checking lock for thread safe.
        if(instance == null){
            synchronized (Connector.class) {
                if(instance == null){
                    instance = new Connector();
                }
            }
        }
        return instance;
    }
    

    /**
     * Caveat: processes must be sorted
     * 
     * @param listenPort
     * @param myId
     * @param in
     * @param out
     * @param processes Sorted list of nodes containing nodeId, host address, and port.
     * @throws IOException 
     * @throws ClassNotFoundException 
     * @throws InterruptedException 
     * @throws Exception
     */
    public void connect(
            int listenPort, 
            int myId, 
            ObjectInputStream[] in, 
            ObjectOutputStream[] out, 
            List<Node> processes) throws IOException, ClassNotFoundException, InterruptedException {
        
        int numProc = processes.size();
        link = new Socket[numProc];

        System.out.println(String.format("[Node %d] [Connect: Phase 0] Setup Server at port %d", myId, listenPort));
        listener = new ServerSocket(listenPort);
        
        /* Accept connections from all the smaller processes */
        int numRecved = 0;
        System.out.println(String.format("[Node %d] [Connect: Phase 0] Targets: %s", myId, processes.toString()));
        
        while(numRecved < processes.size() && processes.get(numRecved).getNodeId() < myId){
            System.out.println(String.format("[Node %d] [Connect:Phase 1] Status: numRecved %d, nodeId %d",  myId, numRecved, processes.get(numRecved).getNodeId()));
            Socket socket = listener.accept();
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            
            // Read the first message from new request.
            Message msg = (Message)ois.readObject();
            System.out.println(String.format("[Node %d] [Connect:Phase 1] Receive %s", myId, msg.toString()));
            
            int fromId = msg.getSrcId();
            int fromIndex = Collections.binarySearch(processes, new Node(fromId));
            if(msg.getTag().equals(Tag.HANDSHAKE)){
                link[fromIndex] = socket;  
                in[fromIndex] = ois;
                out[fromIndex] = new ObjectOutputStream(socket.getOutputStream());

                int src = myId, dst = fromId;
                msg = new Message(src, dst, Tag.HANDSHAKE, "Response");
                out[fromIndex].writeObject(msg);
                
                numRecved++;
            }
        }
        System.out.println(String.format("[Node %d] [Connect:Phase 1] Complete.", myId));
        
        /* Contact all the bigger process*/
        while(numRecved < processes.size()){
            Node process = processes.get(numRecved);
            int dstId = process.getNodeId();
            String host = process.getHostName();
            int port = process.getPort();

            int dstIndex = Collections.binarySearch(processes, new Node(dstId));
            
            boolean connected = false;
            while(!connected){
                try{ 
                    System.out.println(String.format("[Node %d] [Connect:Phase 2] Connect to %s:%d", myId, host, port));
                    link[dstIndex] = new Socket(host, port);
                    connected = true;
                } catch (ConnectException e){
                    System.out.println(String.format("[Node %d] [Connect:Phase 2] Connection fail: %s", myId, e.toString()));
                    Thread.sleep(1000);          // Wait 1s to resent
                    System.out.println(String.format("[Node %d] [Connect:Phase 2] Retry connecting...", myId));
                }
            }
            System.out.println(String.format("[Node %d] [Connect:Phase 2] Connection success! to %s:%d", myId, host, port));
            
            out[dstIndex] = new ObjectOutputStream(link[dstIndex].getOutputStream());
            
            /* Send a handshake message to P_i */
            int src = myId, dst = process.getNodeId();
            Message msg = new Message(src, dst, Tag.HANDSHAKE, "Request");
            out[dstIndex].writeObject(msg);
            out[dstIndex].flush();

            // ObjectInputStream constructor will block until the header has been read.  
            in[dstIndex] = new ObjectInputStream(link[dstIndex].getInputStream());
            msg = (Message)in[dstIndex].readObject();
            if(msg.getTag().equals(Tag.HANDSHAKE)){
                System.out.println(String.format("[Node %d] [Connect:Phase 3] InputStream Setup Success! ", myId));
            }
            System.out.println(String.format("[Node %d] [Connect:Phase 3] Send %s", myId, msg.toString()));
            numRecved++;
        }

        System.out.println(String.format("[Node %d] [Connect:Phase 3] Build Channel Done.", myId));
        
    }
    
    
    /**
     * Close all connection to this node.
     * 
     */
    public void closeSockets(){
        try{
            listener.close();
            
            //TODO: May cause io exception.
            for(int i = 0; i < link.length; i++){
                link[i].close();
            }
        } catch(Exception e){
            System.err.println(e);
        }
    }
}
