/*
  UDPEchoServer.java
  A simple echo server with no error handling
*/

import java.io.*;
import java.net.*;


public class Server implements Runnable {

    public static final int BUFSIZE= 1024;
	public static final int MYPORT= 4950;
	public final Socket socket;
	
	public Server(Socket s) {
		socket = s;
	}

    public static void main(String[] args) throws IOException {

        ServerSocket input = new ServerSocket(MYPORT);

		while (true) {

			Socket socket = input.accept();
			Thread runningThread = new Thread(new Server(socket));
			runningThread.start();

		}
	
	}
	public void run() {
		try {

			String msgFromClient;
			String msgToClient;
			BufferedReader read = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			DataOutputStream stream = new DataOutputStream(socket.getOutputStream());
			msgFromClient = read.readLine();
			System.out.println("Msg: " + msgFromClient);
			msgToClient = msgFromClient;
			stream.writeBytes(msgToClient);
			stream.close();

		} catch (Exception e) {

			System.out.println(e);

		}
	}
}