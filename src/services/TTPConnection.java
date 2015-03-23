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


    protected Datagram recdDatagram;
    String sourceKey;


    protected int base;


    protected int nextSeqNum;
    protected int N;
    protected int acknNum;

    protected int expectedSeqNum;
    protected int nextFragment;
    protected int time;
    protected Timer clock;
    protected ConcurrentSkipListMap<Integer,Datagram> unackedPackets;
    
    protected boolean connClosed;

    
    public static final int FIN = 1;
    public static final int ACK = 2;
    public static final int FINACK = 3;
    public static final int SYN = 4;
    public static final int SYNACK = 6;
    public static final int END = 8;    
    public static final int FINACKACK = 16;
    public static final int DATA = 0;
    
    public static final int FLAG = 8;
    public static final int MAXLEN = 1281;
    public static final int HEADERLEN = 9;

    public TTPConnection(int N, int time) {
        datagram = new Datagram();
        recdDatagram = new Datagram();
        unackedPackets = new ConcurrentSkipListMap<Integer,Datagram>();
        

        this.N = N;

        this.time = time;

        connClosed = false;

        Random rand = new Random();
        nextSeqNum = rand.nextInt(65536);
    }

    public TTPConnection(int N, int time, DatagramService ds) {
        this(N, time);
        this.ds = ds;
    }

 
    public int getAcknNum() {
        return acknNum;
    }

    public void setAcknNum(int acknNum) {
        this.acknNum = acknNum;
    }

    public int getExpectedSeqNum() {
        return expectedSeqNum;
    }

    public void setExpectedSeqNum(int expectedSeqNum) {
        this.expectedSeqNum = expectedSeqNum;
    }
    
    public int getBase() {
        return base;
    }

    public void setBase(int base) {
        this.base = base;
    }

    public int getNextSeqNum() {
        return nextSeqNum;
    }

    public void setNextSeqNum(int nextSeqNum) {
        this.nextSeqNum = nextSeqNum;
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
     * @return
     */
    protected byte[] fillHeader(int flags) {
        byte[] isnBytes = ByteBuffer.allocate(4).putInt(nextSeqNum).array();
        byte[] ackBytes = ByteBuffer.allocate(4).putInt(acknNum).array();
        byte[] header = new byte[9];
        

        switch (flags) {
        case SYN:
            for (int i = 0; i < 4; i++) {
                header[i] = isnBytes[i];
            }
            for (int i = 4; i < 8; i++) {
                header[i] = (byte) 0;
            }
            header[8] = (byte) SYN;
            break;
            
        case SYNACK:
            for (int i = 0; i < 4; i++) {
                header[i] = isnBytes[i];
            }
            for (int i = 4; i < 8; i++) {
                header[i] = ackBytes[i - 4];
            }
            header[FLAG] = (byte) SYNACK;
            break;
            
        case DATA:
            for (int i = 0; i < 4; i++) {
                header[i] = isnBytes[i];
            }
            for (int i = 4; i < 8; i++) {
                header[i] = (byte) 0;
            }
            header[8] = (byte) DATA;
            break;

        case ACK:
            for (int i = 0; i < 4; i++) {
                header[i] = isnBytes[i];
            }
            for (int i = 4; i < 8; i++) {
                header[i] = ackBytes[i - 4];
            }
            header[8] = (byte) ACK;
            break;
            
        case END:
            for (int i = 0; i < 4; i++) {
                header[i] = isnBytes[i];
            }
            for (int i = 4; i < 8; i++) {
                header[i] = (byte) 0;
            }
            header[FLAG] = (byte) END;
            break;

        case FIN:
            for (int i = 0; i < 4; i++) {
                header[i] = isnBytes[i];
            }
            for (int i = 4; i < 8; i++) {
                header[i] = (byte) 0;
            }
            header[8] = (byte) FIN;
            break;

        case FINACK:
            for (int i = 0; i < 4; i++) {
                header[i] = isnBytes[i];
            }
            System.out.println(nextSeqNum);
            for (int i = 4; i < 8; i++) {
                header[i] = ackBytes[i - 4];
            }
            header[8] = (byte) FINACK;
            break;

        case FINACKACK:
            for (int i = 0; i < 4; i++) {
                header[i] = isnBytes[i];
            }
            for (int i = 4; i < 8; i++) {
                header[i] = ackBytes[i - 4];
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
     * @return
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
        System.out.println("Data sent to " + datagram.getDstaddr() + ":" + datagram.getDstport() + " with Seq No " + nextSeqNum);

        

        unackedPackets.put(nextSeqNum, new Datagram(datagram.getSrcaddr(), datagram.getDstaddr(), datagram.getSrcport(), datagram.getDstport(), datagram.getSize(), datagram.getChecksum(), datagram.getData()));
    }
    
    private void packPackage (byte[] fragment, boolean lastFragment) throws IOException {

        byte[] header = new byte[9];
        if (lastFragment) {
            header = fillHeader(TTPConnection.END);
        } else {
            header = fillHeader(TTPConnection.DATA);
        }

        byte[] headerPlusData = new byte[fragment.length + header.length];
        System.arraycopy(header, 0, headerPlusData, 0, header.length);
        System.arraycopy(fragment, 0, headerPlusData, header.length, fragment.length);

        datagram.setData(headerPlusData);
        datagram.setSize((short)headerPlusData.length);
        datagram.setChecksum(calcChecksum(headerPlusData));

        
        
        sendFmt(datagram);
        
        nextSeqNum++;
    }
    
    

    /**
     * Takes an array of byte, send until the sending window is full, then call receive to move the window, 
     * 
     * @param data
     * @throws IOException
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
                    System.out.println("len " + length + " nextSeqNum" + nextSeqNum + " base" + base + " N" + N);
                    while(nextSeqNum >= base + N){
                        try {
                            System.out.println("sending window full, start timer and wait for ack");
                            
                            receive();
                            if(connClosed == true)
                                return -1;
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
                while(nextSeqNum >= base + N){
                    try {
                        System.out.println("reach full send window " + nextSeqNum +" "+ base + " " + N);
                        receive();
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
                    receive();
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
            recdDatagram = ds.receiveDatagram(); 
        }

        byte[] data = (byte[]) recdDatagram.getData();
        return data;
    }
    /**
     * The receive data function for the client side. It reads the incoming packets, and according
     * to the packet, it sends it up to the FTP client
     * 
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public byte[] receive() throws IOException, ClassNotFoundException {
        
        byte[] data = receiveData();
        System.out.println("receive got data");
        byte[] pdu = null;
        System.out.println("datagram size "+ recdDatagram.getSize() + "flag " + data[FLAG]);
        if (recdDatagram.getSize() <= HEADERLEN) {
            
            if (data[FLAG] == (byte)SYNACK) {               
                acknNum = byteArrayToInt(new byte[]{ data[0], data[1], data[2], data[3]});
                expectedSeqNum =  acknNum + 1;
                base = byteArrayToInt(new byte[]{ data[4], data[5], data[6], data[7]}) + 1;
                clock.stop();
                System.out.println("Received SYNACK with seq no:" + acknNum + " and Acknowledgement No " + (base-1));
                Set<Integer> keys = unackedPackets.keySet();
                for (Integer i: keys) {
                    if (i< base) {
                        unackedPackets.remove(i);
                    }
                }
                sendSYNAcknowledgement();
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
                
                clock.restart();
            }
            if(data[FLAG] == (byte)FIN){
                unackedPackets.clear();
                acknNum = byteArrayToInt(new byte[]{ data[0], data[1], data[2], data[3]});
                expectedSeqNum =  acknNum + 1;
                datagram.setSize((short) 9);
                datagram.setData(fillHeader(FINACK));
                datagram.setChecksum((short)-1);
                ds.sendDatagram(datagram);

                System.out.println("FINACK sent to " + datagram.getDstaddr() + ":" + datagram.getDstport());

                clock.restart();
                unackedPackets.put(nextSeqNum, new Datagram(datagram.getSrcaddr(), datagram.getDstaddr(), datagram.getSrcport(), datagram.getDstport(), datagram.getSize(), datagram.getChecksum(), datagram.getData()));
                nextSeqNum++;
                //far end close the connection, just return a null to let app layer to konw that
                connClosed = true;
                return null;
            }
            if(data[FLAG]== (byte)FINACK) {
                acknNum = byteArrayToInt(new byte[]{ data[0], data[1], data[2], data[3]});
                expectedSeqNum =  acknNum + 1;
                base = byteArrayToInt(new byte[]{ data[4], data[5], data[6], data[7]}) + 1;
                System.out.println("Received FINACK with seq no:" + acknNum );

                if (ds!=null) {
                    sendFinackAcknowledgement();
                }
                connClosed = true;
                return null;
            }
           
        }else{
            int seqNum = byteArrayToInt(new byte[] { data[0], data[1], data[2], data[3]});
            System.out.println("seqNum " + seqNum + " expected "+ expectedSeqNum);
            if(byteArrayToInt(new byte[] { data[0], data[1], data[2], data[3]}) == expectedSeqNum) {
                if (calcChecksum(data) != recdDatagram.getChecksum()) {
                    System.out.println("packet " + seqNum + "Checksum error!!");
                    //discard the packet, do not ack and let sender to resend it
                } else {
                    System.out.println("Received data with Seq no " + seqNum + " Checksum verified!!");
                    
                    acknNum = byteArrayToInt(new byte[] { data[0], data[1], data[2], data[3]});
                    
                    sendAcknowledgement();
                    expectedSeqNum++;
                    
                    if(data[FLAG]==END) {
                        pdu = new byte[data.length - HEADERLEN];
                        for (int i=0; i < pdu.length; i++) {
                            pdu[i] = data[i+9];
                        }
                        
                    }
                    else if(data[FLAG]== 0) {
                        
                        ArrayList<Byte> dataList = reassemble(data);
                        pdu = new byte[dataList.size()];
                        for (int i=0;i<dataList.size();i++) {
                            pdu[i] = (byte)dataList.get(i);
                        }
                    }
                }
            }
            else {
                sendAcknowledgement();
            }
        }
        return pdu;
    }

    /**
     * Takes a byte array of data which is the first fragment of fragmented data, and then waits to 
     * receive the remaining packets of the fragment. It then reassembles the data and returns it
     * 
     * @param data2
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private ArrayList<Byte> reassemble(byte[] data2) throws IOException, ClassNotFoundException {
        ArrayList<Byte> reassembledData = new ArrayList<Byte>();

        for(int i=9;i < data2.length;i++) {
            reassembledData.add(data2[i]);
        }

        while(true) {
            //recdDatagram = ds.receiveDatagram(); 
            byte[] data = receiveData();
            int seqNum = byteArrayToInt(new byte[] { data[0], data[1], data[2], data[3]});
            System.out.println("reasamble seqNum " + seqNum + " expected " + expectedSeqNum + " flag " + data[8]);
            if(byteArrayToInt(new byte[] { data[0], data[1], data[2], data[3]}) == expectedSeqNum) {
                acknNum = byteArrayToInt(new byte[] { data[0], data[1], data[2], data[3]});

                for(int i=9;i < data.length;i++) {
                    reassembledData.add(data[i]);
                }

                sendAcknowledgement();
                nextSeqNum++;
                expectedSeqNum++;

                if(data[8]==0) {
                    continue;
                }
                else if(data[8]==8) {
                    break;
                }
            }
            else {
                sendAcknowledgement();
            }
        }
        return reassembledData;
    }

    public String getSourceKey() {
        return sourceKey;
    }

    public void setSourceKey(String sourceKey) {
        this.sourceKey = sourceKey;
    }
    
    protected void sendAcknowledgement() throws IOException {
        datagram.setSize((short)9);
        datagram.setData(fillHeader(ACK));
        datagram.setChecksum((short)-1);
        if(ds != null){
            ds.sendDatagram(datagram);
            System.out.println("Acknowledgement sent! No:" + acknNum);
        }
    }
    
    protected void sendSYNAcknowledgement() throws IOException {
        datagram.setSize((short)9);
        datagram.setData(fillHeader(SYNACK));
        datagram.setChecksum((short)-1);
        ds.sendDatagram(datagram);
        System.out.println("Acknowledgement sent! No:" + acknNum);
    }

    protected void sendFinackAcknowledgement() throws IOException {
        datagram.setSize((short)9);
        datagram.setData(fillHeader(FINACKACK));
        datagram.setChecksum((short)-1);
        ds.sendDatagram(datagram);
        System.out.println("Acknowledgement sent for FINACK! No:" + acknNum);

        clock.removeActionListener(listener);
        clock.addActionListener(deleteClient);
        clock.restart();
        nextSeqNum++;
    }

    /**
     *  Action listener for when the packet times out
     */
    
    ActionListener listener = new ActionListener(){
        public void actionPerformed(ActionEvent event){
            System.out.println("Timeout for Packet " + base);
            Iterator<Entry<Integer, Datagram>> it = unackedPackets.entrySet().iterator();
            while (it.hasNext()) {
                try {
                    Entry<Integer,Datagram> pair = it.next();
                    ds.sendDatagram(pair.getValue());
                    System.out.println("Datagram with sequence number " + pair.getKey() + " resent!!");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }           
            clock.restart();
        }
    };
    
    
    
    /**
     *  Action listener for when the FINACKACK times out, thus signalling to the client
     *  that the connection has been closed
     */
    ActionListener deleteClient = new ActionListener(){
        public void actionPerformed(ActionEvent event){
            ds = null;
            clock.stop();
            System.out.println("TTP Client closes connection!");
        }
    };




    protected static int byteArrayToInt(byte[] b) {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            int shift = (4 - 1 - i) * 8;
            value += (b[i] & 0x000000FF) << shift;
        }
        return value;
    }

}
