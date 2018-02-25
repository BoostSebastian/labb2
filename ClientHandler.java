import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.*;
import java.io.File;
import java.io.FilenameFilter;

public class ClientHandler implements Runnable {

	// Change for debugging console
	private static boolean DEBUGGING = true;
	private boolean test_500 = false;

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

		StringBuilder sb = new StringBuilder();															// stringbuilder to obtain page
		BufferedReader request = new BufferedReader(new InputStreamReader(client.getInputStream()));	// strem
		BufferedWriter response = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));	// strem

		try {

			String requestHeader = "";
			String temp = ".";

			// Read stream
			while (!temp.equals("")) {
				temp = request.readLine();
				System.out.println(temp);
				requestHeader += temp + "\n";
			}

			// Get the method from HTTP header
			String file = requestHeader.split("\n")[0].split(" ")[1];	// header split to obtain filepath

			if(DEBUGGING){
				System.out.println();
				System.out.println("------SPLITED FILES-------");
				System.out.println();
				System.out.println(Arrays.toString(requestHeader.split("\n")[0].split(" ")));					// print array
				System.out.println(Arrays.toString(requestHeader.split("\n")[0].split(" ")[1].split("/")));		// print array
				System.out.println();
			}

			// Testing a 500 error
			if(test_500){ throw new IOException(); }


			// Check for GET 
			if (requestHeader.split("\n")[0].contains("GET")) {

				String path = buildURL(file); 											// build url to obtain file
				String type = checkTypeOfFile(path); 									// check type of file
				String ext = getFileExtension(path);									// extension
				//boolean _root = new File(path).isDirectory();							// check if root directory

				
				if(restrictedDirectory(path) && !checkRoot(path)) {

					if(!path.toLowerCase().contains("google")){ 						// redirect

						if(checkURL(file)){ 											// if url exist or if we need to redirect

							//////////////////////
							//   200 RESPONSE  //
							////////////////////

							if (type == "html"){
	
								// Get the correct page
								constructResponseHeader(200, sb, "text/" + ext);		// build header
								response.write(sb.toString());							// send header
								response.write(getData(path));							// send data with html response
								sb.setLength(0);				 						// reset
								response.flush();										// flush
			
							} else if (type == "image") {
			
								File img_file = new File(path);							// file path to img
								FileInputStream fis = new FileInputStream(img_file);	// file strem
								byte[] data = new byte[(int) img_file.length()];		// file to byte
								fis.read(data);											// read the bytes
								fis.close();											// close stream
								
								// Header and data for media
								DataOutputStream binaryOut = new DataOutputStream(outputStream);
								binaryOut.writeBytes("HTTP/1.0 200 OK\r\n");
								binaryOut.writeBytes("Content-Type: image/" + ext + "\r\n");
								binaryOut.writeBytes("Content-Length: " + data.length);
								binaryOut.writeBytes("\r\n\r\n");
								binaryOut.write(data);
								binaryOut.close();
			
							} else {
								System.out.println("Do not support this format");
							}

							/////////////////////////////
							//     END 200 RESPONSE    //
							/////////////////////////////
	
						
	
						} else { 

							/////////////////////////
							//    404 NOT FOUND   //
							////////////////////////

							// Enter the error code
							// 404 page not found
							constructResponseHeader(404, sb, "");
							response.write(sb.toString());
							response.write(_404Temp());
							sb.setLength(0);
							response.flush();

							////////////////////////////
							//   END 404 NOT FOUND   //
							///////////////////////////

						}

					} else {

						/////////////////////////
						//    302 REDIRECT    //
						///////////////////////

						try {

							// 302 redirected
							constructResponseHeader(302, sb, "");
							response.write(sb.toString());
							sb.setLength(0);
							response.flush();
			
						} catch (Exception e) {
							e.printStackTrace();
						}

						/////////////////////////
						//  END 302 REDIRECT   //
						////////////////////////

					}

				} else {

					/////////////////////////
					//    403 FORBIDDEN    //
					////////////////////////

					// Enter the forbidden response 
					// 403 Forbidden
					constructResponseHeader(403, sb, "");
					response.write(sb.toString());
					sb.setLength(0);
					response.flush();

					////////////////////////////
					//   END 403 FORBIDDEN    //
					////////////////////////////

				}
			
			// Check for PUT
			} else if (requestHeader.split("\n")[0].contains("PUT") && checkURL(file)) {

				System.out.println("Do not support PUT");

			} else {

				// Enter the error code
				// 404 page not found
				constructResponseHeader(404, sb, "");
				response.write(sb.toString());
				response.write("<html lang=\"en\"><head><meta charset=\"utf-8\"><title>HTML-Page</title></head><body><p>html error from sebastian</p></body></html>");
				sb.setLength(0);
				response.flush();

			}

			// CLOSING
			request.close();
			response.close();
			client.close();
			return;

		} catch (Exception e) {

			// Enter the error code
			// 500 Internal Server Error
			constructResponseHeader(500, sb, "");
			response.write(sb.toString());
			response.write(_500Temp());
			sb.setLength(0);
			response.flush();

		}

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

			sb.append("HTTP/1.1 404 NOT FOUND\r\n");
			sb.append("Date:" + getTimeStamp() + "\r\n");
			sb.append("Server:localhost\r\n");
			sb.append("\r\n");

		} else if (responseCode == 403) {

			sb.append("HTTP/1.1 403 Forbidden \r\n");
			sb.append("Date:" + getTimeStamp() + "\r\n");
			sb.append("Server:localhost\r\n");
			sb.append("\r\n");

		} else if (responseCode == 302) {

			sb.append("HTTP/1.1 302 Redirected\r\n");
			sb.append("Location: https://www.google.se/ \r\n");
			sb.append("Date:" + getTimeStamp() + "\r\n");
			sb.append("Server:localhost\r\n");
			sb.append("\r\n");

		} else if (responseCode == 500) {

			sb.append("HTTP/1.1 500 Internal Server Error\r\n");
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

	// controlling 403 error with root
	private boolean checkRoot(String file){
        return file.toLowerCase().startsWith("root") || file.toLowerCase().startsWith(System.getProperty("user.dir").toLowerCase() + "/root");
    }

	// 400 Error
	private String _404Temp(){

		String html = ""
				+ "<html>"
				+ "<head>"
				+	"<meta charset=\"utf-8\">"
				+	"<meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge,chrome=1\">"
				+		"<title>404 Page no found</title>"
				+		"<meta name=\"viewport\" content=\"width=device-width\">"
				+ "</head>"
				+ "<body>"
				+	"<div class=\"error-page-wrap\">"
				+		"<article class=\"error-page gradient\">"
				+			"<hgroup>"
				+				"<h1>404</h1>"
				+				"<h2>Ops! Page Not Found</h2>"
				+			"</hgroup>"
				+		"</article>"
				+	"</div>"
				+ "</body>"
				+ "</html>"

				+ "<style>*,"
				+ "*:after,"
				+ " *:before {"
				+ "-webkit-box-sizing: border-box;"
				+ "-moz-box-sizing: border-box;"
				+ "-ms-box-sizing: border-box;"
				+ "box-sizing: border-box;"
				+ "}"
				+ "html {background: #ccc;font: bold 14px/20px \"Trajan Pro\", \"Times New Roman\", Times, serif;color: #430400;text-shadow: 0 1px 0 rgba(255, 255, 255, 0.15);}"
				+ ".error-page-wrap {width: 310px;height: 310px;margin: 155px auto;}"
				+ ".error-page-wrap:before {box-shadow: 0 0 200px 150px #fff;width: 310px;height: 310px;border-radius: 50%;position: relative;z-index: -1;content: \"\";display: block;}"
				+ ".error-page {width: 310px;height: 310px;border-radius: 50%;top: -310px;position: relative;text-align: center;background: #d36242;background: -moz-linear-gradient(top, #d36242 0%, darkred 100%);background: -webkit-gradient(linear,left top,left bottom,color-stop(0%, #d36242),color-stop(100%, darkred));background: -webkit-linear-gradient(top, #d36242 0%, darkred 100%);background: -o-linear-gradient(top, #d36242 0%, darkred 100%);background: -ms-linear-gradient(top, #d36242 0%, darkred 100%);background: linear-gradient(to bottom, #d36242 0%, darkred 100%);filter: progid:DXImageTransform.Microsoft.gradient(startColorstr=\"$firstColor\",endColorstr=\"$secondColor\",GradientType=0);}"
				+ ".error-page:before {width: 63px;height: 63px;border-radius: 50%;box-shadow: 3px 25px 0 5px #c95439;content: \"\";z-index: -1;display: block;position: relative;top: -19px;left: 44px;}"
				+ ".error-page:after {width: 310px;height: 17px;margin: 0 auto;top: 44px;content: \"\";z-index: -1;display: block;position: relative;background: -moz-radial-gradient(center,ellipse cover,rgba(0, 0, 0, 0.65) 0%,rgba(35, 26, 26, 0) 59%,rgba(60, 44, 44, 0) 100%);"
				+ "background: -webkit-gradient(radial,center center,0px,center center,100%,color-stop(0%, rgba(0, 0, 0, 0.65)),color-stop(59%, rgba(35, 26, 26, 0)),color-stop(100%, rgba(60, 44, 44, 0)));"
				+ "background: -webkit-radial-gradient(center,ellipse cover,rgba(0, 0, 0, 0.65) 0%,rgba(35, 26, 26, 0) 59%,rgba(60, 44, 44, 0) 100%);"
				+ "background: -o-radial-gradient(center,ellipse cover,rgba(0, 0, 0, 0.65) 0%,rgba(35, 26, 26, 0) 59%,rgba(60, 44, 44, 0) 100%);"
				+ "background: -ms-radial-gradient(center,ellipse cover,rgba(0, 0, 0, 0.65) 0%,rgba(35, 26, 26, 0) 59%,rgba(60, 44, 44, 0) 100%);"
				+ "background: radial-gradient(ellipse at center,rgba(0, 0, 0, 0.65) 0%,rgba(35, 26, 26, 0) 59%,rgba(60, 44, 44, 0) 100%);"
				+ "filter: progid:DXImageTransform.Microsoft.gradient(startColorstr=\"#a6000000\",endColorstr=\"#003c2c2c\",GradientType=1);}"
				+ ".error-page h1 {color: rgba(255, 255, 255, 0.94);font-size: 100px;margin: 65px auto 0 auto;text-shadow: 0px 0 7px rgba(0, 0, 0, 0.5);}"
				+ ".error-page h1:before {width: 260px;height: 1px;position: relative;margin: 0 auto;top: 70px;content: \"\";display: block;"
				+ "background: -moz-radial-gradient(center,ellipse cover,rgba(111, 25, 25, 0.65) 0%,rgba(75, 38, 38, 0) 70%,rgba(60, 44, 44, 0) 100%);"
				+ "background: -webkit-gradient(radial,center center,0px,center center,100%,color-stop(0%, rgba(111, 25, 25, 0.65)),color-stop(70%, rgba(75, 38, 38, 0)),color-stop(100%, rgba(60, 44, 44, 0)));background: -webkit-radial-gradient(center,ellipse cover,rgba(111, 25, 25, 0.65) 0%,rgba(75, 38, 38, 0) 70%,rgba(60, 44, 44, 0) 100%);"
				+ "background: -o-radial-gradient(center,ellipse cover,rgba(111, 25, 25, 0.65) 0%,rgba(75, 38, 38, 0) 70%,rgba(60, 44, 44, 0) 100%);"
				+ "background: -ms-radial-gradient(center,ellipse cover,rgba(111, 25, 25, 0.65) 0%,rgba(75, 38, 38, 0) 70%,rgba(60, 44, 44, 0) 100%);"
				+ "background: radial-gradient(ellipse at center,rgba(111, 25, 25, 0.65) 0%,rgba(75, 38, 38, 0) 70%,rgba(60, 44, 44, 0) 100%);"
				+ "filter: progid:DXImageTransform.Microsoft.gradient(startColorstr=\"#a66f1919\",endColorstr=\"#003c2c2c\",GradientType=1);}"
				+ ".error-page h1:after {width: 260px;height: 1px;content: \"\";display: block;opacity: 0.2;margin: 0 auto;top: 50px;position: relative;background: -moz-radial-gradient(center,ellipse cover,rgba(247, 173, 148, 0.65) 0%,rgba(255, 255, 255, 0.01) 99%,rgba(255, 255, 255, 0) 100%);"
				+ "background: -webkit-gradient(radial,center center,0px,center center,100%,color-stop(0%, rgba(247, 173, 148, 0.65)),color-stop(99%, rgba(255, 255, 255, 0.01)),color-stop(100%, rgba(255, 255, 255, 0)));"
				+ "background: -webkit-radial-gradient(center,ellipse cover,rgba(247, 173, 148, 0.65) 0%,rgba(255, 255, 255, 0.01) 99%,rgba(255, 255, 255, 0) 100%);"
				+ "background: -o-radial-gradient(center,ellipse cover,rgba(247, 173, 148, 0.65) 0%,rgba(255, 255, 255, 0.01) 99%,rgba(255, 255, 255, 0) 100%);"
				+ "background: -ms-radial-gradient(center,	ellipse cover,rgba(247, 173, 148, 0.65) 0%,rgba(255, 255, 255, 0.01) 99%,rgba(255, 255, 255, 0) 100%);"
				+ "background: radial-gradient(ellipse at center,rgba(247, 173, 148, 0.65) 0%,rgba(255, 255, 255, 0.01) 99%,rgba(255, 255, 255, 0) 100%);filter: progid:DXImageTransform.Microsoft.gradient(startColorstr=\"#a6f7ad94\",endColorstr=\"#00ffffff\",GradientType=1);}"
				+ ".error-page h2 {margin: 55px 0 30px 0;font-size: 17px;}"
				+ ".error-page h2:before {width: 130px;height: 1px;position: relative;margin: 0 auto;top: 31px;content: \"\";display: block;"
				+ "background: -moz-radial-gradient(center,ellipse cover,rgba(111, 25, 25, 0.65) 0%,rgba(75, 38, 38, 0) 70%,rgba(60, 44, 44, 0) 100%);"
				+ "background: -webkit-gradient(radial,center center,0px,center center,100%,color-stop(0%, rgba(111, 25, 25, 0.65)),color-stop(70%, rgba(75, 38, 38, 0)),color-stop(100%, rgba(60, 44, 44, 0)));"
				+ "background: -webkit-radial-gradient(center,ellipse cover,rgba(111, 25, 25, 0.65) 0%,rgba(75, 38, 38, 0) 70%,rgba(60, 44, 44, 0) 100%);background: -o-radial-gradient(center,ellipse cover,rgba(111, 25, 25, 0.65) 0%,rgba(75, 38, 38, 0) 70%,rgba(60, 44, 44, 0) 100%);"
				+ "background: -ms-radial-gradient(center,ellipse cover,rgba(111, 25, 25, 0.65) 0%,rgba(75, 38, 38, 0) 70%,rgba(60, 44, 44, 0) 100%);"
				+ "background: radial-gradient(ellipse at center,rgba(111, 25, 25, 0.65) 0%,rgba(75, 38, 38, 0) 70%,rgba(60, 44, 44, 0) 100%);"
				+ "filter: progid:DXImageTransform.Microsoft.gradient(startColorstr=\"#a66f1919\",endColorstr=\"#003c2c2c\",GradientType=1);}"
				+ ".error-page h2:after {width: 130px;height: 1px;content: \"\";display: block;opacity: 0.2;margin: 0 auto;top: 11px;position: relative;background: -moz-radial-gradient(center,ellipse cover,rgba(247, 173, 148, 0.65) 0%,rgba(255, 255, 255, 0.01) 99%,rgba(255, 255, 255, 0) 100%);"
				+ "background: -webkit-gradient(radial,center center,0px,center center,100%,color-stop(0%, rgba(247, 173, 148, 0.65)),color-stop(99%, rgba(255, 255, 255, 0.01)),color-stop(100%, rgba(255, 255, 255, 0)));"
				+ "background: -webkit-radial-gradient(center,ellipse cover,rgba(247, 173, 148, 0.65) 0%,rgba(255, 255, 255, 0.01) 99%,rgba(255, 255, 255, 0) 100%);background: -o-radial-gradient(center,ellipse cover,rgba(247, 173, 148, 0.65) 0%,rgba(255, 255, 255, 0.01) 99%,rgba(255, 255, 255, 0) 100%);"
				+ "background: -ms-radial-gradient(center,ellipse cover,rgba(247, 173, 148, 0.65) 0%,rgba(255, 255, 255, 0.01) 99%,rgba(255, 255, 255, 0) 100%);background: radial-gradient(ellipse at center,rgba(247, 173, 148, 0.65) 0%,rgba(255, 255, 255, 0.01) 99%,rgba(255, 255, 255, 0) 100%);"
				+ "filter: progid:DXImageTransform.Microsoft.gradient(startColorstr=\"#a6f7ad94\",endColorstr=\"#00ffffff\",GradientType=1);}"
				+ ".error-back {text-decoration: none;color: #430400;font-size: 15px;}"
				+ ".error-back:hover {color: #eb957d;text-shadow: 0 0 3px black;}</style>";


				return html;
		
	}

	// 500 Error
	private String _500Temp(){

		String html = ""
				+ "<html>"
				+ "<head>"
				+	"<meta charset=\"utf-8\">"
				+	"<meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge,chrome=1\">"
				+		"<title>404 Page no found</title>"
				+		"<meta name=\"viewport\" content=\"width=device-width\">"
				+ "</head>"
				+ "<body>"
				+	"<div class=\"error-page-wrap\">"
				+		"<article class=\"error-page gradient\">"
				+			"<hgroup>"
				+				"<h1>500</h1>"
				+				"<h2>Ops! Internal Server Error</h2>"
				+			"</hgroup>"
				+		"</article>"
				+	"</div>"
				+ "</body>"
				+ "</html>"

				+ "<style>*,"
				+ "*:after,"
				+ " *:before {"
				+ "-webkit-box-sizing: border-box;"
				+ "-moz-box-sizing: border-box;"
				+ "-ms-box-sizing: border-box;"
				+ "box-sizing: border-box;"
				+ "}"
				+ "html {background: #ccc;font: bold 14px/20px \"Trajan Pro\", \"Times New Roman\", Times, serif;color: #430400;text-shadow: 0 1px 0 rgba(255, 255, 255, 0.15);}"
				+ ".error-page-wrap {width: 310px;height: 310px;margin: 155px auto;}"
				+ ".error-page-wrap:before {box-shadow: 0 0 200px 150px #fff;width: 310px;height: 310px;border-radius: 50%;position: relative;z-index: -1;content: \"\";display: block;}"
				+ ".error-page {width: 310px;height: 310px;border-radius: 50%;top: -310px;position: relative;text-align: center;background: #d36242;background: -moz-linear-gradient(top, #d36242 0%, darkred 100%);background: -webkit-gradient(linear,left top,left bottom,color-stop(0%, #d36242),color-stop(100%, darkred));background: -webkit-linear-gradient(top, #d36242 0%, darkred 100%);background: -o-linear-gradient(top, #d36242 0%, darkred 100%);background: -ms-linear-gradient(top, #d36242 0%, darkred 100%);background: linear-gradient(to bottom, #d36242 0%, darkred 100%);filter: progid:DXImageTransform.Microsoft.gradient(startColorstr=\"$firstColor\",endColorstr=\"$secondColor\",GradientType=0);}"
				+ ".error-page:before {width: 63px;height: 63px;border-radius: 50%;box-shadow: 3px 25px 0 5px #c95439;content: \"\";z-index: -1;display: block;position: relative;top: -19px;left: 44px;}"
				+ ".error-page:after {width: 310px;height: 17px;margin: 0 auto;top: 44px;content: \"\";z-index: -1;display: block;position: relative;background: -moz-radial-gradient(center,ellipse cover,rgba(0, 0, 0, 0.65) 0%,rgba(35, 26, 26, 0) 59%,rgba(60, 44, 44, 0) 100%);"
				+ "background: -webkit-gradient(radial,center center,0px,center center,100%,color-stop(0%, rgba(0, 0, 0, 0.65)),color-stop(59%, rgba(35, 26, 26, 0)),color-stop(100%, rgba(60, 44, 44, 0)));"
				+ "background: -webkit-radial-gradient(center,ellipse cover,rgba(0, 0, 0, 0.65) 0%,rgba(35, 26, 26, 0) 59%,rgba(60, 44, 44, 0) 100%);"
				+ "background: -o-radial-gradient(center,ellipse cover,rgba(0, 0, 0, 0.65) 0%,rgba(35, 26, 26, 0) 59%,rgba(60, 44, 44, 0) 100%);"
				+ "background: -ms-radial-gradient(center,ellipse cover,rgba(0, 0, 0, 0.65) 0%,rgba(35, 26, 26, 0) 59%,rgba(60, 44, 44, 0) 100%);"
				+ "background: radial-gradient(ellipse at center,rgba(0, 0, 0, 0.65) 0%,rgba(35, 26, 26, 0) 59%,rgba(60, 44, 44, 0) 100%);"
				+ "filter: progid:DXImageTransform.Microsoft.gradient(startColorstr=\"#a6000000\",endColorstr=\"#003c2c2c\",GradientType=1);}"
				+ ".error-page h1 {color: rgba(255, 255, 255, 0.94);font-size: 100px;margin: 65px auto 0 auto;text-shadow: 0px 0 7px rgba(0, 0, 0, 0.5);}"
				+ ".error-page h1:before {width: 260px;height: 1px;position: relative;margin: 0 auto;top: 70px;content: \"\";display: block;"
				+ "background: -moz-radial-gradient(center,ellipse cover,rgba(111, 25, 25, 0.65) 0%,rgba(75, 38, 38, 0) 70%,rgba(60, 44, 44, 0) 100%);"
				+ "background: -webkit-gradient(radial,center center,0px,center center,100%,color-stop(0%, rgba(111, 25, 25, 0.65)),color-stop(70%, rgba(75, 38, 38, 0)),color-stop(100%, rgba(60, 44, 44, 0)));background: -webkit-radial-gradient(center,ellipse cover,rgba(111, 25, 25, 0.65) 0%,rgba(75, 38, 38, 0) 70%,rgba(60, 44, 44, 0) 100%);"
				+ "background: -o-radial-gradient(center,ellipse cover,rgba(111, 25, 25, 0.65) 0%,rgba(75, 38, 38, 0) 70%,rgba(60, 44, 44, 0) 100%);"
				+ "background: -ms-radial-gradient(center,ellipse cover,rgba(111, 25, 25, 0.65) 0%,rgba(75, 38, 38, 0) 70%,rgba(60, 44, 44, 0) 100%);"
				+ "background: radial-gradient(ellipse at center,rgba(111, 25, 25, 0.65) 0%,rgba(75, 38, 38, 0) 70%,rgba(60, 44, 44, 0) 100%);"
				+ "filter: progid:DXImageTransform.Microsoft.gradient(startColorstr=\"#a66f1919\",endColorstr=\"#003c2c2c\",GradientType=1);}"
				+ ".error-page h1:after {width: 260px;height: 1px;content: \"\";display: block;opacity: 0.2;margin: 0 auto;top: 50px;position: relative;background: -moz-radial-gradient(center,ellipse cover,rgba(247, 173, 148, 0.65) 0%,rgba(255, 255, 255, 0.01) 99%,rgba(255, 255, 255, 0) 100%);"
				+ "background: -webkit-gradient(radial,center center,0px,center center,100%,color-stop(0%, rgba(247, 173, 148, 0.65)),color-stop(99%, rgba(255, 255, 255, 0.01)),color-stop(100%, rgba(255, 255, 255, 0)));"
				+ "background: -webkit-radial-gradient(center,ellipse cover,rgba(247, 173, 148, 0.65) 0%,rgba(255, 255, 255, 0.01) 99%,rgba(255, 255, 255, 0) 100%);"
				+ "background: -o-radial-gradient(center,ellipse cover,rgba(247, 173, 148, 0.65) 0%,rgba(255, 255, 255, 0.01) 99%,rgba(255, 255, 255, 0) 100%);"
				+ "background: -ms-radial-gradient(center,	ellipse cover,rgba(247, 173, 148, 0.65) 0%,rgba(255, 255, 255, 0.01) 99%,rgba(255, 255, 255, 0) 100%);"
				+ "background: radial-gradient(ellipse at center,rgba(247, 173, 148, 0.65) 0%,rgba(255, 255, 255, 0.01) 99%,rgba(255, 255, 255, 0) 100%);filter: progid:DXImageTransform.Microsoft.gradient(startColorstr=\"#a6f7ad94\",endColorstr=\"#00ffffff\",GradientType=1);}"
				+ ".error-page h2 {margin: 55px 0 30px 0;font-size: 17px;}"
				+ ".error-page h2:before {width: 130px;height: 1px;position: relative;margin: 0 auto;top: 31px;content: \"\";display: block;"
				+ "background: -moz-radial-gradient(center,ellipse cover,rgba(111, 25, 25, 0.65) 0%,rgba(75, 38, 38, 0) 70%,rgba(60, 44, 44, 0) 100%);"
				+ "background: -webkit-gradient(radial,center center,0px,center center,100%,color-stop(0%, rgba(111, 25, 25, 0.65)),color-stop(70%, rgba(75, 38, 38, 0)),color-stop(100%, rgba(60, 44, 44, 0)));"
				+ "background: -webkit-radial-gradient(center,ellipse cover,rgba(111, 25, 25, 0.65) 0%,rgba(75, 38, 38, 0) 70%,rgba(60, 44, 44, 0) 100%);background: -o-radial-gradient(center,ellipse cover,rgba(111, 25, 25, 0.65) 0%,rgba(75, 38, 38, 0) 70%,rgba(60, 44, 44, 0) 100%);"
				+ "background: -ms-radial-gradient(center,ellipse cover,rgba(111, 25, 25, 0.65) 0%,rgba(75, 38, 38, 0) 70%,rgba(60, 44, 44, 0) 100%);"
				+ "background: radial-gradient(ellipse at center,rgba(111, 25, 25, 0.65) 0%,rgba(75, 38, 38, 0) 70%,rgba(60, 44, 44, 0) 100%);"
				+ "filter: progid:DXImageTransform.Microsoft.gradient(startColorstr=\"#a66f1919\",endColorstr=\"#003c2c2c\",GradientType=1);}"
				+ ".error-page h2:after {width: 130px;height: 1px;content: \"\";display: block;opacity: 0.2;margin: 0 auto;top: 11px;position: relative;background: -moz-radial-gradient(center,ellipse cover,rgba(247, 173, 148, 0.65) 0%,rgba(255, 255, 255, 0.01) 99%,rgba(255, 255, 255, 0) 100%);"
				+ "background: -webkit-gradient(radial,center center,0px,center center,100%,color-stop(0%, rgba(247, 173, 148, 0.65)),color-stop(99%, rgba(255, 255, 255, 0.01)),color-stop(100%, rgba(255, 255, 255, 0)));"
				+ "background: -webkit-radial-gradient(center,ellipse cover,rgba(247, 173, 148, 0.65) 0%,rgba(255, 255, 255, 0.01) 99%,rgba(255, 255, 255, 0) 100%);background: -o-radial-gradient(center,ellipse cover,rgba(247, 173, 148, 0.65) 0%,rgba(255, 255, 255, 0.01) 99%,rgba(255, 255, 255, 0) 100%);"
				+ "background: -ms-radial-gradient(center,ellipse cover,rgba(247, 173, 148, 0.65) 0%,rgba(255, 255, 255, 0.01) 99%,rgba(255, 255, 255, 0) 100%);background: radial-gradient(ellipse at center,rgba(247, 173, 148, 0.65) 0%,rgba(255, 255, 255, 0.01) 99%,rgba(255, 255, 255, 0) 100%);"
				+ "filter: progid:DXImageTransform.Microsoft.gradient(startColorstr=\"#a6f7ad94\",endColorstr=\"#00ffffff\",GradientType=1);}"
				+ ".error-back {text-decoration: none;color: #430400;font-size: 15px;}"
				+ ".error-back:hover {color: #eb957d;text-shadow: 0 0 3px black;}</style>";


				return html;
		
	}

}