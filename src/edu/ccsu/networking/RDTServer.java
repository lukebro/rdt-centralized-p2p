package edu.ccsu.networking;

import edu.ccsu.gui.ServerPanel;
import edu.ccsu.util.HttpUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.*;
import java.util.Arrays;


public class RDTServer implements Runnable {

    private int sendingPortNumber = 2010;
    private DatagramSocket socket = null;
    private int packetSize = 128;
    private boolean slowMode = false;
    private byte[] savedData;
    private static ServerPanel panel;

    /**
     * Methods of our HTTP protocol
     */
    private enum RequestMethods {
        REQUEST, // Request a file
        POST // Update file list
    }

    /**
     * Set up a server used for RDT
     * assign all private variables to the values passed
     * if slow mode is enabled increase timeout
     *
     */
    public RDTServer(ServerPanel panel) throws SocketException, UnknownHostException {
        this.slowMode = false;

        this.panel = panel;
        socket = new DatagramSocket(this.sendingPortNumber);
    }

    /**
     * Close open socket
     */
    public void closeSocket(){
        if (socket!=null){
            socket.close();
        }
    }

    public void changeSlowMode(boolean change) {
        this.slowMode = change;
    }

    public void saveData(byte[] loadedData) {
        this.savedData = loadedData;
    }

    /**
     * splits saved data into smaller packets
     * builds HTTP header then sends each packet
     * advances only when receiving correct ACK
     *
     * @param data data that we want to send
     * @param receiver SocketAddress of the person requesting data
     */
    public void rdtSend(byte[] data, SocketAddress receiver) throws IOException, InterruptedException {

        // Create a InputStream to easily split up byte[] into smaller packets
        ByteArrayInputStream byteStream = new ByteArrayInputStream(data);

        int packetNumber = 0;
        int seq = 0;
        boolean waiting;

        // while more data is available in the stream, or we want to send a packet saying we're done sending
        while(byteStream.available() > 0) {

            waiting = true;

            // Create field for done flag
            String[][] packetFields = {{"SEQ_NO", String.valueOf(seq)}, {"DFLAG", "0"}};

            // Create a header with method POST and seq number
            byte[] packetHeader = HttpUtil.createResponseHeader("OK", "202", packetFields);

            // Create byte[] where the size = packetSize - packerHeader size
            byte[] packetData = new byte[packetSize - packetHeader.length];

            // read bytes to fill the byte[] array
            int bytesRead = byteStream.read(packetData);

            // if we ready less bytes then size of byte[] then make the packetData byte[] smaller to fit data
            if (bytesRead < (packetData.length)) {
                packetData = Arrays.copyOf(packetData, bytesRead);
            }

            // Bytes available after we read
            int bytesAvailable = byteStream.available();

            // If this is the last packet
            if(bytesAvailable == 0) {
                // Last packet so put dFlag as 1 and rebuilt header will be same size as if with 0, changing 1 byte
                packetFields[1][1] = "1";
                packetHeader = HttpUtil.createResponseHeader("OK", "202", packetFields);
            }


            // pass header + dat and build an HTTP packet
            byte[] builtPacket = HttpUtil.buildPacket(packetHeader, packetData);

            if(slowMode) {
                panel.console("Server sending packet #" + packetNumber + " of size " + builtPacket.length + " in 5 seconds");
                Thread.sleep(5000);
            } else {
                panel.console("Server sending packet #" + packetNumber + " of size " + builtPacket.length);
            }

            // Create a DatagramPacket with data builtPacket
            DatagramPacket packet = new DatagramPacket(builtPacket, builtPacket.length, receiver);

            // While we are waiting for ACK
            while(waiting) {

                // Send packet to client
                try {
                    socket.send(packet);
                } catch (IOException e) {}

                // Create DatagramPacket to receive ACK
                byte[] ack = new byte[packetSize];
                DatagramPacket getACK = new DatagramPacket(ack, ack.length);

                try {
                    panel.console("Server waiting for ACK for packet #" + packetNumber + " with seq #" + seq);

                    // Wait to receive ACK
                    socket.receive(getACK);


                    // put received packet into receivingPacket byte[]
                    byte[] receivingPacket = Arrays.copyOf(getACK.getData(), getACK.getLength());

                    // if the packet is an ACK, get seq number
                    if(HttpUtil.isACK(receivingPacket)) {
                        int getSeq = HttpUtil.getSeq(receivingPacket);

                        // compare sequence numbers to see if correct ACK received
                        if(getSeq != seq) {
                            panel.console("Received ACK with seq #" + getSeq + ", wrong seq number.");
                            continue;
                        } else {
                            panel.console("Received ACK with seq #" + getSeq + ", correct seq number.");
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
                    panel.console("Server timed out waiting for ACK for packet #" + packetNumber + ". Sending again.");
                    continue;
                }
            }

        }

        System.out.println("Server done sending.");
    }


    /**
     * Waits for a request from below (client) then starts the correct process
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public void waitFromBelow() throws IOException, InterruptedException {
        byte[] callData = new byte[packetSize];
        DatagramPacket call = new DatagramPacket(callData, callData.length);

        socket.setSoTimeout(0);

        panel.console("Server waiting for a request...");

        socket.receive(call);

        if(slowMode)
            socket.setSoTimeout(10000);
        else
            socket.setSoTimeout(20);
        processRequest(call);
    }

    /**
     * Processes an incoming request by looking at the method inside packet
     *
     * @param packet incoming packet we want to process
     * @throws IOException
     * @throws InterruptedException
     */
    private void processRequest(DatagramPacket packet) throws IOException, InterruptedException {

        byte[] data = packet.getData();
        String[] packetInfo = HttpUtil.parseRequestHeader(data);

        RequestMethods method = RequestMethods.valueOf(packetInfo[0].toUpperCase());

        switch(method) {
            case REQUEST:
                // Handle Request
                panel.console("Received a REQUEST, sending response to client.");
                rdtSend(this.savedData, packet.getSocketAddress());
                break;
            case POST:
                // Handle Post
                panel.console("POST not implemented yet.");
                break;
            default:
                panel.console("Received an error out of context, doing nothing.");

        }

    }

    public void run() {
        while(true) {

            try {

                this.waitFromBelow();

            } catch (Exception e) {

                e.printStackTrace();

            }

            panel.console("Request finished sending, back to waiting form below.");
        }
    }

}

