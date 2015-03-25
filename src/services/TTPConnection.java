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
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.swing.Timer;

import datatypes.Datagram;

public class TTPConnection {
    protected DatagramService ds;
    protected Datagram datagram;
    String sourceKey;
    int retrnsNum;

    protected Datagram recvdDg;
    //sliding window variables
    protected int base;
    protected int N;
    protected int nextSeq;
    protected int expectedSeq;
    protected int ackn;

    protected int retrsTime;
    protected Timer clock;
    protected boolean connClosed;
    protected ConcurrentSkipListMap<Integer,Datagram> unackedPackets;
    
    

    
    public static final int FIN = 1;
    public static final int ACK = 2;
    public static final int FINACK = 3;
    public static final int SYN = 4;
    public static final int SYNACK = 6;
    public static final int END = 8;    
    public static final int FINACKACK = 16;
    public static final int DATA = 0;
    
    public static final int FLAG = 8;
    public static final int MAXLEN = 1291;
    public static final int HEADERLEN = 9;

    public TTPConnection(int N, int time) {
        datagram = new Datagram();
        recvdDg = new Datagram();
        unackedPackets = new ConcurrentSkipListMap<Integer,Datagram>();
        

        this.N = N;

        this.retrsTime = time;

        connClosed = false;

        Random rand = new Random();
        nextSeq = rand.nextInt(65536);
    }

    public TTPConnection(int N, int time, DatagramService ds) {
        this(N, time);
        this.ds = ds;
    }

 
    public int getAcknNum() {
        return ackn;
    }

    public void setAcknNum(int acknNum) {
        this.ackn = acknNum;
    }

    public int getExpectedSeqNum() {
        return expectedSeq;
    }

    public void setExpectedSeqNum(int expectedSeqNum) {
        this.expectedSeq = expectedSeqNum;
    }
    
    public int getBase() {
        return base;
    }

    public void setBase(int base) {
        this.base = base;
    }

    public int getNextSeqNum() {
        return nextSeq;
    }

    public void setNextSeqNum(int nextSeqNum) {
        this.nextSeq = nextSeqNum;
    }
    
    public Datagram getDatagram() {
        return datagram;
    }

    public void setDatagram(Datagram datagram) {
        this.datagram = datagram;
    }
    
    public void startClock(){
        clock.start();
    }
    public void stopClock(){
        clock.stop();
    }
    
    
    /**
     * Takes the various flags as a parameter and uses it to create a header consisting of Sequence Number,
     * Acknowledgement number and header flags
     * 
     * @param flags
     * @return byte[]
     */
    protected byte[] fillHeader(int flags) {
        
        byte[] header = new byte[9];
        byte[] sn = ByteBuffer.allocate(4).putInt(nextSeq).array();
        byte[] ack = ByteBuffer.allocate(4).putInt(ackn).array();
        

        switch (flags) {
        case FIN:
            for (int i = 0; i < 4; i++) {
                header[i] = sn[i];
            }
            for (int i = 4; i < 8; i++) {
                header[i] = (byte) 0;
            }
            header[8] = (byte) FIN;
            break;
            
        case SYN:
            for (int i = 0; i < 4; i++) {
                header[i] = sn[i];
            }
            for (int i = 4; i < 8; i++) {
                header[i] = (byte) 0;
            }
            header[8] = (byte) SYN;
            break;
        
            
        case SYNACK:
            for (int i = 0; i < 4; i++) {
                header[i] = sn[i];
            }
            for (int i = 4; i < 8; i++) {
                header[i] = ack[i - 4];
            }
            header[FLAG] = (byte) SYNACK;
            break;
            
        case DATA:
            for (int i = 0; i < 4; i++) {
                header[i] = sn[i];
            }
            for (int i = 4; i < 8; i++) {
                header[i] = (byte) 0;
            }
            header[8] = (byte) DATA;
            break;

        case ACK:
            for (int i = 0; i < 4; i++) {
                header[i] = sn[i];
            }
            for (int i = 4; i < 8; i++) {
                header[i] = ack[i - 4];
            }
            header[8] = (byte) ACK;
            break;
            
        case END:
            for (int i = 0; i < 4; i++) {
                header[i] = sn[i];
            }
            for (int i = 4; i < 8; i++) {
                header[i] = (byte) 0;
            }
            header[FLAG] = (byte) END;
            break;

        

        case FINACK:
            for (int i = 0; i < 4; i++) {
                header[i] = sn[i];
            }
            System.out.println(nextSeq);
            for (int i = 4; i < 8; i++) {
                header[i] = ack[i - 4];
            }
            header[8] = (byte) FINACK;
            break;

        case FINACKACK:
            for (int i = 0; i < 4; i++) {
                header[i] = sn[i];
            }
            for (int i = 4; i < 8; i++) {
                header[i] = ack[i - 4];
            }
            header[FLAG] = (byte) FINACKACK;
            break;
        }
        return header;
    }

