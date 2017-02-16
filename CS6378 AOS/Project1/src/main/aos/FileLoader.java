package aos;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * For the purpose of reading and storing configurations. 
 * @author Zeqing Li, zxl165030, The University of Texas at Dallas
 *
 */
public final class FileLoader {

	public static void loadConfig(String relativePath, int nodeId){
		
        
        // Checking File Accessibility
//        boolean isRegularExecutableFile = 
//            Files.isRegularFile(file) &
//            Files.isReadable(file) & 
//            Files.isExecutable(file);
		SystemProperty system = SystemProperty.getInstance();
		Path file = Paths.get(relativePath).toAbsolutePath();
		System.out.println(file.toString());
		
		
		int networkSize = 0;
		List<Node> hosts = new ArrayList<>();
		List<Node> neighbors = new ArrayList<>();
        
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
            
        } catch (IOException x) {
            System.err.format("IOException: %s%n", x);
        } catch (NullPointerException e){
            System.err.println(e.getMessage());
        }
        
        system.setId(nodeId);
        system.setNetworkSize(networkSize);
        system.setHosts(hosts);
        system.setNeighbors(neighbors);
        system.initActiveNeighbor();
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