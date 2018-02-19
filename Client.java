/*
  TCPEchoClient.java
  A simple echo client with no error handling
*/

import java.util.concurrent.TimeUnit;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class TCPEchoClient {

    public static final int BUFSIZE= 1024;
	public static final int MYPORT= 0;
	
	// my variables
	public static int SIMULATIONTIME = 1000;
	public static String MSG = "An Echo Message!";

    public static void main(String[] args) throws IOException {

		System.out.println("\nHello, I'm awake and redy to send some packages!\n");

		

		/*

			args[0] = IP
			args[1] = Port
			args[2] = Buffersize
			arga[3] = Messages transfer rate

		*/
		
		// Error handeling
		if ( args.length == 4 && validateIP(args[0]) && validatePort(args[1]) ) {

			Socket socket = new Socket(args[0], 4950);

			// some varibles name
			String ip = args[0];
			String port = args[1];
			byte[] buffSize = new byte[Integer.parseInt(args[2])];
			int mtr = Integer.parseInt(args[3]);


			/*
			Scanner keyboard = new Scanner(System.in);
			System.out.println("\nEnter a text to send via TCP: ");
			MSG = keyboard.nextLine();

			
			System.out.println("\nDo you want to change simultation time? (Y/n): ");
			String answer = keyboard.nextLine();

			if(answer == "Y" || answer ==  "y"){
				System.out.println("\nEnter simultation time in milliseconds: ");
				SIMULATIONTIME = keyboard.nextInt();
			}

			keyboard.close();
			*/
			
			// Streams
			DataOutputStream out = new DataOutputStream(socket.getOutputStream());
			InputStream in = socket.getInputStream();
			
			out.writeBytes(MSG + "\n");
		
			String msg = "";
			int bytes;
			while((bytes = in.read(buffSize)) != -1) {
				msg += new String(buffSize, "UTF-8").substring(0, bytes);
				System.out.println(msg);	
			}
			
			System.out.println();
			System.out.println("Server response: " + msg);
			System.out.println();
			System.out.println("Thank you for sending!");
			System.out.println();

			// Close socket
			socket.close();

		
		} else {

			if(!validateIP(args[0])){
				System.err.printf("invalid ip");
				System.exit(1);
			}
			if(!validatePort(args[1])){
				System.err.printf("invalid port");
				System.exit(1);
			} else {
				System.err.printf("usage: %s%s%s%s Ip Port Buffersize Packages/sec\n", args[0], args[1], args[2], args[3]);
		    	System.exit(1);
			}
			
		}

	}// end main

	// Validate by checking ip's length after seperating by dots,
	// then, check if each ip part is between 0 and 255.
	public static boolean validateIP(String ip){
		String [] token = ip.split("\\.");
		if(token.length != 4){
			return false;
		}
		for(String str : token){
			int i = Integer.parseInt(str);
			if(((i < 0) || (i > 255))){
				return false;
			}
		}
		return true;
	}
	// Validating ports
	public static boolean validatePort(String port){
		if(Integer.parseInt(port) < 1024 || Integer.parseInt(port) > 65535){
			return false;
		}
		return true;
	}
	
}