    /**
     * Takes a byte array of data and calculates the checksum according to the UDP checksum discussed in class
     * 
     * @param payload
     * @return short
     * @throws IOException
     */
    
    protected short calcChecksum(byte[] payload) throws IOException {
        int j = 0, sum=0;

        int len = payload.length;
        
        int temp;
        int first, second;

        for(;len > 1;len=len-2) {
            first = (payload[j] << 8) & 0xFF00;
            second = (payload[j + 1]) & 0xFF;

            temp = first | second;
            sum += temp;

            if ((sum & 0xFFFF0000) > 0) {
                sum = sum & 0xFFFF;
                sum += 1;
            }

            j += 2;
            
        }

        if (len > 0) {
            sum += (payload[j] << 8 & 0xFF00);
            if ((sum & 0xFFFF0000) > 0) {
                sum = sum & 0xFFFF;
                sum += 1;
            }
        }

        sum = ~sum;
        sum = sum & 0xFFFF;
        return (short) sum;
    }
    
    private void sendFmt(Datagram datagram) throws IOException {
        ds.sendDatagram(datagram);
        unackedPackets.put(nextSeq, new Datagram(datagram.getSrcaddr(), datagram.getDstaddr(), datagram.getSrcport(), datagram.getDstport(), datagram.getSize(), datagram.getChecksum(), datagram.getData()));
        System.out.println("sent to packet  " + datagram.getDstaddr() + ":" + datagram.getDstport() + "  Seq No " + nextSeq);
     
    }
    
    private void packPackage (byte[] data, boolean last) throws IOException {

        byte[] header = new byte[HEADERLEN];
        if (last) {
            header = fillHeader(TTPConnection.END);
        } else {
            header = fillHeader(TTPConnection.DATA);
        }

        byte[] total = new byte[data.length + header.length];
        System.arraycopy(header, 0, total, 0, header.length);
        System.arraycopy(data, 0, total, header.length, data.length);
        
        datagram.setSize((short)total.length);
        datagram.setData(total);
        datagram.setChecksum(calcChecksum(total));
        if(base == nextSeq)
            clock.restart();
        sendFmt(datagram);       
        nextSeq++;
    }
    
    

    /**
     * Takes an array of byte, send until the sending window is full, then call receive to move the window, 
     * 
     * @param data
     * @throws IOException
     * @return int
     */
    
