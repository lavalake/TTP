/*
 *  A Stub that provides datagram send and receive functionality
 *  
 *  Feel free to modify this file to simulate network errors such as packet
 *  drops, duplication, corruption etc. But for grading purposes we will
 *  replace this file with out own version. So DO NOT make any changes to the
 *  function prototypes
 */
package services;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import datatypes.Datagram;

public class DatagramService {

	private int port;
	private int verbose;
	private DatagramSocket socket;
    private ArrayList<DatagramPacket> bufferPacket;
    private int num = 5;

	public DatagramService(int port, int verbose) throws SocketException {
		super();
		this.port = port;
		this.verbose = verbose;

		socket = new DatagramSocket(port);
	}

	public void sendDatagram(Datagram datagram) throws IOException {
        num++;

		ByteArrayOutputStream bStream = new ByteArrayOutputStream(1500);
		ObjectOutputStream oStream = new ObjectOutputStream(bStream);

        //Testing for packet corruption
        if (num % 11 == 0) {
            byte[] temp = (byte[])datagram.getData();
            temp[temp.length - 1] = (byte)(temp[temp.length -1]^1);
            datagram.setData(temp);
            System.out.println("Testing for packet corruption");
        }

        oStream.writeObject(datagram);
		oStream.flush();

        //Creating random number for testing different situations
        Random numGenerator = new Random();
        System.out.println("Count number " + num);

		// Create Datagram Packet
		byte[] data = bStream.toByteArray();
		InetAddress IPAddress = InetAddress.getByName(datagram.getDstaddr());

		DatagramPacket packet = new DatagramPacket(data, data.length,
				IPAddress, datagram.getDstport());

        // Testing for delay packet
//        if (num % 11 == 0){
//            int randomNum = numGenerator.nextInt(10);
//            delayPacket(packet, randomNum * 1000 + 3000);
//            System.out.println("Testing for delay packet, delaying" + randomNum + "seconds");
//        }
		// Testing for duplicate packet
//        if (num % 13 == 0) {
//            int randomNum = numGenerator.nextInt(10);
//            duplicatePacket(packet, randomNum);
//            System.out.println("Testing for duplicate packet, duplicating" + randomNum + "times");
//        }

        //Testing for packet drop
        if (num % 7 == 0)
            System.out.println("Testing for packet drop");
        else {
            System.out.println("Send Packet");
            socket.send(packet);
        }
//        if (num % 7 == 0) {
//            //Store packet to arraylist to do out-of-order deliveries testing
//            if (bufferPacket == null)
//                bufferPacket = new ArrayList<DatagramPacket>();
//            bufferPacket.add(packet);
//            System.out.println("Add to packet buffer");
//        }
//        else {
//            if (bufferPacket != null){
//                System.out.println("Testing for packet out-of-order deliveries");
//                bufferPacket.add(packet);
//                Collections.shuffle(bufferPacket);
//                for (DatagramPacket p : bufferPacket)
//                    socket.send(p);
//
//                bufferPacket = null;
//            } else {
//
//            }
//        }


	}

	public Datagram receiveDatagram() throws IOException,
			ClassNotFoundException {

		byte[] buf = new byte[1500];
		DatagramPacket packet = new DatagramPacket(buf, buf.length);

		socket.receive(packet);

		ByteArrayInputStream bStream = new ByteArrayInputStream(
				packet.getData());
		ObjectInputStream oStream = new ObjectInputStream(bStream);
		Datagram datagram = (Datagram) oStream.readObject();

		return datagram;
	}

    protected void delayPacket(DatagramPacket packet, int delay){
        try {
            Thread.sleep(delay);
            socket.send(packet);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void duplicatePacket(DatagramPacket packet, int duplicate){
        for (int i = 0; i < duplicate; i++)
            try {
                socket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
    }
}
