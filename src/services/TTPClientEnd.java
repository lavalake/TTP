package services;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.swing.Timer;

import datatypes.Datagram;

public class TTPClientEnd extends TTPConnection {

    public TTPClientEnd(int N, int time) {
        super(N,time);
        this.N = N;
        this.retrnsNum = 0;
        clock = new Timer(this.retrsTime,handShakeListener);
        clock.setInitialDelay(this.retrsTime);

    }

    public void changeClockListener(){
        
        clock.removeActionListener(handShakeListener);
        clock.addActionListener(dataListener);
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
        datagram.setData(fillHeader(TTPConnection.SYN));
        //datagram.setChecksum((short) -1);
        
        datagram.setChecksum(calcChecksum((byte[])datagram.getData()));
        this.ds.sendDatagram(datagram);
        System.out.println("data: "+ Arrays.toString(((byte[])datagram.getData())) + "chsum " + datagram.getChecksum());
        System.out.println("SYN sent to " + datagram.getDstaddr() + ":" + datagram.getDstport() + " with ISN " + nextSeq);

        base = nextSeq;
        clock.start();
        
        nextSeq++;
        while(true){
            Datagram request = ds.receiveDatagram(); 
            byte[] data = (byte[]) request.getData();
            if (data[8] == (byte)6) {  
                
                ackn = byteArrayToInt(new byte[]{ data[0], data[1], data[2], data[3]});
                expectedSeq =  ackn + 1;
                base = byteArrayToInt(new byte[]{ data[4], data[5], data[6], data[7]}) + 1;
                System.out.println("Received SYNACK with seq no:" + ackn + " and Acknowledgement No " + (base-1));
                clock.stop();
                SYNAcknowledgement();
                changeClockListener();
                return;
            }
        }
    }

    /**
     * Called by the application layer protocol to close the connection. 
     * Send FIN to server and waiting for the FINACK
     * 
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void close() throws IOException, ClassNotFoundException {
        datagram.setData(fillHeader(FIN));
        datagram.setSize((short)9);
        //datagram.setChecksum((short)-1);
        datagram.setChecksum(calcChecksum((byte[])datagram.getData()));

        ds.sendDatagram(datagram);
        System.out.println("FIN sent! Seq No:" + nextSeq);
        unackedPackets.clear();

        unackedPackets.put(nextSeq, new Datagram(datagram.getSrcaddr(), datagram.getDstaddr(), datagram.getSrcport(), datagram.getDstport(), datagram.getSize(), datagram.getChecksum(), datagram.getData()));
        

        //if(base == nextSeq){
            //System.out.println("start fin timer");
            clock.restart();
        //}
        nextSeq++;
        //receive the FINACK
        while(true){
            byte[] data = receiveData();
            if(data[8]== (byte)3) {
                ackn = byteArrayToInt(new byte[]{ data[0], data[1], data[2], data[3]});
                expectedSeq =  ackn + 1;
                base = byteArrayToInt(new byte[]{ data[4], data[5], data[6], data[7]}) + 1;
                System.out.println("Received FINACK with seq no:" + ackn + "stop fin timer");
                clock.stop();
                
                finackAcknowledgement();
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
    
    public void changeCloseListener(){
        clock.stop();
        clock.removeActionListener(dataListener);
        clock.addActionListener(closeClient);
    }
    
    


}
