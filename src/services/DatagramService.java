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
import java.util.Random;

import datatypes.Datagram;

public class DatagramService {

	private int port;
	private int verbose;
	private DatagramSocket socket;

	public DatagramService(int port, int verbose) throws SocketException {
		super();
		this.port = port;
		this.verbose = verbose;

		socket = new DatagramSocket(port);
	}

	public void sendDatagram(Datagram datagram) throws IOException {

		ByteArrayOutputStream bStream = new ByteArrayOutputStream(1500);
		ObjectOutputStream oStream = new ObjectOutputStream(bStream);
		oStream.writeObject(datagram);
		oStream.flush();

		// Create Datagram Packet
		byte[] data = bStream.toByteArray();
		InetAddress IPAddress = InetAddress.getByName(datagram.getDstaddr());
		DatagramPacket packet = new DatagramPacket(data, data.length,
				IPAddress, datagram.getDstport());

        Random numGenerator = new Random();
        int num = numGenerator.nextInt(10);

        // Testing for delay packet
        if (num == 3){
            int randomNum = numGenerator.nextInt(10);
            delayPacket(packet, randomNum * 1000);
            System.out.println("Testing for delay packet, delaying" + randomNum + "seconds");
        }
		// Testing for duplicate packet
        if (num == 7) {
            int randomNum = numGenerator.nextInt(10);
            duplicatePacket(packet, randomNum);
            System.out.println("Testing for duplicate packet, duplicating" + randomNum + "times");
        }
		socket.send(packet);
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
