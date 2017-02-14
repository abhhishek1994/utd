package aos;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * For the purpose of reading and storing configurations. 
 * @author Zeqing Li, zxl165030, The University of Texas at Dallas
 *
 */
public class Configuration {
    
    private Path path;
    private Path file;
    private int nodeId;
    private int networkSize;
    private ArrayList<Node> hosts;
    private ArrayList<Node> neighbors;
    
    
    public Configuration(String relativePath, int nodeId){
        this.nodeId = nodeId;
        this.path = Paths.get(relativePath);
        this.file = path.toAbsolutePath();
        System.out.println(file.toString());
        
        // Load file
        load();
        
        // Printout configuration
        printConfiguration();
    }
    
    public ArrayList<Node> getHosts(){
        return hosts;
    }

    public int getNetworkSize() {
		return networkSize;
	}
    
	public int getNodeId() {
		return nodeId;
	}

	public void setNodeId(int nodeId) {
		this.nodeId = nodeId;
	}

	public ArrayList<Node> getNeighbors() {
		return neighbors;
	}

	public void setNeighbors(ArrayList<Node> neighbors) {
		this.neighbors = neighbors;
	}

	public void setNetworkSize(int networkSize) {
		this.networkSize = networkSize;
	}

	private void load(){
        // Checking File Accessibility
//        boolean isRegularExecutableFile = 
//            Files.isRegularFile(file) &
//            Files.isReadable(file) & 
//            Files.isExecutable(file);
        
        Charset charset = Charset.forName("UTF-8");
        try (BufferedReader reader = Files.newBufferedReader(file, charset)) {
            String line = null;
            int n = 0;
            while ((line = reader.readLine()) != null) {
                line = line.replaceAll("#.*","");  // Erase everything after a comment.
                line = line.trim();                // Trim leading and trailing spaces.
                if(line.length() == 0)
                    continue;
                networkSize = Integer.parseInt(line);
                break;
            }
            
            n = networkSize;
            hosts = new ArrayList<>(networkSize);
            
            // Load host list.
            while ((line = reader.readLine()) != null && n != 0) {
                line = line.replaceAll("#.*","");  // Erase everything after a comment.
                line = line.trim();                // Trim leading and trailing spaces.
                if(line.length() == 0)
                    continue;
                
                
                //System.out.println(String.format("[host info: %s]", line));
                String[] hostInfo = line.split("\\s+");
                
                // hostInfo[0] - node id, hostInfo[1] - host addr, hostInfo[2] - host port
                Node host = new Node(Integer.parseInt(hostInfo[0]), hostInfo[1], hostInfo[2]);
				hosts.add(host);
                n--;
            }
            if(n != 0){
                throw new IOException("Insufficent valid lines in config file.");
            }
            
            n = networkSize;
            
            // Load neighbors
            while ((line = reader.readLine()) != null && n != 0) {
                line = line.replaceAll("#.*","");  // Erase everything after a comment.
                line = line.trim();                // Trim leading and trailing spaces.
                if(line.length() == 0)
                    continue;
                String[] neighborIds = line.split("\\s+");
                int currentId = Integer.parseInt(neighborIds[0]);
                
                if( currentId == nodeId){
                    neighbors = new ArrayList<>();
                    for(int i = 1; i < neighborIds.length; i++){
                        int id = Integer.parseInt(neighborIds[i]);
                        Node node = hosts.get(id);
                        neighbors.add(node);
                    }
                }
                n--;
            }
            if(n != 0){
                throw new IOException("Insufficent valid lines in config file.");
            }
            if(neighbors == null){
                throw new NullPointerException("Expect adjacent neighbors for node " + nodeId);
            }
            
        } catch (IOException x) {
            System.err.format("IOException: %s%n", x);
        } catch (NullPointerException e){
            System.err.println(e.getMessage());
        }
    }

    public void test(String[] args) {
      
        if (args.length < 2) {
            System.out.println("usage: ConfigFileTest <file> <nodeId>");
            System.exit(-1);
        }
        
        
        nodeId = Integer.parseInt(args[1]);
        
        Configuration config = new Configuration(args[0], nodeId);
        config.load();
    }
    
    public void printConfiguration(){
		System.out.println(String.format("===== Node %d Configuration =====", nodeId));
        // Print hosts 
        System.out.println("----- Host List -----");
		for(Node node : hosts){
            System.out.println(node.toString());
        }
        
		System.out.println("----- Neighbor List -----");
        // Print neighbors
        for(Node node : neighbors){
            System.out.println(node.toString());
        }
		System.out.println("===== End of Configuration =====");
    }
  
    public boolean isValidLine(String line){
      // Skipping empty line and commments
            if(line.length() == 0 || line.charAt(0) == '#')
                return false;
            int i = line.charAt(0) - '0';
            if(0 <= i && i <= 9)
                return true;
            else 
                return false;
                
    }
}