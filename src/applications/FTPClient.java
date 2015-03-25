package applications;

import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Scanner;

import services.*;

public class FTPClient {

	public static void main(String[] args) {
	    
	    if(args.length != 3){
	        System.out.println("worng input arguments, usage: java applications.FTPClient <N> <time> <port>");
	        return;
	    }

        TTPClientEnd client = new TTPClientEnd(Integer.parseInt(args[0]), Integer.parseInt(args[1]));


        String srcAddr = "127.0.0.1";
        String dstAddr = "127.0.0.1";
        int dstPort = 1222;
        int srcPort = Integer.parseInt(args[2]);
        File dirFile = new File("recdFiles");
        if(!dirFile.exists()){
            if(!dirFile.mkdir()){
                System.out.println("create dir failure");
            }
        }
		System.out.println("Enter the request file name");
		Scanner requestFile = new Scanner(System.in);
		String fileName = requestFile.nextLine();
		String path = System.getProperty("user.dir") + "/recdFiles/";
		
		try {
			client.open(srcAddr, dstAddr, (short)srcPort, (short)dstPort, 10);
			System.out.println("Connect to FTP Server (" + dstAddr + ":" + dstPort + ")");

            client.send(fileName.getBytes());
            System.out.println("Send request to get "+ Arrays.toString(fileName.getBytes()));

            //receive first message (MD5 or error)
            byte[] msgReceived = client.receive();

            //receive file-not-existed error message
            if (Arrays.equals(msgReceived, "FileNotExisted".getBytes())){
                System.out.println("Requested File not existed");
            }
            //receive md5 hash code
            else if (msgReceived != null){
                System.out.println("!!!!!!!!!!!!!!!!!!!Received md5 hash!!!!!!!!!!!!!!!!");

                //receive file date
                byte[] data = client.receive();
                if (data != null){
                    System.out.println("!!!!!!!!!!!!!!!!!Received file and start to check md5!!!!!!!!!!!!!!!");

                    MessageDigest complete = MessageDigest.getInstance("MD5");
                    byte[] md5Check = complete.digest(data);

                    //check and verify md5
                    if (Arrays.equals(md5Check, msgReceived)){
                        System.out.println("!!!!!!!!!!!!md5 checked and validated");
                        File newFile = new File(path + fileName);
                        FileOutputStream fs = new FileOutputStream(newFile);
                        BufferedOutputStream bs = new BufferedOutputStream(fs);
                        bs.write(data);
                        bs.close();
                        System.out.println("File received");
                    }
                    else {
                        //md5 does not match
                        System.out.println("md5 does not match");
                        System.out.println("cal md5 " + Arrays.toString(md5Check));
                        for (byte aMd5Check : md5Check) System.out.println(aMd5Check + " ");
                        System.out.println("rec md5 " + Arrays.toString(msgReceived));
                        for (byte aMsgReceived : msgReceived) System.out.println(aMsgReceived + " ");
                    }
                }
            }

            System.out.println("Client closes");
            client.close();

		} catch (EOFException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}
}
