package edu.ccsu.networking;

import edu.ccsu.util.HttpUtil;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.*;
import java.util.Arrays;


public class RDTServer {
    private int receiverPortNumber = 0;
    private int sendingPortNumber = 0;
    private DatagramSocket socket = null;
    private InetAddress internetAddress = null;
    private int packetSize = 512;
    private boolean slowMode = false;

    public RDTServer(byte[] targetAddress, int receiverPortNumber, int sendingPortNumber, boolean slowMode) throws SocketException, UnknownHostException {
        internetAddress = InetAddress.getByAddress(targetAddress);
        this.receiverPortNumber = receiverPortNumber;
        this.sendingPortNumber = sendingPortNumber;
        this.slowMode = slowMode;

        socket = new DatagramSocket(this.sendingPortNumber);
        if(slowMode)
            socket.setSoTimeout(10000);
        else
            socket.setSoTimeout(20);
    }
    
    public void closeSocket(){
        if (socket!=null){
            socket.close();
        }
    }

    public void rdtSend(byte[] data) throws IOException, InterruptedException {

        ByteArrayInputStream byteStream = new ByteArrayInputStream(data);
        int packetNumber = 0;
        int seq = 0;
        boolean waiting;

        while(byteStream.available() > 0) {

            waiting = true;
            byte[] packetHeader = HttpUtil.createHeader("POST", String.valueOf(seq));
            byte[] packetData = new byte[packetSize - packetHeader.length];

            int bytesRead = byteStream.read(packetData);

            if (bytesRead < (packetData.length)) {
                packetData = Arrays.copyOf(packetData, bytesRead);
            }

            byte[] builtPacket = HttpUtil.buildPacket(packetHeader, packetData);

            if(slowMode) {
                System.out.println("Server sending packet #" + packetNumber + " of size " + builtPacket.length + " in 5 seconds");
                Thread.sleep(5000);
            } else {
                System.out.println("Server sending packet #" + packetNumber + " of size " + builtPacket.length);
            }

            DatagramPacket packet = new DatagramPacket(builtPacket, builtPacket.length, internetAddress, receiverPortNumber);

            while(waiting) {

                socket.send(packet);

                byte[] ack = new byte[packetSize];
                DatagramPacket getACK = new DatagramPacket(ack, ack.length);

                try {
                    System.out.println("Server waiting for ACK for packet #" + packetNumber + " with seq #"+seq);

                    socket.receive(getACK);

                    byte[] receivingPacket = Arrays.copyOf(getACK.getData(), getACK.getLength());

                    if(HttpUtil.isACK(receivingPacket)) {
                        int getSeq = HttpUtil.getSeq(receivingPacket);

                        if(getSeq != seq) {
                            System.out.println("Received ACK with seq #" + getSeq + ", wrong seq number.");
                            continue;
                        } else {
                            System.out.println("Received ACK with seq #" + getSeq + ", correct seq number.");
                            seq = (seq == 0)? 1 : 0;
                            packetNumber++;
                            waiting = false;
                            break;
                        }
                    } else {
                        System.out.println("Received a packet that is not an ACK. Throwing it out.");
                    }
                } catch (SocketTimeoutException e) {
                    System.out.println("Server timed out waiting for ACK for packet #" + packetNumber + ". Sending again.");
                    continue;
                }
            }

        }
        System.out.println("Server done sending.");

        closeSocket();
    }

}
