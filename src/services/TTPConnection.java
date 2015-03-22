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
    protected ConcurrentSkipListMap<Integer,Datagram> unacknowledgedPackets;
    protected LinkedList<Datagram> sendBuffer;

    public static final int SYN = 0;
    public static final int ACK = 1;
    public static final int FIN = 2;
    public static final int DATA = 3;
    public static final int EOFDATA = 4;
    public static final int SYNACK = 5;
    public static final int FINACK = 6;
    public static final int FINACKACK = 7;

    public TTPConnection(int N, int time) {
        datagram = new Datagram();
        recdDatagram = new Datagram();
        unacknowledgedPackets = new ConcurrentSkipListMap<Integer,Datagram>();
        sendBuffer = new LinkedList<Datagram>();

        this.N = N;

        this.time = time;

        

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
    protected byte[] createPayloadHeader(int flags) {
        byte[] header = new byte[9];
        byte[] isnBytes = ByteBuffer.allocate(4).putInt(nextSeqNum).array();
        byte[] ackBytes = ByteBuffer.allocate(4).putInt(acknNum).array();

        switch (flags) {
        case SYN:
            for (int i = 0; i < 4; i++) {
                header[i] = isnBytes[i];
            }
            for (int i = 4; i < 8; i++) {
                header[i] = (byte) 0;
            }
            header[8] = (byte) 4;
            break;

        case ACK:
            for (int i = 0; i < 4; i++) {
                header[i] = isnBytes[i];
            }
            for (int i = 4; i < 8; i++) {
                header[i] = ackBytes[i - 4];
            }
            header[8] = (byte) 2;
            break;

        case FIN:
            for (int i = 0; i < 4; i++) {
                header[i] = isnBytes[i];
            }
            for (int i = 4; i < 8; i++) {
                header[i] = (byte) 0;
            }
            header[8] = (byte) 1;
            break;

        case DATA:
            for (int i = 0; i < 4; i++) {
                header[i] = isnBytes[i];
            }
            for (int i = 4; i < 8; i++) {
                header[i] = (byte) 0;
            }
            header[8] = (byte) 0;
            break;

        case EOFDATA:
            for (int i = 0; i < 4; i++) {
                header[i] = isnBytes[i];
            }
            for (int i = 4; i < 8; i++) {
                header[i] = (byte) 0;
            }
            header[8] = (byte) 8;
            break;

        case SYNACK:
            for (int i = 0; i < 4; i++) {
                header[i] = isnBytes[i];
            }
            for (int i = 4; i < 8; i++) {
                header[i] = ackBytes[i - 4];
            }
            header[8] = (byte) 6;
            break;

        case FINACK:
            for (int i = 0; i < 4; i++) {
                header[i] = isnBytes[i];
            }
            System.out.println(nextSeqNum);
            for (int i = 4; i < 8; i++) {
                header[i] = ackBytes[i - 4];
            }
            header[8] = (byte) 3;
            break;

        case FINACKACK:
            for (int i = 0; i < 4; i++) {
                header[i] = isnBytes[i];
            }
            for (int i = 4; i < 8; i++) {
                header[i] = ackBytes[i - 4];
            }
            header[8] = (byte) 16;
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
    
    protected short calculateChecksum(byte[] payload) throws IOException {
        int length = payload.length;
        int i = 0;

        int sum = 0;
        int data, firstByte, secondByte;

        while (length > 1) {
            firstByte = (payload[i] << 8) & 0xFF00;
            secondByte = (payload[i + 1]) & 0xFF;

            data = firstByte | secondByte;
            sum += data;

            if ((sum & 0xFFFF0000) > 0) {
                sum = sum & 0xFFFF;
                sum += 1;
            }

            i += 2;
            length -= 2;
        }

        if (length > 0) {
            sum += (payload[i] << 8 & 0xFF00);
            if ((sum & 0xFFFF0000) > 0) {
                sum = sum & 0xFFFF;
                sum += 1;
            }
        }

        sum = ~sum;
        sum = sum & 0xFFFF;
        return (short) sum;
    }

    /**
     * Takes a byte array of data, checks if the next sequence number is within the send window, 
     * encapsulates the data and sends it.
     * 
     * @param data
     * @throws IOException
     */
    
    public void send(byte[] data) throws IOException {
        int lengthOfData = data.length;
        byte[] fragment = null;
        int dataCounter = 0;
        int currentCounter;
        int indexController = 0;

       
            if (lengthOfData > 1281) {

                do {
                    currentCounter = dataCounter;
                    indexController = Math.min(lengthOfData , 1281);
                    fragment = new byte[indexController];

                    for (int i = currentCounter; i < currentCounter + indexController; dataCounter++, i++) {
                        fragment[i % 1281] = data[i];
                    }
                    //need to wait for the ack to move forward
                    while(nextSeqNum >= base + N){
                        try {
                            receive();
                        } catch (ClassNotFoundException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                    if (lengthOfData > 1281)
                        encapsulate(fragment, false);
                    else
                        encapsulate(fragment, true);

                    lengthOfData -= 1281;

                } while (lengthOfData > 0);
            } else {
              //need to wait for the ack to move forward
                while(nextSeqNum >= base + N){
                    try {
                        receive();
                    } catch (ClassNotFoundException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                fragment = data.clone();
                encapsulate(fragment, true);
            }
        
    }

    private void encapsulate (byte[] fragment, boolean lastFragment) throws IOException {

        byte[] header = new byte[9];
        if (lastFragment) {
            header = createPayloadHeader(TTPConnection.EOFDATA);
        } else {
            header = createPayloadHeader(TTPConnection.DATA);
        }

        byte[] headerPlusData = new byte[fragment.length + header.length];
        System.arraycopy(header, 0, headerPlusData, 0, header.length);
        System.arraycopy(fragment, 0, headerPlusData, header.length, fragment.length);

        datagram.setData(headerPlusData);
        datagram.setSize((short)headerPlusData.length);
        datagram.setChecksum(calculateChecksum(headerPlusData));

        
        
            sendFragment(datagram);
        
        nextSeqNum++;
    }
    
    private void sendFragment(Datagram datagram) throws IOException {
        ds.sendDatagram(datagram);
        System.out.println("Data sent to " + datagram.getDstaddr() + ":" + datagram.getDstport() + " with Seq No " + nextFragment);

        if (base == nextSeqNum) {
            clock.restart();
        }

        unacknowledgedPackets.put(nextSeqNum, new Datagram(datagram.getSrcaddr(), datagram.getDstaddr(), datagram.getSrcport(), datagram.getDstport(), datagram.getSize(), datagram.getChecksum(), datagram.getData()));
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
        byte[] app_data = null;

        if (recdDatagram.getSize() > 9) {
            if(byteArrayToInt(new byte[] { data[0], data[1], data[2], data[3]}) == expectedSeqNum) {
                if (calculateChecksum(data) != recdDatagram.getChecksum()) {
                    System.out.println("Checksum error!!");
                    sendAcknowledgement();
                } else {
                    System.out.println("Checksum verified!!");
                    acknNum = byteArrayToInt(new byte[] { data[0], data[1], data[2], data[3]});
                    System.out.println("Received data with Seq no " + acknNum);

                    if(data[8]==8) {
                        app_data = new byte[data.length - 9];
                        for (int i=0; i < app_data.length; i++) {
                            app_data[i] = data[i+9];
                        }
                        sendAcknowledgement();
                        expectedSeqNum++;
                    }
                    else if(data[8]== 0) {
                        sendAcknowledgement();
                        expectedSeqNum++;
                        ArrayList<Byte> dataList = reassemble(data);
                        app_data = new byte[dataList.size()];
                        for (int i=0;i<dataList.size();i++) {
                            app_data[i] = (byte)dataList.get(i);
                        }
                    }
                }
            }
            else {
                sendAcknowledgement();
            }
        } else {
            if (data[8] == (byte)6) {               
                acknNum = byteArrayToInt(new byte[]{ data[0], data[1], data[2], data[3]});
                expectedSeqNum =  acknNum + 1;
                base = byteArrayToInt(new byte[]{ data[4], data[5], data[6], data[7]}) + 1;
                clock.stop();
                System.out.println("Received SYNACK with seq no:" + acknNum + " and Acknowledgement No " + (base-1));
                Set<Integer> keys = unacknowledgedPackets.keySet();
                for (Integer i: keys) {
                    if (i< base) {
                        unacknowledgedPackets.remove(i);
                    }
                }
                sendSYNAcknowledgement();
            }
            if(data[8]== (byte)2) {
                base = byteArrayToInt(new byte[]{ data[4], data[5], data[6], data[7]}) + 1;
                System.out.println("Received ACK for packet no:" + byteArrayToInt(new byte[]{ data[4], data[5], data[6], data[7]}));

                Set<Integer> keys = unacknowledgedPackets.keySet();
                for (Integer i: keys) {
                    if (i< base) {
                        unacknowledgedPackets.remove(i);
                    }
                }
            }
            if(data[8] == (byte)1){
                unacknowledgedPackets.clear();
                acknNum = byteArrayToInt(new byte[]{ data[0], data[1], data[2], data[3]});
                expectedSeqNum =  acknNum + 1;
                datagram.setSize((short) 9);
                datagram.setData(createPayloadHeader(FINACK));
                datagram.setChecksum((short)-1);
                ds.sendDatagram(datagram);

                System.out.println("FINACK sent to " + datagram.getDstaddr() + ":" + datagram.getDstport());

                clock.restart();
                unacknowledgedPackets.put(nextSeqNum, new Datagram(datagram.getSrcaddr(), datagram.getDstaddr(), datagram.getSrcport(), datagram.getDstport(), datagram.getSize(), datagram.getChecksum(), datagram.getData()));
                nextSeqNum++;
            }
            if(data[8]== (byte)3) {
                acknNum = byteArrayToInt(new byte[]{ data[0], data[1], data[2], data[3]});
                expectedSeqNum =  acknNum + 1;
                base = byteArrayToInt(new byte[]{ data[4], data[5], data[6], data[7]}) + 1;
                System.out.println("Received FINACK with seq no:" + acknNum );

                if (ds!=null) {
                    sendFinackAcknowledgement();
                }
            }
            if(base == nextSeqNum) {
                clock.stop();
            } else {
                clock.restart();
            }
        }
        return app_data;
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
        datagram.setData(createPayloadHeader(ACK));
        datagram.setChecksum((short)-1);
        ds.sendDatagram(datagram);
        System.out.println("Acknowledgement sent! No:" + acknNum);
    }
    
    protected void sendSYNAcknowledgement() throws IOException {
        datagram.setSize((short)9);
        datagram.setData(createPayloadHeader(SYNACK));
        datagram.setChecksum((short)-1);
        ds.sendDatagram(datagram);
        System.out.println("Acknowledgement sent! No:" + acknNum);
    }

    protected void sendFinackAcknowledgement() throws IOException {
        datagram.setSize((short)9);
        datagram.setData(createPayloadHeader(FINACKACK));
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
            Iterator<Entry<Integer, Datagram>> it = unacknowledgedPackets.entrySet().iterator();
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
