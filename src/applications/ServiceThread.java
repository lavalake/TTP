package applications;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import services.TTPServerEnd;

public class ServiceThread implements Runnable {
	private TTPServerEnd ttp;
	private byte[] data;

	public ServiceThread(TTPServerEnd ttp) {
		super();
		this.ttp = ttp;
		
	}

	@Override
    public void run() {
        System.out.println("FTP Server starts to service client");
        try {

            data = ttp.receive();
            String fileName = new String(data, "US-ASCII");

            System.out.println("Requested filename:" + fileName);

            //read file on the server to the data stream
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
            String error = "FileNotExisted";
            byte[] errorMsg = error.getBytes();
            try {
                ttp.send(errorMsg);
                ttp.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            } catch (ClassNotFoundException e1) {
                e1.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

}
