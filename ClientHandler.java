import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.*;
import java.io.File;
import java.io.FilenameFilter;

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

	List <String> maps = new ArrayList<String>();

	private String rederectURL = "/webb/temp.htmlg";

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
			StringBuilder sb = new StringBuilder();						// stringbuilder to obtain page
			String file = requestHeader.split("\n")[0].split(" ")[1];	// header split to obtain filepath

			if(DEBUGGING){
				System.out.println();
				System.out.println("------SPLITED FILES-------");
				System.out.println();
				System.out.println(Arrays.toString(requestHeader.split("\n")[0].split(" ")));					// print array
				System.out.println(Arrays.toString(requestHeader.split("\n")[0].split(" ")[1].split("/")));		// print array
				System.out.println();
			}







			

			// Check for GET 
			if (requestHeader.split("\n")[2].contains("GET")) {

				if(checkURL(file)){

					String path = buildURL(file); 									// build url to obtain file
					String type = checkTypeOfFile(path); 							// check type of file
					String ext = getFileExtension(path);							// extension
					//boolean _root = new File(path).isDirectory();					// check if root directory

					if(restrictedDirectory(path)) {

						if (type == "html"){

							// Get the correct page
							constructResponseHeader(200, sb, "text/" + ext);		// build header
							response.write(sb.toString());							// send header
							response.write(getData(path));							// send data
							sb.setLength(0);				 						// reset
							response.flush();										// flush
		
						} else if (type == "image") {

							constructResponseHeader(200, sb, "image/" + ext);		// build header
							response.write(sb.toString());							// send header
		
							File img_file = new File(path);
							FileInputStream fis = new FileInputStream(img_file);
							byte[] data = new byte[(int) img_file.length()];
							fis.read(data);
							fis.close();
		
							DataOutputStream binaryOut = new DataOutputStream(outputStream);
							binaryOut.writeBytes("HTTP/1.0 200 OK\r\n");
							binaryOut.writeBytes("Content-Type: image/png\r\n");
							binaryOut.writeBytes("Content-Length: " + data.length);
							binaryOut.writeBytes("\r\n\r\n");
							binaryOut.write(data);
		
							binaryOut.close();
		
						} else {
							System.out.println("Do not support this format");
						}

					} else {

						// Enter the forbidden response 
						// 403 page not found
						constructResponseHeader(403, sb, "");
						response.write(sb.toString());
						sb.setLength(0);
						response.flush();

					}

				} else { // SEE IF WE CAN FIND THE FILE














					try {
				
						File[] fileList = getFileList("/Users/sebastianthorngren/Desktop/labb2", "temp.html");
		
						for(File file_ : fileList) {
							//System.out.println(file_.getName());
						}
		
					} catch (Exception e) {
						e.printStackTrace();
					}












				}

				























				

				

			// Check for PUT
			} else if (requestHeader.split("\n")[0].contains("PUT") && checkURL(file)) {

				System.out.println("Do not support PUT");

			} else {

				// Enter the error code
				// 404 page not found
				constructResponseHeader(404, sb, "");
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













	private File[] getFileList(String dirPath, String name) {
		
		File dir = new File(dirPath);   
		File[] fileList = dir.listFiles();

		if(dirPath.contains(name)){

		} else {

			for(File f : fileList){

				
	
			}

		}

		


		return fileList;
	}




































	


	// Build a Response Header
	private static void constructResponseHeader(int responseCode, StringBuilder sb, String fileType) {

		if (responseCode == 200) {

			sb.append("HTTP/1.1 200 OK\r\n");
			sb.append("Date:" + getTimeStamp() + "\r\n");
			sb.append("Server:localhost\r\n");
			sb.append("Content-Type: ");
			sb.append(fileType);
			sb.append("\r\n");
			sb.append("Connection: Closed\r\n\r\n");

		} else if (responseCode == 404) {

			sb.append("HTTP/1.1 404 Not Found\r\n");
			sb.append("Date:" + getTimeStamp() + "\r\n");
			sb.append("Server:localhost\r\n");
			sb.append("\r\n");

		} else if (responseCode == 403) {

			sb.append("HTTP/1.1 403 Forbidden response\r\n");
			sb.append("Date:" + getTimeStamp() + "\r\n");
			sb.append("Server:localhost\r\n");
			sb.append("\r\n");

		}

	}

	// Check if we enter restricted area
	private static Boolean restrictedDirectory(String string){

		if(string.toLowerCase().contains("restricted")){
			return false;
		} else {
			return true;
		}

	}


	// Build URL to obtain files in other directories
	private static String buildURL(String file){

		String [] urlParts = file.split("/");

		StringBuilder my_url = new StringBuilder();
		for(int i = 1; i < urlParts.length; i++){
			my_url.append(urlParts[i]);
			if(i != urlParts.length - 1) my_url.append("/");
		}

		return my_url.toString();

	}


	// Get the data of a file like html
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

			responseToClient += line; // last line

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


	// Check type of file
	private static String checkTypeOfFile(String string){

		if(string.toLowerCase().contains("html") || (string.toLowerCase().contains("htm"))){
			return "html";
		}
		else if ((string.toLowerCase().contains("png") || (string.toLowerCase().contains("jpg")) || (string.toLowerCase().contains("jpeg")))){
			return "image";
		} 
		else return "";

	}


	// Check the URL if it exist a file where it is pointing
	private static boolean checkURL(String file) {

		File myFile = new File(buildURL(file));

		if(DEBUGGING){
			System.out.println("----------FILE------------");
			System.out.println();
			System.out.println("name: " + file + " / exist: " + (myFile.exists() && !myFile.isDirectory()) + " / is root: " + myFile.isDirectory());
			System.out.println();
		}

		return myFile.exists() && !myFile.isDirectory();

	}


	// Return the extension of a file
	private static String getFileExtension(String file) {
        if(file.lastIndexOf(".") != -1 && file.lastIndexOf(".") != 0)
        return file.substring(file.lastIndexOf(".")+1);
        else return "";
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