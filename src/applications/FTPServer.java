package applications;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import services.TTPServer;
import services.TTPServerEnd;


public class FTPServer implements Runnable{
    private TTPServerEnd ttp;
    private byte[] data;

    public FTPServer(TTPServerEnd ttp) {
        super();
        this.ttp = ttp;
    }

    public void run() {
        System.out.println("FTP Server starts to service client");
        try {

            data = ttp.receive();
//            byte[] filename = new byte[data.length];
//            System.arraycopy(data, 0, filename, 0, data.length);

            String fileName = new String(data, "US-ASCII");

            System.out.println("Requested filename:" + fileName);

            File file = new File(fileName);
            FileInputStream fs = new FileInputStream(file);
            byte[] fileData = new byte[(int)file.length()];
            fs.read(fileData, 0, (int)file.length());
            fs.close();

            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] md5Hash = md.digest(fileData);

            //send md5 hash
            ttp.send(md5Hash);
            //send the file data
            ttp.send(fileData);
            ttp.close();
            System.out.println("Sent file to TTP");

        } catch (FileNotFoundException e) {
            System.out.println("The file does not exist");
        }
        catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
	public static void main(String[] args) {
		
		TTPServer ttp_server = new TTPServer(Integer.parseInt(args[0]), Integer.parseInt(args[1]));

		boolean listening = true;
		TTPServerEnd serverEnd;

        System.out.println("Enter the port number");
//        int port = Integer.parseInt(args[2]);
        int port = 2221;
		System.out.println("FTP Server starts to listen (Port " + port + ")");
		
		try {
			ttp_server.open(port, 10);

			while (listening) {
			    serverEnd = ttp_server.accept();
			    System.out.println("FTP server finds a client connection");
				
				Thread serviceClient = new Thread(new FTPServer(serverEnd));
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

