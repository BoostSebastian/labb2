import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Arrays;

public class ClientHandler implements Runnable {

	// Change for debugging console
	private static boolean DEBUGGING = true;

	private Socket client;

	// input and output streams
	private DataInputStream inputStream = null;
	private DataOutputStream outputStream = null;

	// buffer
	private byte[] buf = null;
	private int bufferSize = 1024;

	public ClientHandler(Socket client) {

		buf = new byte[bufferSize];
		this.client = client;

		try {
			// Send and receive
			inputStream = new DataInputStream(this.client.getInputStream());
			outputStream = new DataOutputStream(this.client.getOutputStream());

		} catch (IOException e) {
			System.err.println("Problem with the stream!");
			e.printStackTrace();
		} catch (Exception e) {
			e.getMessage();
		}

	}

	@Override
	public void run() {



		try {





			System.out.println("Thread started with name: " + Thread.currentThread().getName());
			readResponse();








		} catch (IOException e) {

			System.out.println("Problem with the stream!");
			e.printStackTrace();

		} catch (Exception e) {
			e.getMessage();
		}finally {

			closeConection();

		}

		
	}

	private void readResponse() throws IOException, InterruptedException {

		try {

			// Streams
			BufferedReader request = new BufferedReader(new InputStreamReader(client.getInputStream()));
			BufferedWriter response = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));

			String requestHeader = "";
			String temp = ".";

			// Read stream
			while (!temp.equals("")) {
				temp = request.readLine();
				System.out.println(temp);
				requestHeader += temp + "\n";
			}

			// Get the method from HTTP header
			StringBuilder sb = new StringBuilder();															// stringbuilder to obtain page
			String file = requestHeader.split("\n")[0].split(" ")[1].split("/")[1];							// header split to obtain file

			if(DEBUGGING){
				System.out.println("------SPLITED FILES-------");
				System.out.println();
				System.out.println(Arrays.toString(requestHeader.split("\n")[0].split(" ")));					// print array
				System.out.println(Arrays.toString(requestHeader.split("\n")[0].split(" ")[1].split("/")));		// print array
				System.out.println();
			}

			// Check for GET 
			if (requestHeader.split("\n")[0].contains("GET") && checkURL(file)) {

				// Get the correct page
				constructResponseHeader(200, sb);
				response.write(sb.toString());
				response.write(getData(file));
				sb.setLength(0);
				response.flush();

			// CHeck for PUT
			} else if (requestHeader.split("\n")[0].contains("PUT") && checkURL(file)) {

				System.out.println("Do not support PUT");

			} else {

				// Enter the error code
				// 404 page not found
				constructResponseHeader(404, sb);
				response.write(sb.toString());
				sb.setLength(0);
				response.flush();

			}

			// CLOSING
			request.close();
			response.close();
			client.close();
			return;

		} catch (Exception e) {

		}

	}




	// Check the URL
	private static boolean checkURL(String file) {

		File myFile = new File(file);

		if(DEBUGGING){
			System.out.println("----------FILE------------");
			System.out.println();
			System.out.println("name: " + file + " / exist: " + (myFile.exists() && !myFile.isDirectory()));
			System.out.println();
		}

		

		return myFile.exists() && !myFile.isDirectory();

	}





	// Response Header
	private static void constructResponseHeader(int responseCode, StringBuilder sb) {

		if (responseCode == 200) {

			sb.append("HTTP/1.1 200 OK\r\n");
			sb.append("Date:" + getTimeStamp() + "\r\n");
			sb.append("Server:localhost\r\n");
			sb.append("Content-Type: text/html\r\n");
			sb.append("Connection: Closed\r\n\r\n");

		} else if (responseCode == 404) {

			sb.append("HTTP/1.1 404 Not Found\r\n");
			sb.append("Date:" + getTimeStamp() + "\r\n");
			sb.append("Server:localhost\r\n");
			sb.append("\r\n");

		}

	}







	private static String getData(String file) {

		File myFile = new File(file);
		String responseToClient = "";
		BufferedReader reader;

		if(DEBUGGING){

			System.out.println("-----ABSOLUTE PATH--------");
			System.out.println();
			System.out.println(myFile.getAbsolutePath());
			System.out.println();

		}

		try {
			reader = new BufferedReader(new FileReader(myFile));
			String line = null;

			// read until en of html-tag
			while (!(line = reader.readLine()).contains("</html>")) {
				responseToClient += line;
			}

			responseToClient += line;

			if(DEBUGGING){

				System.out.println("---------RESPONSE---------");
				System.out.println();
				System.out.println(responseToClient);
				System.out.println();

	
			}

			reader.close();

		} catch (Exception e) {

		}
		return responseToClient;
	}
















	// TimeStamp
	private static String getTimeStamp() {
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy h:mm:ss a");
		String formattedDate = sdf.format(date);
		return formattedDate;
	}

	// Closing down streams and connection
	private void closeConection() {
		try {
			inputStream.close();
			outputStream.close();
			this.client.close();
			System.out.println("Client connection ended..");
		} catch (IOException e) {
			System.err.println("Issue with closing down..");
			e.printStackTrace();
		}
	}

}