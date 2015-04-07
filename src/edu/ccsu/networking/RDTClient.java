package edu.ccsu.networking;

import edu.ccsu.util.HTTP;
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

    public RDTClient(byte[] targetAddress, int receiverPortNumber, int sendingPortNumber, boolean slowMode) throws SocketException, UnknownHostException {
        internetAddress = InetAddress.getByAddress(targetAddress);
        this.receiverPortNumber = receiverPortNumber;
        this.sendingPortNumber = sendingPortNumber;
        this.slowMode = slowMode;

        socket = new DatagramSocket(this.sendingPortNumber);
    }

    public void closeSocket(){
        if (socket!=null){
            socket.close();
        }
    }

    public void rdtReceive() throws IOException, InterruptedException {

        int previousSeq = 1;
        int currentSeq = 0;

        while(true) {

            byte[] data = new byte[packetSize];
            DatagramPacket packet = new DatagramPacket(data, data.length);

            if(slowMode)
                System.out.println("Waiting for packet...");

            socket.receive(packet);

            byte[] builtPacket = Arrays.copyOf(packet.getData(), packet.getLength());

            String[] packetInfo = HTTP.parseHeader(builtPacket);
            currentSeq = Integer.parseInt(packetInfo[1]);


            byte[] ackPacket;

            if(currentSeq == previousSeq) {
                if(slowMode)
                    System.out.println("Already got this packet, discarding...");

                ackPacket = HTTP.createACK(previousSeq);
            } else {
                ackPacket = HTTP.createACK(currentSeq);
                if(slowMode)
                    System.out.println("Received correct packet, delivering...");

                previousSeq = currentSeq;
                byte[] packetData = HTTP.getData(builtPacket);
                deliverData(packetData);
            }

            DatagramPacket ack = new DatagramPacket(ackPacket, ackPacket.length, internetAddress, receiverPortNumber);

            if(slowMode) {
                System.out.println("Sending ACK back in 5 seconds");
                Thread.sleep(5000);
            }

            socket.send(ack);

            if(slowMode) {
                System.out.println("ACK sent.");
            }
        }
    }

    private void deliverData(byte[] packet) {
        System.out.println(new String(packet));
    }
}
