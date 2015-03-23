package services;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.swing.Timer;

import datatypes.Datagram;

public class TTPServerEnd extends TTPConnection {
    String sourceKey;
    



    BlockingQueue<Datagram> dgQ;
    

    public TTPServerEnd(int N, int time) {
        super(N, time);
        clock = new Timer(this.retrsTime,handShakeListener);
        clock.setInitialDelay(this.retrsTime);
    }

    public TTPServerEnd(int N, int time, DatagramService ds) {
        this(N, time);
        this.ds = ds;
    }

    public String getSourceKey() {
        return sourceKey;
    }

    public void setSourceKey(String sourceKey) {
        this.sourceKey = sourceKey;
    }
    
    public BlockingQueue<Datagram> getDgQ() {
        return dgQ;
    }

    public void setDgQ(BlockingQueue<Datagram> dgQ) {
        this.dgQ = dgQ;
    }
    /**
     * The receive data function . It reads the incoming packets
     * 
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public byte[] receiveData() throws IOException, ClassNotFoundException {
        
        byte[] data;
        try {
            System.out.println("TTPServerEnd waiting fro data");
            recvdDg = dgQ.take();
            data = (byte[]) recvdDg.getData();
            return data;
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
        
    }
    
    //server side close, wait for FIN from client
    public void close() throws IOException, ClassNotFoundException {
        System.out.println("server close the connection");
        //receive the FIN
        while(true){
            byte[] data = receiveData();
            if(data[8] == (byte)1){
                unackedPackets.clear();
                ackn = byteArrayToInt(new byte[]{ data[0], data[1], data[2], data[3]});
                expectedSeq =  ackn + 1;
                datagram.setSize((short) 9);
                datagram.setData(fillHeader(FINACK));
                datagram.setChecksum((short)0);
                ds.sendDatagram(datagram);

                System.out.println("FINACK sent to " + datagram.getDstaddr() + ":" + datagram.getDstport());

                clock.restart();
                return;
            }
        }

    }
    public void changeClockListener(){
        
        clock.stop();
        clock.removeActionListener(handShakeListener);
        clock.addActionListener(dataListener);
    }
    
    /**
     *  Action listener for when the packet times out
     */
    
    ActionListener handShakeListener = new ActionListener(){
        public void actionPerformed(ActionEvent event){
            System.out.println("Timeout for SYNACK to :" + sourceKey);
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
