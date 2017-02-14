package aos;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;


/**
 * 
 * @author Zeqing Li, zxl165030, The University of Texas at Dallas
 *
 */
public class ServerThread extends Thread{
	
    private Socket clientSocket = null;
    private Configuration config = null;
    

    public ServerThread(Socket clientSocket, Configuration config){
        super("ServerThread");
        this.clientSocket = clientSocket;
        this.config = config;
    }

    public void run(){
        try(      
            ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream());
        		
        ) {
        	Message m, response;
        	DiscoveryProtocol proto = DiscoveryProtocol.getInstance();
        	
        	m = (Message)ois.readObject();
        	print("Receive message " + m.toString());

        	response = proto.processInput(m);
	
        	// Send back response.
        	print("Response message " + response.toString());
        	oos.writeObject(response);
        	
        	clientSocket.close();

        	proto.roundSyncrhonization();
        	
        } catch (EOFException e){
        	printErr("EOF reached");
        } catch (ClassNotFoundException e){
        	printErr("Class Not Found");
            System.exit(1);
        } catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    
	public void print(String str){
		//System.out.println(String.format("[node%d] Server: %s", config.getNodeId(), str));
	}
	
	public void printErr(String str){
		//System.err.println(String.format("[node%d] Server: %s", config.getNodeId(), str));
	}
}