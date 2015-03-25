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
    
    TTPServer server;
    boolean connected;



    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    BlockingQueue<Datagram> dgQ;
    

    public TTPServerEnd(int N, int time) {
        super(N, time);
        clock = new Timer(this.retrsTime,handShakeListener);
        clock.setInitialDelay(this.retrsTime);
        retrnsNum = 0;
        connected = false;
    }

    public TTPServerEnd(int N, int time, DatagramService ds, TTPServer server) {
        this(N, time);
        this.ds = ds;
        this.server = server;
        
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
     * The receive data function . It reads the incoming packets from the blockingQueue
     * 
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public byte[] receiveData() throws IOException, ClassNotFoundException {
        
        byte[] data;
        try {
            //System.out.println("TTPServerEnd waiting fro data");
            recvdDg = dgQ.take();
            data = (byte[]) recvdDg.getData();
            return data;
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
        
    }
    
    /**
     * Called by the server application layer protocol to close the connection. 
     * wait for the FIN from client and send FINACK
     * 
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void close() throws IOException, ClassNotFoundException {
        System.out.println("server close the connection");
        connClosed = true;
        //receive the FIN
        changeCloseListener();
        while(true){
            byte[] data = receiveData();
            if(data[8] == (byte)FIN){
                unackedPackets.clear();
                ackn = byteArrayToInt(new byte[]{ data[0], data[1], data[2], data[3]});
                expectedSeq =  ackn + 1;
                datagram.setSize((short) 9);
                datagram.setData(fillHeader(FINACK));
                //datagram.setChecksum((short)0);
                datagram.setChecksum(calcChecksum((byte[])datagram.getData()));
                ds.sendDatagram(datagram);

                System.out.println("FINACK sent to " + datagram.getDstaddr() + ":" + datagram.getDstport());
                unackedPackets.put(nextSeq, datagram);
                
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
    
    public void changeCloseListener(){
        clock.stop();
        clock.removeActionListener(dataListener);
        clock.addActionListener(closeServer);
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
    
    /**
     *  Action listener for when the FINACK times out
     */
    
    ActionListener closeServer = new ActionListener(){
        public void actionPerformed(ActionEvent event){
            System.out.println("Timeout for FINACK to :" + sourceKey);
            try {
                ds.sendDatagram(datagram);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            if(retrnsNum > 10){
                System.out.println("max retrns, delete the conn");
                server.openConnections.remove(sourceKey);
                clock.stop();
            }else{
                clock.restart();
                retrnsNum++;
            }
            
        }
    };

}
