
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class MultiThreadServer {

	private ServerSocket serverSocket;
	private int port;

	public MultiThreadServer(int port) {
		this.port = port;
	}

    public void start() throws IOException, InterruptedException {

		serverSocket = new ServerSocket(port);
		System.out.println();
		System.out.println();
		System.out.println("Starting the socket server at port: " + port);

		Socket client = null;

		while (true) {
			System.out.println("Waiting for clients...");
			client = serverSocket.accept();
			System.out.println("The following client has connected: ( " + client.getInetAddress().getCanonicalHostName() + " / " + client.getInetAddress().getHostAddress() + " )");
			// A client has connected to this server. Send welcome message
			Thread thread = new Thread(new ClientHandler(client));
			thread.start();
		}

	}
	



	


}