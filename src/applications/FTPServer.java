package applications;
import java.io.IOException;
import java.net.SocketException;

import services.TTPServer;
import services.TTPServerEnd;


public class FTPServer{

	public static void main(String[] args) {
		
		TTPServer ttp_server = new TTPServer(Integer.parseInt(args[0]), Integer.parseInt(args[1]));

		boolean listening = true;
		TTPServerEnd serverEnd;

        //System.out.println("Enter the port number");
//        int port = Integer.parseInt(args[2]);
        int port = 1222;
		System.out.println("FTP Server starts to listen (Port " + port + ")");
		
		try {
			ttp_server.open(port, 10);

			while (listening) {
			    serverEnd = ttp_server.accept();
			    System.out.println("FTP server finds a client connection");
				
				Thread serviceClient = new Thread(new ServiceThread(serverEnd));
				serviceClient.start();
				
				System.out.println("FTP Server continues to listen and wait for client connection");
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

