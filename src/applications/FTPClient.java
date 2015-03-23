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

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		int dstPort = 2221;
		int srcPort = 2000;
		
		String srcAddr = "127.0.0.1";
		String dstAddr = "127.0.0.1";
		
		System.out.println("Enter file name");
		Scanner readfile = new Scanner(System.in);
		String fileName = readfile.nextLine();
		String path = System.getProperty("user.dir") + "/ClientFiles/";
		
		byte[] hashIndicator = "MD5-HASH".getBytes();
		byte[] startBytes = new byte[hashIndicator.length];
		byte[] md5hashRecd = new byte[16];

		TTPClientEnd client = new TTPClientEnd(Integer.parseInt(args[0]),Integer.parseInt(args[1]));
		
		try {
			client.open(srcAddr, dstAddr, (short)Integer.parseInt(args[2]), (short)dstPort, 10);
			System.out.println("\nEstablised connection to FTP Server at " + dstAddr + ":" + dstPort);
			System.out.println("get file "+fileName.getBytes());	
			client.send(fileName.getBytes());
			
			boolean listening = true;
			while (listening) {
				byte[] data = client.receive();
				System.out.println("FTP client get data");
				if (data!=null) {
					System.arraycopy(data, 0, startBytes, 0, hashIndicator.length);	
					
					if (Arrays.equals(startBytes, hashIndicator)) {
					    System.out.println("FTP client get md5 ");
						System.arraycopy(data, startBytes.length, md5hashRecd, 0, 16);
					} else {
						System.out.println("FTP Client received file!");

						MessageDigest md = MessageDigest.getInstance("MD5");
						byte[] md5HashComputed = md.digest(data);

						if (Arrays.equals(md5HashComputed,md5hashRecd)) {
							System.out.println("MD5 Hash verified!!");
							File f = new File(path + fileName);
							System.out.println("FTP client write to " + path + fileName);
							
							f.createNewFile();
							FileOutputStream fs = new FileOutputStream(f);
							BufferedOutputStream bs = new BufferedOutputStream(fs);
							bs.write(data);
							bs.close();
							bs = null;
							
						} else {
							System.out.println("Error in file received! MD5 digest does not match!");
							System.out.println("cal md5 " + md5HashComputed);
							for(int i =0; i<md5HashComputed.length; i++)
							    System.out.println(md5HashComputed[i] + " ");
							System.out.println("rec md5 " + md5hashRecd);
							for(int i =0; i<md5hashRecd.length; i++)
                                System.out.println(md5hashRecd[i] + " ");
						}
						client.close();
					}				
				}
			}			
		} catch (EOFException e) {
			System.out.println("EOF Exception!! Perhaps you are using the CMU network? Appears to work fine on all other networks we tested and on localhost! Test a text file instead e.g. Sample1.txt!");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}
}
