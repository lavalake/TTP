package applications;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import services.TTPServer;
import services.TTPServerEnd;

public class ProxyFTPServer implements Runnable {
	private TTPServerEnd ttp;
	private byte[] data;

	public ProxyFTPServer(TTPServerEnd ttp) {
		super();
		this.ttp = ttp;
		
	}

	@Override
	public void run() {
		System.out.println("Proxy FTP Server servicing FTP Client...");
		try {
		    try {
                data = ttp.receive();
            } catch (ClassNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
			byte[] temp = new byte[data.length];
			System.arraycopy(data, 0, temp, 0, data.length);
			
			String fileName = new String(temp,"US-ASCII");

			System.out.println("File Requested:" + fileName);

			byte[] clientInfo = new byte[6];
			//System.arraycopy(data, 0, clientInfo, 0, 6);

			File file = new File(fileName.toString());
			FileInputStream fs = new FileInputStream(file);
			byte[] fileData = new byte[(int)file.length()];
			fs.read(fileData, 0, (int)file.length());
			fs.close();
			
			byte[] totalData = new byte[(int)file.length() ];
			//System.arraycopy(clientInfo, 0, totalData, 0, 6);
			System.arraycopy(fileData, 0, totalData, 0, (int)file.length());
			
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] md5Hash = md.digest(fileData);
			
			byte[] hashIndicator = "MD5-HASH".getBytes();
			byte[] hashMsg = new byte[md5Hash.length + hashIndicator.length];
			//System.arraycopy(clientInfo, 0, hashMsg, 0, 6);
			System.arraycopy(hashIndicator, 0, hashMsg, 0, hashIndicator.length);
			System.arraycopy(md5Hash, 0, hashMsg, hashIndicator.length, md5Hash.length);
			
			ttp.send(hashMsg);
			//Thread.sleep(1000);
			ttp.send(totalData);
			try {
                ttp.close();
            } catch (ClassNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
			
			System.out.println("FTP Server has sent file to TTP to send to FTP client!");
		} catch (FileNotFoundException e) {
			System.out.println("The file specified does not exist!");
		}
		catch (IOException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} 
	}

}
