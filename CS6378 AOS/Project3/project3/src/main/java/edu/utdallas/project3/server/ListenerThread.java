package edu.utdallas.project3.server;

import java.io.EOFException;
import java.net.SocketException;

/**
 * Thread listen to each neighbors
 * @author zeqing
 *
 */
public class ListenerThread implements Runnable {
    private int myId;
    private int channel;
    private MessageHandler process;
    
    
    public ListenerThread(int myId, int channel, MessageHandler process) {
        this.myId = myId;
        this.channel = channel;
        this.process = process;
    }


    @Override
    public void run() {
        try {
            while(true){
                Message msg;
                msg = process.receiveMessage(channel);
                process.handleMessage(msg, msg.getSrcId(), msg.getTag());
            }
        } catch (SocketException | EOFException e) {
            // Handle Socket Closed Exception
            System.out.println(String.format("[Node %d] Channel %d Terminated. %s", myId, channel, e.toString()));
        } catch (Exception e) {
            e.printStackTrace();
        }        
    }


}
