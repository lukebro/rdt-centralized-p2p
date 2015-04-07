package edu.ccsu.networking;

import edu.ccsu.util.HttpUtil;
import java.io.IOException;
import java.net.*;
import java.util.Arrays;


public class RDTClient {
    private int receiverPortNumber = 0;
    private int sendingPortNumber = 0;
    private DatagramSocket socket = null;
    private InetAddress internetAddress = null;
    private boolean slowMode = false;
    private int packetSize = 512;
    private byte[] data;

    /*
     * Set up a client used for RDT
     * assign all private variables to the values passed
     */
    public RDTClient(byte[] targetAddress, int receiverPortNumber, int sendingPortNumber, boolean slowMode) throws SocketException, UnknownHostException {
        internetAddress = InetAddress.getByAddress(targetAddress);
        this.receiverPortNumber = receiverPortNumber;
        this.sendingPortNumber = sendingPortNumber;
        this.slowMode = slowMode;

        socket = new DatagramSocket(this.sendingPortNumber);
    }

    /*
     * Closes socket
     */
    public void closeSocket(){
        if (socket!=null){
            socket.close();
        }
    }

    /*
     * Sits in wait state till it receives a packet.
     * Once a packet is received ACK's back according with the seq number provided in the packet received
     * Discards any packets with wrong seq number
     */
    public void rdtReceive() throws IOException, InterruptedException {

        int previousSeq = 1;
        int currentSeq = 0;

        while(true) {

            // Create new packet for receiving data
            byte[] data = new byte[packetSize];
            DatagramPacket packet = new DatagramPacket(data, data.length);

            if(slowMode)
                System.out.println("Waiting for packet...");

            // wait to receive packet
            socket.receive(packet);

            // once packet is received get data and put into byte array
            byte[] builtPacket = Arrays.copyOf(packet.getData(), packet.getLength());

            // parse header to get the current sequence number
            String[] packetInfo = HttpUtil.parseHeader(builtPacket);
            currentSeq = Integer.parseInt(packetInfo[1]);


            byte[] ackPacket;

            // Build ACK according to if current seq = previous seq
            // If it is we discard and ACK packet we already have
            // If they aren't the same, we deliver packet and ack back packet SEQ
            // In both conditions an ACK packet is created
            if(currentSeq == previousSeq) {
                if(slowMode)
                    System.out.println("Already got this packet, discarding...");

                ackPacket = HttpUtil.createACK(previousSeq);
            } else {
                ackPacket = HttpUtil.createACK(currentSeq);
                if(slowMode)
                    System.out.println("Received correct packet, delivering...");

                previousSeq = currentSeq;
                byte[] packetData = HttpUtil.getData(builtPacket);
                deliverData(packetData);
            }

            // create DatagramPacket from byte[] ACK
            DatagramPacket ack = new DatagramPacket(ackPacket, ackPacket.length, internetAddress, receiverPortNumber);

            if(slowMode) {
                System.out.println("Sending ACK back in 5 seconds");
                Thread.sleep(5000);
            }

            // send the packet back to server
            socket.send(ack);

            if(slowMode) {
                System.out.println("ACK sent.");
            }
        }
    }

    /*
     * Print out packet data
     * @param byte[] packet
     */
    private void deliverData(byte[] packet) {
        System.out.println(new String(packet));
    }
}
