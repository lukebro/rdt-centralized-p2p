package edu.ccsu.networking;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;

/**
 * Simple receiver thread that starts up and endlessly listens for packets on
 * the specified and delivers them. Recall this simple implementation does not
 * handle loss or corrupted packets.
 *
 * @author Chad Williams
 */
public class RDT10Receiver extends Thread {

    private int port;
    private DatagramSocket receivingSocket = null;

    public RDT10Receiver(String name, int port) {
        super(name);
        this.port = port;
    }

    public void stopListening() {
        if (receivingSocket != null) {
            receivingSocket.close();
        }
    }

    public void deliverData(byte[] data) {
        System.out.println("@@@ Receiver delivered packet with: '" + new String(data) + "'");
    }

    /**
     * Start the thread to begin listening
     */
    public void run() {
        try {
            receivingSocket = new DatagramSocket(49000);
            while (true) {
                System.out.println("@@@ Receiver waiting for packet");
                byte[] buf = new byte[16];
                // receive request
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                receivingSocket.receive(packet);
                byte[] packetData = Arrays.copyOf(packet.getData(), packet.getLength());
                deliverData(packetData);
            }
        } catch (Exception e) {
            stopListening();
        }
    }
}
