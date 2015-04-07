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

    /*
    * Set up a server used for RDT
    * assign all private variables to the values passed
    * if slow mode is enabled increase timeout
    */
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

    /*
     * Close open socket
     */
    public void closeSocket(){
        if (socket!=null){
            socket.close();
        }
    }

    /*
     * splits byte[] into smaller packets
     * builds HTTP header then sends each packet
     * advances only when receiving correct ACK
     * @param byte[] data to send
     */
    public void rdtSend(byte[] data) throws IOException, InterruptedException {

        // Create a InputStream to easily split up byte[] into smaller packets
        ByteArrayInputStream byteStream = new ByteArrayInputStream(data);
        int packetNumber = 0;
        int seq = 0;
        boolean waiting;

        // while more data is available in the stream
        while(byteStream.available() > 0) {

            waiting = true;
            // Create a header with method POST and seq number
            byte[] packetHeader = HttpUtil.createHeader("POST", String.valueOf(seq));
            // Create byte[] where the size = packetSize - packerHeader size
            byte[] packetData = new byte[packetSize - packetHeader.length];

            // read bytes to fill the byte[] array
            int bytesRead = byteStream.read(packetData);

            // if we ready less bytes then size of byte[] then make the packetData byte[] smaller to fit data
            if (bytesRead < (packetData.length)) {
                packetData = Arrays.copyOf(packetData, bytesRead);
            }

            // pass header + dat and build an HTTP packet
            byte[] builtPacket = HttpUtil.buildPacket(packetHeader, packetData);

            if(slowMode) {
                System.out.println("Server sending packet #" + packetNumber + " of size " + builtPacket.length + " in 5 seconds");
                Thread.sleep(5000);
            } else {
                System.out.println("Server sending packet #" + packetNumber + " of size " + builtPacket.length);
            }

            // Create a DatagramPacket with data builtPacket
            DatagramPacket packet = new DatagramPacket(builtPacket, builtPacket.length, internetAddress, receiverPortNumber);

            // While we are waiting for ACK
            while(waiting) {

                // Send packet to client
                socket.send(packet);

                // Create DatagramPacket to receive ACK
                byte[] ack = new byte[packetSize];
                DatagramPacket getACK = new DatagramPacket(ack, ack.length);

                try {
                    System.out.println("Server waiting for ACK for packet #" + packetNumber + " with seq #"+seq);

                    // Wait to receive ACK
                    socket.receive(getACK);

                    // put received packet into receivingPacket byte[]
                    byte[] receivingPacket = Arrays.copyOf(getACK.getData(), getACK.getLength());

                    // if the packet is an ACK, get seq number
                    if(HttpUtil.isACK(receivingPacket)) {
                        int getSeq = HttpUtil.getSeq(receivingPacket);

                        // compare sequence numbers to see if correct ACK received
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
                    // Runs when socket times out waiting for an ACK
                    System.out.println("Server timed out waiting for ACK for packet #" + packetNumber + ". Sending again.");
                    continue;
                }
            }

        }
        System.out.println("Server done sending.");

        // close the socket
        closeSocket();
    }

}
