package edu.ccsu.networking;

import edu.ccsu.util.HttpUtil;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;


public class RDTClient {
    private int serverPortNumber = 2010;
    private int clientPortNumber = 3010;
    private DatagramSocket socket = null;
    private InetAddress internetAddress = null;
    private boolean slowMode = false;
    private int packetSize = 128;
    private byte[] data = null;

    /**
     * Set up a client used for RDT
     * assign all private variables to the values passed
     *
     * @param targetAddress Address of the server
     * @param slowMode boolean to enable/disable slow mode
     */
    public RDTClient(InetAddress targetAddress, boolean slowMode) throws SocketException, UnknownHostException {
        this.internetAddress = targetAddress;
        this.slowMode = slowMode;

        socket = new DatagramSocket(this.clientPortNumber);
        
    }
    

    /**
     * Closes socket
     */
    private void closeSocket(){
        if (socket!=null){
            socket.close();
        }
    }

    public void rdtRequest(String name) throws IOException, InterruptedException {

        // Create a request for the file, assume it's going to get there
        byte[] request = HttpUtil.createRequestHeader("REQUEST", name);

        DatagramPacket requestPacket = new DatagramPacket(request, request.length, this.internetAddress, this.serverPortNumber);

        System.out.println("Sending request and hoping it gets there...");
        socket.send(requestPacket);

        System.out.println("Starting to wait for data from server.");
        rdtReceive();
        System.out.println("Combining all packets and delivering data:");
        buildFile();
    }

    /**
     * Sits in wait state till it receives a packet.
     * Once a packet is received ACK's back according with the seq number provided in the packet received
     * Discards any packets with wrong seq number
     */
    private void rdtReceive() throws IOException, InterruptedException {

        boolean waiting = true;
        int previousSeq = 1;
        int currentSeq;

        while(waiting) {

            // Create new packet for receiving data
            byte[] data = new byte[packetSize];
            DatagramPacket packet = new DatagramPacket(data, data.length);

            if(slowMode)
                System.out.println("Waiting for packet.");

            // wait to receive packet
            socket.receive(packet);

            // once packet is received get data and put into byte array
            byte[] builtPacket = Arrays.copyOf(packet.getData(), packet.getLength());
            String[] packetInfo = HttpUtil.parseResponseHeader(builtPacket);

            if(!packetInfo[0].equals("OK") && !packetInfo[1].equals("200")) {
                // Did not receive an OK
                break;
            }


            // parse header to get the current sequence number
            currentSeq = HttpUtil.getSeq(builtPacket);


            byte[] ackPacket;

            // Build ACK according to if current seq = previous seq
            // If it is we discard and ACK packet we already have
            // If they aren't the same, we deliver packet and ack back packet SEQ
            // In both conditions an ACK packet is created
            if(currentSeq == previousSeq) {
                if(slowMode)
                    System.out.println("Already got this packet, discarding.");

                ackPacket = HttpUtil.createACK(previousSeq);
            } else {
                ackPacket = HttpUtil.createACK(currentSeq);
                if(slowMode)
                    System.out.println("Received correct packet, saving.");

                previousSeq = currentSeq;
                byte[] packetData = HttpUtil.getData(builtPacket);

                addData(packetData);

                String[][] packetField = HttpUtil.parseFields(packetInfo[2]);

                // If dFlag is set to 1 this is last packet
                if(packetField[1][0].equals("DFLAG") && Integer.parseInt(packetField[1][1]) == 1) {
                    waiting = false;
                }
            }

            // create DatagramPacket from byte[] ACK
            DatagramPacket ack = new DatagramPacket(ackPacket, ackPacket.length, internetAddress, serverPortNumber);

            if(slowMode) {
                System.out.println("Sending ACK back in 5 seconds.");
                Thread.sleep(5000);
            }

            // send the packet back to server
            socket.send(ack);

            if(slowMode) {
                System.out.println("ACK sent.");
            }
        }
    }

    /**
     * Data collecter so we can build the file on finish
     *
     * @param addedData data from packet
     */
    private void addData(byte[] addedData) {
        if(this.data == null) {
            this.data = addedData;
        } else {
            byte[] newData = new byte[this.data.length + addedData.length];

            System.arraycopy(this.data, 0, newData, 0, this.data.length);
            System.arraycopy(addedData, 0, newData, this.data.length, addedData.length);

            this.data = newData;
        }
    }

    /**
     * Print out all data collected from packets
     */
    private void buildFile() {
        System.out.println(new String(this.data));

        this.data = null;
    }
}
