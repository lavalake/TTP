package applications;
import java.io.IOException;
import java.net.SocketException;

import services.TTPServer;
import services.TTPServerEnd;



public class FTPServer {

	public static void main(String[] args) {

		System.out.println("FTP Server is listening on Port 2221");

		TTPServer ttp_server = new TTPServer(Integer.parseInt(args[0]),Integer.parseInt(args[1]));

		boolean listening = true;
		TTPServerEnd serverEnd;

		try {
			ttp_server.open(2221,10);

			while ( true) {
			    serverEnd = ttp_server.accept();
			    System.out.println("FTP server get a client connection");
				//byte[] request = serverEnd.receive();
				
					Thread serviceClient = new Thread(new ProxyFTPServer(serverEnd));
					serviceClient.start();
				
				System.out.println("FTP Server continues listening..");
			}
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
}
