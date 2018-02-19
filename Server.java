import java.io.IOException;

class Server {

	/**
	 * Creates a SocketServer object and starts the server.
	 * 
	 * @param args
	 * @throws InterruptedException
	 */
	
	public static void main(String[] args) throws InterruptedException {

		if ( args.length == 1 && validatePort(args[0]) ) {

			
			int portNumber = Integer.parseInt(args[0]);
			//int portNumber = 8888;

			
			try {
				// initializing the Socket Server
				MultiThreadServer socket = new MultiThreadServer(portNumber);
				socket.start();

			} catch (IOException e) {
				e.printStackTrace();
			}

		} else {
			System.err.printf("invalid port");
			System.exit(1);
		}

	}

	// Validating ports
	public static boolean validatePort(String port){
		if(Integer.parseInt(port) < 1024 || Integer.parseInt(port) > 65535){
			return false;
		}
		return true;
	}

}