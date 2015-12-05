import java.io.*;
import java.net.*;

public class Listener {

	public static void main(String[] args) throws Exception {
		
		ServerSocket server = null;
		boolean running = true;
		
		try {
			server = new ServerSocket(50000);
			System.out.println("Server started");
			
			while(running) {
				Socket socket = null; 
				PrintWriter writer = null;
				BufferedReader reader = null;
				try {
					socket = server.accept();
					System.out.println(socket.getInetAddress());
					
					reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					writer = new PrintWriter(new FileWriter("Lab3B.txt"));
					
					String line = reader.readLine();
					while(line != null) {
						System.out.println(line);
						writer.println(line);
						line = reader.readLine();
					}
				} catch(Exception e) {
					e.printStackTrace();
				}
				finally {
					writer.close();
					socket.close();
					System.out.println("Client socket closed");
				}
			}
		}
		finally {
			server.close();
			System.out.println("Server stopped");
		}
	}

}