    public int send(byte[] data) throws IOException {
        
        byte[] fragment = null;
        int dataCounter = 0;
        int currentCounter;
        int indexController = 0;
        int length = data.length;

            clock.start();
            if (length > MAXLEN) {

                while (length > 0) {
                    currentCounter = dataCounter;
                    indexController = Math.min(length , MAXLEN);
                    fragment = new byte[indexController];

                    for (int i = currentCounter; i < currentCounter + indexController; dataCounter++, i++) {
                        fragment[i % MAXLEN] = data[i];
                    }
                    //need to wait for the ack to move forward
                    System.out.println("len " + length + " nextSeqNum" + nextSeq + " base" + base + " N" + N);
                    while(nextSeq >= base + N){
                        try {
                            //System.out.println("sending window full, start timer and wait for ack");
                            
                            receiveAck();
                            if(connClosed == true)
                                return data.length - length;
                        } catch (ClassNotFoundException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                    if (length > MAXLEN)
                        packPackage(fragment, false);
                    else
                        packPackage(fragment, true);

                    length -= MAXLEN;

                }
            } else {
              //need to wait for the ack to move forward
                while(nextSeq >= base + N){
                    try {
                        System.out.println("reach full send window " + nextSeq +" "+ base + " " + N);
                        receiveAck();
                        if(connClosed == true)
                            return -1;
                    } catch (ClassNotFoundException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                
                packPackage(data, true);
            }
            System.out.println("has send all packets and wait for the ack");
            try {
                while(true){
                    receiveAck();
                    if(unackedPackets.isEmpty())
                        break;
                }
            } catch (ClassNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return data.length;
        
    }

    

    /**
     * The receive data function . It reads the incoming packets
     * 
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    protected byte[] receiveData() throws IOException, ClassNotFoundException {
        if (ds != null) {
            recvdDg = ds.receiveDatagram(); 
        }

        byte[] data = (byte[]) recvdDg.getData();
        return data;
    }
    /**
     * The receive data function for the client side. It reads the incoming packets, and according
     * to the packet, it sends it up to the FTP client
     * 
     * @return byte[]
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public byte[] receive() throws IOException, ClassNotFoundException {
        
        while(true){
            byte[] data = receiveData();
            //System.out.println("receive got data");
            byte[] pdu = null;
            //System.out.println("datagram size "+ recvdDg.getSize() + "flag " + data[FLAG]);
            if (recvdDg.getSize() <= HEADERLEN) {
                
                if (data[FLAG] == (byte)SYNACK) {               
                    ackn = byteArrayToInt(new byte[]{ data[0], data[1], data[2], data[3]});
                    expectedSeq =  ackn + 1;
                    base = byteArrayToInt(new byte[]{ data[4], data[5], data[6], data[7]}) + 1;

                    System.out.println("Received SYNACK with seq no:" + ackn + " and Acknowledgement No " + (base-1));
                    Set<Integer> keys = unackedPackets.keySet();
                    for (Integer i: keys) {
                        if (i< base) {
                            unackedPackets.remove(i);
                        }
                    }
                    SYNAcknowledgement();
                    continue;
                }
                if(data[FLAG]== (byte)ACK) {
                    base = byteArrayToInt(new byte[]{ data[4], data[5], data[6], data[7]}) + 1;
                    System.out.println("Received ACK for packet no:" + byteArrayToInt(new byte[]{ data[4], data[5], data[6], data[7]}) + "restart timer");
    
                    Set<Integer> keys = unackedPackets.keySet();
                    for (Integer i: keys) {
                        if (i< base) {
                            unackedPackets.remove(i);
                        }
                    }
                    //if there are unacked packets, restart the timer. Othewise, dont.
                    
                    if(!unackedPackets.isEmpty()){
                        //System.out.println("unack not empty, restart timer");
                        clock.restart();
                    }else{
                        //System.out.println("unack  empty, stop timer");
                        clock.stop();
                    }
                    continue;
                }
                if(data[FLAG] == (byte)FIN){
                    unackedPackets.clear();
                    System.out.println("receive FIN from " + datagram.getDstaddr() + ":" + datagram.getDstport() + " send FINACK");
                    ackn = byteArrayToInt(new byte[]{ data[0], data[1], data[2], data[3]});
                    expectedSeq =  ackn + 1;
                    datagram.setSize((short) 9);
                    datagram.setData(fillHeader(FINACK));
                    //datagram.setChecksum((short)-1);
                    datagram.setChecksum(calcChecksum((byte[])datagram.getData()));
                    ds.sendDatagram(datagram);
    
                    clock.restart();
                    //unackedPackets.put(nextSeq, new Datagram(datagram.getSrcaddr(), datagram.getDstaddr(), datagram.getSrcport(), datagram.getDstport(), datagram.getSize(), datagram.getChecksum(), datagram.getData()));
                    //nextSeq++;
                    //far end close the connection, just return a null to let app layer to konw that
                    connClosed = true;
                    return null;
                }
                if(data[FLAG]== (byte)FINACK) {
                    ackn = byteArrayToInt(new byte[]{ data[0], data[1], data[2], data[3]});
                    expectedSeq =  ackn + 1;
                    base = byteArrayToInt(new byte[]{ data[4], data[5], data[6], data[7]}) + 1;
                    System.out.println("Received FINACK with seq no:" + ackn );
    
                    if (ds!=null) {
                        finackAcknowledgement();
                    }
                    connClosed = true;
                    return null;
                }
               
            }else{
                int seqNum = byteArrayToInt(new byte[] { data[0], data[1], data[2], data[3]});
                System.out.println("seqNum " + seqNum + " expected "+ expectedSeq);
                if(byteArrayToInt(new byte[] { data[0], data[1], data[2], data[3]}) == expectedSeq) {
                    if (calcChecksum(data) != recvdDg.getChecksum()) {
                        System.out.println("packet " + seqNum + "Checksum error!!");
                        //discard the packet, do not ack and let sender to resend it
                        acknowledgement();
                        continue;
                    } else {
                        System.out.println("Received data with Seq no " + seqNum + " Checksum verified!!");
                        
                        ackn = byteArrayToInt(new byte[] { data[0], data[1], data[2], data[3]});
                        
                        acknowledgement();
                        expectedSeq++;
                        
                        if(data[FLAG]==END) {
                            pdu = new byte[data.length - HEADERLEN];
                            for (int i=0; i < pdu.length; i++) {
                                pdu[i] = data[i+9];
                            }
                            
                        }
                        else if(data[FLAG]== 0) {
                            
                            ArrayList<Byte> dataList = reassemPacket(data);
                            pdu = new byte[dataList.size()];
                            for (int i=0;i<dataList.size();i++) {
                                pdu[i] = (byte)dataList.get(i);
                            }
                        }
                    }
                }
                else {
                    acknowledgement();
                    continue;
                }
            }
            return pdu;
        }
    }
    /**
     * receive acknowledgment after send the packets. It will be called by send()
     * when the sending window is full
     * 
     * @param payload
     * @return short
     * @throws IOException
     * @return int
     */
public int receiveAck() throws IOException, ClassNotFoundException {
        
        while(true){
            byte[] data = receiveData();
            //System.out.println("receiveAck got data");
            byte[] pdu = null;
            //System.out.println("datagram size "+ recvdDg.getSize() + "flag " + data[FLAG] + "sq " + byteArrayToInt(new byte[]{ data[4], data[5], data[6], data[7]}));
            if (recvdDg.getSize() <= HEADERLEN) {
                
                if (data[FLAG] == (byte)SYNACK) {               
                    ackn = byteArrayToInt(new byte[]{ data[0], data[1], data[2], data[3]});
                    expectedSeq =  ackn + 1;
                    base = byteArrayToInt(new byte[]{ data[4], data[5], data[6], data[7]}) + 1;

                    System.out.println("Received SYNACK with seq no:" + ackn + " and Acknowledgement No " + (base-1));
                    Set<Integer> keys = unackedPackets.keySet();
                    for (Integer i: keys) {
                        if (i< base) {
                            unackedPackets.remove(i);
                        }
                    }
                    SYNAcknowledgement();
                    continue;
                }
                if(data[FLAG]== (byte)ACK) {
                    int sn = byteArrayToInt(new byte[]{ data[4], data[5], data[6], data[7]}) + 1;
                    
                    System.out.println("Received ACK for packet no:" + byteArrayToInt(new byte[]{ data[4], data[5], data[6], data[7]}) + "restart timer");
                    if(sn > base+N){
                        System.out.println("ack sn is larger than window upper limit ");
                        continue;
                    }
                    if(sn < base){
                        System.out.println("ack sn is smaller than window lower limit ");
                        continue;
                    }
                    base = sn;
                    Set<Integer> keys = unackedPackets.keySet();
                    for (Integer i: keys) {
                        if (i< base) {
                            unackedPackets.remove(i);
                        }
                    }
                    //if there are unacked packets, restart the timer. Othewise, dont.
                    
                    if(!unackedPackets.isEmpty()){
                        //System.out.println("unack not empty, restart timer");
                        clock.restart();
                    }else{
                        //System.out.println("unack  empty, stop timer");
                        clock.stop();
                    }
                    return 1;
                }
                if(data[FLAG] == (byte)FIN){
                    
                    
                    System.out.println("receive FIN from " + datagram.getDstaddr() + ":" + datagram.getDstport() + " send FINACK");
                     
                    connClosed = true;
                    return -1;
                    
                }
                
               
            }else{
                continue;
            }
        }
}
    /**
     * Takes a byte array of data from the first fragment of fragmented data, and  waits to 
     * receive the remaining fragments. It then reassembles the data and returns it
     * 
     * @param packet
     * @return ArrayList<Byte>
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private ArrayList<Byte> reassemPacket(byte[] packet) throws IOException, ClassNotFoundException {
        ArrayList<Byte> totalData = new ArrayList<Byte>();

        for(int i=9;i < packet.length;i++) {
            totalData.add(packet[i]);
        }

        while(true) {
            
            byte[] data = receiveData();
            int seqNum = byteArrayToInt(new byte[] { data[0], data[1], data[2], data[3]});
            
            System.out.println("reasamble seqNum " + seqNum + " expected " + expectedSeq + " flag " + data[FLAG]);
            if (calcChecksum(data) != recvdDg.getChecksum()){
                System.out.println("checksum failed seqNum "+seqNum);
                acknowledgement();
            }else if(byteArrayToInt(new byte[] { data[0], data[1], data[2], data[3]}) == expectedSeq) {
                for(int i=9;i < data.length;i++) {
                    totalData.add(data[i]);
                }
                ackn = byteArrayToInt(new byte[] { data[0], data[1], data[2], data[3]});

                
                nextSeq++;
                expectedSeq++;
                acknowledgement();
                if(data[FLAG]==END) {
                    
                    break;
                }
                
            }
            else {
                acknowledgement();
            }
            
        }
        
        return totalData;
    }


    public String getSourceKey() {
        return sourceKey;
    }

    public void setSourceKey(String sourceKey) {
        this.sourceKey = sourceKey;
    }
    
    protected void acknowledgement() throws IOException {
        datagram.setSize((short)9);
        datagram.setData(fillHeader(ACK));
        //datagram.setChecksum((short)0);
        datagram.setChecksum(calcChecksum((byte[])datagram.getData()));
        if(ds != null){
            ds.sendDatagram(datagram);
            System.out.println("Acknowledgement sent! No:" + ackn);
        }
    }
    
    protected void SYNAcknowledgement() throws IOException {
        datagram.setSize((short)9);
        datagram.setData(fillHeader(SYNACK));
        //datagram.setChecksum((short)0);
        datagram.setChecksum(calcChecksum((byte[])datagram.getData()));
        if(ds != null){
            ds.sendDatagram(datagram);
            System.out.println("Acknowledgement sent! No:" + ackn);
        }
    }

    protected void finackAcknowledgement() throws IOException {
        
        datagram.setSize((short)9);
        datagram.setData(fillHeader(FINACKACK));
        //datagram.setChecksum((short)0);
        
        datagram.setChecksum(calcChecksum((byte[])datagram.getData()));
        if(ds != null){
            ds.sendDatagram(datagram);
            System.out.println("Acknowledgement for FINACK seq:" + ackn);
        }
        
        nextSeq++;
        clock.removeActionListener(closeClient);
        clock.addActionListener(finalAck);
        clock.restart();
        
    }

    
    protected int byteArrayToInt(byte[] b) {
        int v = 0;
        for (int i = 0; i < b.length; i++) {
            int shift = (b.length - i - 1) * 8;
            v += (b[i] & 0x000000FF) << shift;
        }
        return v;
    }
    
    /**
     *  Action listener for when the packet times out waiting for ACK
     */
    ActionListener dataListener = new ActionListener(){
        public void actionPerformed(ActionEvent event){
            //System.out.println("Ack timeout ");
            if(ds == null || connClosed == true){
                System.out.println("conn closed, stop the timer");
                clock.stop();
                unackedPackets.clear();
                return;
            }
            Iterator<Entry<Integer, Datagram>> element = unackedPackets.entrySet().iterator();
            while (element.hasNext()) {
                try {
                    Entry<Integer,Datagram> p = element.next();
                    System.out.println("retransmit Datagram  seqNum " + p.getKey());
                    ds.sendDatagram(p.getValue());
                    
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }           
            clock.restart();
        }
    };
    
    
    
    /**
     *  Timeout listener for  the FINACKACK message
     */
    ActionListener finalAck = new ActionListener(){
        public void actionPerformed(ActionEvent event){
            clock.stop();
            ds = null;
            
            System.out.println("TTP Client connection closesed !");
        }
    };
    
    /**
     *  Action listener for when the packet times out
     */
    
    ActionListener closeClient = new ActionListener(){
        public void actionPerformed(ActionEvent event){
            System.out.println("Timeout for FIN to :" + sourceKey + "retran number " + retrnsNum);
            try {
                ds.sendDatagram(datagram);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            if(retrnsNum > 10){
                System.out.println("max retrns, delete the conn");
                
                clock.stop();
            }else{
                clock.restart();
                retrnsNum++;
            }
            
        }
    };

}
