package edu.utdallas.project3.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import edu.utdallas.project3.socket.Linker;
import edu.utdallas.project3.tools.MutexConfig;

public class Process implements MessageHandler{
    protected int numProc, myId;
    protected Linker linker;
   
    
    /**
     * Central repository to retrieve and store helpful settings
     */
    protected MutexConfig config;
    
    /**
     * Snapshot for termination detection
     */
    protected int[] snapshotForMap;
    
    
    /**
     * Snapshot for vector clock
     */
    protected int[] snapshotForVector;
    
    /**
     * RecvCamera produce 1 snapshot, release available semaphore once.
     */
    protected Semaphore available;
    
    /**
     * Permission to take snapshot
     * Control by node 0.
     */
    protected Semaphore snapshotPermission;
    
    protected Semaphore mutex;
    
    /**
     * Snapshot history, supplied by RecvCamera
     */
    protected List<int[]> snapshotList;
    
    /**
     * Current snapshot index node 0 are collecting
     */
    protected int snapshotIndex;
    
    public Process(Linker initLinker, MutexConfig config){
        this.config = config;
        
        this.linker = initLinker;
        this.myId = linker.getMyId();
        this.numProc = linker.getNeighbors().size();  
        this.available = new Semaphore(0);
        this.snapshotPermission = new Semaphore(1);
        this.snapshotList = new ArrayList<>();
        this.snapshotIndex = 0;
        this.mutex = new Semaphore(1);
    }
    
    /**
     * Default message handler.
     * Accept and process application message only 
     * 
     * Only one thread can handle message at the same time.
     * 
     * @throws IOException 
     */
    public synchronized void handleMessage(Message msg, int srcId, Tag tag) throws IOException{
        if(tag == Tag.APP) 
            System.out.println("This is application message");
        else 
            System.out.println(String.format("[Node %d] [Request] content=%s", myId, msg.toString()));
    }
    
    /**
     * Send message to a specific node
     * @param dstId
     * @param tag
     * @param content
     * @throws IOException
     */
    public synchronized void sendMessage(int destination, Tag tag, String content) throws IOException{
        Message message = new Message(myId, destination, tag, content);
        linker.sendMessage(destination, message);
    }
    
    public synchronized void sendMessage(int destination, Message message) throws IOException{
        linker.sendMessage(destination, message);
    }
    
    /**
     * Broadcast messages to all its neighbors
     * @param tag
     * @param content
     * @throws IOException
     */
    public synchronized void sendToNeighbors(Tag tag, String content) throws IOException{
        List<Node> neighbors = linker.getNeighbors();
        linker.multicast(neighbors, tag, content);
    }
    
    /**
     * Retrieve message for a specific node
     * @throws IOException 
     */
    public Message receiveMessage(int fromId) throws IOException{
        try{
            Message message = linker.receiveMessage(fromId);
            return message;
        } catch (ClassNotFoundException e){
            e.printStackTrace();
            return null;
        } catch (IOException e){
            linker.close();
            throw e;
        } 
    }
    
    public synchronized void procWait(){
        try{
            wait();
        } catch (InterruptedException e){
            System.err.println(e);
        }
    }
    
    /**
     * Request permission to run globalState() function
     * @throws InterruptedException
     */
    public synchronized void requestSnapshotPermission() throws InterruptedException{
        System.out.println(String.format("[Node %d] [SNAPSHOT] Request Permission.", myId));
        snapshotPermission.acquire();
    }
    
    /**
     * Grant permission when at least one node is active.
     * Control by SpanTree
     */
    protected synchronized void grantSnapshotPermisson(){
        System.out.println(String.format("[Node %d] [SNAPSHOT] Grant Permission.", myId));
        snapshotPermission.release();
    }
    /**
     * String representation of process
     */
    public synchronized String toString(){
        StringBuilder sb = new StringBuilder();
        String newline = "\n";
        String meta = String.format("[Node Id = %d]", myId);
        sb.append(meta).append(newline);
        if(numProc != 0){
            String neighborInfo = String.format("Neighbor Info: \nnumProc: %d", numProc);
            sb.append(neighborInfo).append(newline);
            List<Node> neighbors = linker.getNeighbors();
            for(Node node : neighbors){
                sb.append(node.toString()).append(newline);
            }
        }
        return sb.toString();
    }
}
