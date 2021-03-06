package services;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.Timer;

import services.DatagramService;
import datatypes.Datagram;

public class TTPServer extends TTPConnection {
    public ConcurrentHashMap<String, TTPServerEnd> openConnections= new ConcurrentHashMap<String, TTPServerEnd>();
    

    public TTPServer(int N, int time) {
        super(N,time);
        

        this.N = N;

        this.retrsTime = time;

 

        Random rand = new Random();
        nextSeq = rand.nextInt(65536);
    }

public void open(int srcPort, int ver){
    try {
        ds = new DatagramService(srcPort, ver);
    } catch (SocketException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
}



    
    /**
     * Called by the application layer protocol. Continuously receive datagram from lower layer and creates
     * a new TTP connection end point for every connection request. For the packets for existing connections
     * send to the blockQueue of the connection
     * 
     * @return TTPServerEnd
     * @throws IOException
     * @throws ClassNotFoundException
     */
    
    public TTPServerEnd accept() throws IOException, ClassNotFoundException {
        while(true){
            Datagram request = ds.receiveDatagram(); 
            byte[] data = (byte[]) request.getData();
            TTPServerEnd server_endPoint = null;
            String sourceKey = request.getSrcaddr() + ":" + request.getSrcport();
            
            //System.out.println("TTPServer receive dg " + data[8]);
            //System.out.println("data: "+ Arrays.toString(data)+ "chsum " + request.getChecksum());
            if (data[8] == (byte)SYN) {
                if (calcChecksum(data) != request.getChecksum()){
                    System.out.println("TTPServer receive SYN with bad checksum");
                    continue;
                }
                if(!openConnections.containsKey(sourceKey)) {
                    server_endPoint = new TTPServerEnd(N, retrsTime,ds, this);
                    openConnections.put(sourceKey, server_endPoint);
                    ackn = byteArrayToInt(new byte[]{ data[0], data[1], data[2], data[3]});
                    Random rand = new Random();
                    nextSeq = rand.nextInt(65536);
                    Datagram datagram = new Datagram();
                    datagram.setSrcaddr(request.getDstaddr());
                    datagram.setDstaddr(request.getSrcaddr());
                    datagram.setSrcport((short) request.getDstport());
                    datagram.setDstport((short) request.getSrcport());
                    datagram.setSize((short) 9);
                    datagram.setData(fillHeader(TTPConnection.SYNACK));
                    datagram.setChecksum((short)-1);
                    this.ds.sendDatagram(datagram);
                    //start the timer for SYNACK
                    server_endPoint.startClock();
                    
                    System.out.println("SYNACK sent to " + datagram.getDstaddr() + ":" + datagram.getDstport() + " with Seq no " + nextSeq);
    
                    expectedSeq = ackn + 1;
                    
    
                    server_endPoint.setSourceKey(sourceKey);
                    server_endPoint.setNextSeqNum(nextSeq);
                    //server_endPoint.setBase(nextSeqNum);
                    server_endPoint.setAcknNum(ackn);
                    server_endPoint.setExpectedSeqNum(expectedSeq);
                    server_endPoint.setDatagram(datagram);
                    
                    System.out.println("Received SYN from:" + sourceKey);
                }
                
                else {
                    System.out.println("Duplicate SYN detected!!");
                              
                }
            }
            else if (data[8] == (byte)SYNACK) {  
                if (calcChecksum(data) != request.getChecksum()){
                    System.out.println("TTPServer receive SYNACN with bad checksum");
                    continue;
                }
                if(openConnections.containsKey(sourceKey)) {
                    
                    server_endPoint = openConnections.get(sourceKey);
                    if(server_endPoint.isConnected()){
                        System.out.println("duplicated SYNACK");
                        continue;
                    }
                    server_endPoint.stopClock();
                    server_endPoint.changeClockListener();
                    server_endPoint.setConnected(true);
                    //acknNum = byteArrayToInt(new byte[]{ data[0], data[1], data[2], data[3]});
                    //expectedSeqNum =  acknNum + 1;
                    base = byteArrayToInt(new byte[]{ data[4], data[5], data[6], data[7]}) + 1;
                    server_endPoint.setNextSeqNum(base);
                    //server_endPoint.setAcknNum(acknNum);
                    server_endPoint.setBase(base);
                    //server_endPoint.setExpectedSeqNum(expectedSeqNum);
                    
                    BlockingQueue<Datagram> dgQ = new LinkedBlockingQueue<Datagram>();
                    server_endPoint.setDgQ(dgQ);
                    System.out.println("Received SYNACK with seq no:" + ackn + " and Acknowledgement No " + (base-1));
                    return server_endPoint;
                }
            }
            else if (data[8]== (byte)FINACKACK) {
                if (calcChecksum(data) != request.getChecksum()){
                    System.out.println("TTPServer receive FINACKACK with bad checksum");
                    continue;
                }
                if(openConnections.containsKey(sourceKey)) {
                    server_endPoint = openConnections.get(sourceKey);
                    server_endPoint.stopClock();
                    openConnections.remove(sourceKey);
                    System.out.println("Connection " + sourceKey + " closed at server !");
                }
            }
            else {
                if (calcChecksum(data) != request.getChecksum()){
                    System.out.println("TTPServer receive ACK/DATA with bad checksum");
                    continue;
                }
                if(openConnections.containsKey(sourceKey)) {
                    
                    //System.out.println("Received ACK/DATA from existing client");
                    //use blockingQueue to notify the connection end
                    server_endPoint = openConnections.get(sourceKey);
                    
                    try {
                        if (server_endPoint.getDgQ() != null)
                            server_endPoint.getDgQ().put(request);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    
                }
            }
            
                
        }
    }


    
}
