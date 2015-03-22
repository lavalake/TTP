package services;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.swing.Timer;

import datatypes.Datagram;

public class TTPClientEnd extends TTPConnection {
    
    private int N;
    private int acknNum;
    private int expectedSeqNum;
    private int time;
    private Timer clock;
    


    public TTPClientEnd(int N, int time) {
        super(N,time);
        this.N = N;

        this.time = time;

        clock = new Timer(this.time,handShakeListener);
        clock.setInitialDelay(this.time);

    }

    public void changeClockListener(){
        clock.removeActionListener(handShakeListener);
        clock.addActionListener(listener);
    }
    /**
     * Sends a SYN packet and opens the Connection End Point for receiving data at the client side
     * 
     * @param src
     * @param dest
     * @param srcPort
     * @param destPort
     * @param verbose
     * 
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void open(String src, String dest, short srcPort, short destPort, int verbose) throws IOException, ClassNotFoundException {

        this.ds = new DatagramService(srcPort, verbose);

        datagram.setSrcaddr(src);
        datagram.setDstaddr(dest);
        datagram.setSrcport((short) srcPort);
        datagram.setDstport((short) destPort);
        datagram.setSize((short) 9);
        datagram.setData(createPayloadHeader(TTPConnection.SYN));
        datagram.setChecksum((short) -1);
        this.ds.sendDatagram(datagram);
        System.out.println("SYN sent to " + datagram.getDstaddr() + ":" + datagram.getDstport() + " with ISN " + nextSeqNum);

        base = nextSeqNum;
        
        
        nextSeqNum++;
        while(true){
            Datagram request = ds.receiveDatagram(); 
            byte[] data = (byte[]) request.getData();
            if (data[8] == (byte)6) {  
                
                acknNum = byteArrayToInt(new byte[]{ data[0], data[1], data[2], data[3]});
                expectedSeqNum =  acknNum + 1;
                base = byteArrayToInt(new byte[]{ data[4], data[5], data[6], data[7]}) + 1;
                System.out.println("Received SYNACK with seq no:" + acknNum + " and Acknowledgement No " + (base-1));
                
                sendSYNAcknowledgement();
                changeClockListener();
                return;
            }
        }
    }

    public void close() throws IOException, ClassNotFoundException {
        datagram.setData(createPayloadHeader(FIN));
        datagram.setSize((short)9);
        datagram.setChecksum((short)-1);

        ds.sendDatagram(datagram);
        System.out.println("FIN sent! Seq No:" + nextSeqNum);

        unacknowledgedPackets.put(nextSeqNum, new Datagram(datagram.getSrcaddr(), datagram.getDstaddr(), datagram.getSrcport(), datagram.getDstport(), datagram.getSize(), datagram.getChecksum(), datagram.getData()));
        nextSeqNum++;

        if(base == nextSeqNum)
            clock.restart();
        //receive the FINACK
        while(true){
            byte[] data = receiveData();
            if(data[8]== (byte)3) {
                acknNum = byteArrayToInt(new byte[]{ data[0], data[1], data[2], data[3]});
                expectedSeqNum =  acknNum + 1;
                base = byteArrayToInt(new byte[]{ data[4], data[5], data[6], data[7]}) + 1;
                System.out.println("Received FINACK with seq no:" + acknNum );

                
                sendFinackAcknowledgement();
                return;
                
            }
        }

    }
    /**
     *  Action listener for when the packet times out
     */
    
    ActionListener handShakeListener = new ActionListener(){
        public void actionPerformed(ActionEvent event){
            System.out.println("Timeout for SYN");
            try {
                ds.sendDatagram(datagram);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            clock.restart();
            
        }
    };


}
