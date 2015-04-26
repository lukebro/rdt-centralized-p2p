package edu.ccsu.networking;

import edu.ccsu.structures.Entries;
import edu.ccsu.util.HttpUtil;
import edu.ccsu.gui.ConsolePanel;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.*;
import java.util.Arrays;

public class RDT implements Runnable {

    private int ourPort = 2010;
    private DatagramSocket socket = null;
    private boolean slowMode = false;
    private int packetSize = 128;
    private byte[] data = null;
    private ConsolePanel panel;
    private Entries database;
    private String mode;
    public boolean running = true;
    public InetSocketAddress server;

    /**
     * Methods of our HTTP protocol
     */
    private enum RequestMethods {
        REQUEST, // Request a file
        POST // Update file list
    }

    /**
     * RDT For Server
     * @param sendingPort
     * @param panel
     * @throws SocketException
     * @throws UnknownHostException
     */
    public RDT(int sendingPort, ConsolePanel panel, Entries database, String mode) throws SocketException, UnknownHostException {
        this.slowMode = false;
        this.ourPort = sendingPort;
        this.panel = panel;
        this.database = database;
        this.mode = mode;

        socket = new DatagramSocket(this.ourPort);
    }

    public RDT(int sendingPort, ConsolePanel panel) throws SocketException, UnknownHostException {
        this.slowMode = false;
        this.ourPort = sendingPort;
        this.panel = panel;
        this.database = null;

        socket = new DatagramSocket(this.ourPort);
    }


    public void changeSlowMode(boolean mode) {
        this.slowMode = mode;
    }

    private void closeSocket(){
        if (socket!=null){
            socket.close();
        }
    }

    private void rdtReceive() throws IOException, InterruptedException {

        socket.setSoTimeout(0);
        boolean waiting = true;
        int previousSeq = 1;
        int currentSeq;

        while (waiting) {

            // Create new packet for receiving data
            byte[] data = new byte[packetSize];
            DatagramPacket packet = new DatagramPacket(data, data.length);

            if (slowMode)
                panel.console("@@@ Waiting for packet.");

            // wait to receive packet
            socket.receive(packet);

            SocketAddress receiver = packet.getSocketAddress();

            // once packet is received get data and put into byte array
            byte[] builtPacket = Arrays.copyOf(packet.getData(), packet.getLength());
            String[] packetInfo = HttpUtil.parseResponseHeader(builtPacket);


            // parse header to get the current sequence number
            currentSeq = HttpUtil.getSeq(builtPacket);


            byte[] ackPacket;

            // Build ACK according to if current seq = previous seq
            // If it is we discard and ACK packet we already have
            // If they aren't the same, we deliver packet and ack back packet SEQ
            // In both conditions an ACK packet is created
            if (currentSeq == previousSeq) {
                if (slowMode)
                    panel.console("@@@ Already got this packet, discarding.");

                ackPacket = HttpUtil.createACK(previousSeq);
            } else {
                ackPacket = HttpUtil.createACK(currentSeq);
                if (slowMode)
                    panel.console("@@@ Received correct packet, saving.");

                previousSeq = currentSeq;
                byte[] packetData = HttpUtil.getData(builtPacket);

                this.addData(packetData);

                String[][] packetField = HttpUtil.parseFields(packetInfo[2]);

                // If dFlag is set to 1 this is last packet
                if (packetField[1][0].equals("DFLAG") && Integer.parseInt(packetField[1][1]) == 1) {
                    waiting = false;
                }
            }

            // create DatagramPacket from byte[] ACK
            DatagramPacket ack = new DatagramPacket(ackPacket, ackPacket.length, receiver);

            if (slowMode) {
                panel.console("@@@ Sending ACK back in 5 seconds.");
                Thread.sleep(5000);
            }

            // send the packet back to server
            socket.send(ack);

            if (slowMode) {
                panel.console("@@@ ACK sent.");
            }
        }

    }

    /**
     * splits saved data into smaller packets
     * builds HTTP header then sends each packet
     * advances only when receiving correct ACK
     *
     * @param data data that we want to send
     * @param receiver SocketAddress of the person requesting data
     */

    public void rdtSend(byte[] data, InetSocketAddress receiver, String type) throws IOException, InterruptedException {

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
            byte[] packetHeader;

            if(type.equals("POST"))
                packetHeader = HttpUtil.createRequestHeader("POST", "updList", packetFields);
            else
                packetHeader = HttpUtil.createResponseHeader("OK", "200", packetFields);

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

                if(type.equals("request"))
                    packetHeader = HttpUtil.createRequestHeader("REQUEST", "updList", packetFields);
                else
                    packetHeader = HttpUtil.createResponseHeader("OK", "200", packetFields);
            }


            // pass header + dat and build an HTTP packet
            byte[] builtPacket = HttpUtil.buildPacket(packetHeader, packetData);

            if(slowMode) {
                panel.console("# Sending packet #" + packetNumber + " of size " + builtPacket.length + " in 5 seconds");
                Thread.sleep(5000);
            } else {
                panel.console("# Sending packet #" + packetNumber + " of size " + builtPacket.length);
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
                    panel.console("# Waiting for ACK for packet #" + packetNumber + " with seq #" + seq);

                    // Wait to receive ACK
                    socket.receive(getACK);


                    // put received packet into receivingPacket byte[]
                    byte[] receivingPacket = Arrays.copyOf(getACK.getData(), getACK.getLength());

                    // if the packet is an ACK, get seq number
                    if(HttpUtil.isACK(receivingPacket)) {
                        int getSeq = HttpUtil.getSeq(receivingPacket);

                        // compare sequence numbers to see if correct ACK received
                        if(getSeq != seq) {
                            panel.console("# Received ACK with seq #" + getSeq + ", wrong seq number.");
                            continue;
                        } else {
                            panel.console("# Received ACK with seq #" + getSeq + ", correct seq number.");
                            seq = (seq == 0)? 1 : 0;
                            packetNumber++;
                            waiting = false;
                            break;
                        }
                    } else {
                        System.out.println("# Received a packet that is not an ACK. Throwing it out.");
                    }
                } catch (SocketTimeoutException e) {
                    // Runs when socket times out waiting for an ACK
                    panel.console("# Timed out waiting for ACK for packet #" + packetNumber + ". Sending again.");
                    continue;
                }
            }

        }

        panel.console("# Done sending.");
    }


    /**
     * Client attempt to join a network
     * @throws IOException
     * @throws InterruptedException
     */
    public void rdtPost() throws IOException, InterruptedException {

        // Create a request for the file, assume it's going to get there
        byte[] request = HttpUtil.createRequestHeader("POST", "join");

        String[][] ourEntries = database.getEntries();

        String entries = "";

        for(int i = 0; i < ourEntries.length; i++) {
            entries += ourEntries[i][0] + ": " + ourEntries[i][1] + "\r\n";
        }

        byte[] data =  entries.getBytes();

        panel.console("# Sending data " + new String(data));

        DatagramPacket requestPacket = new DatagramPacket(request, request.length, this.server);

        panel.console("# Sending request to join network.");
        socket.send(requestPacket);

        panel.console("# Request sent, sending list to server now.");
        Thread.sleep(2000);
        rdtSend(data, server, "POST");

        panel.console("# Waiting to receive list from server");

        rdtReceive();

        String allEntries1 = this.dumpData();

        String[][] allEntries2 = HttpUtil.parseFields(allEntries1);

        panel.console("# Updating my list.");

        panel.processEntries(allEntries2);

        panel.console("# Entries updated.");
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

    private String dumpData() {
        String data = new String(this.data);
        this.data = null;

        return data;
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

        panel.console("@ Server waiting for a request...");

        socket.receive(call);

        if(slowMode)
            socket.setSoTimeout(10000);
        else
            socket.setSoTimeout(20);
        processRequest(call);
    }

    /**
     * Request song and peer holder
     * @param song
     * @throws IOException
     * @throws InterruptedException
     */
    public String rdtRequest(String song, InetSocketAddress receiver) throws IOException, InterruptedException {

        // Create a request for the file, assume it's going to get there
        byte[] request = HttpUtil.createRequestHeader("REQUEST", song);

        DatagramPacket requestPacket = new DatagramPacket(request, request.length, receiver);

        panel.console("Requesting peer who has '" + song + "'.");
        socket.send(requestPacket);


        byte[] data = new byte[packetSize];
        DatagramPacket packet = new DatagramPacket(data, data.length);

        socket.receive(packet);

        byte[] builtPacket = Arrays.copyOf(packet.getData(), packet.getLength());

        String[] packetInfo = HttpUtil.parseResponseHeader(builtPacket);

        String name = HttpUtil.parseFields(packetInfo[2])[0][1];

        closeSocket();

        return name;
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

        InetSocketAddress peer = new InetSocketAddress(packet.getAddress(), packet.getPort());

        switch(method) {
            case REQUEST:
                String ip = database.getPeer(packetInfo[1]);
                
                String[][] fields = {{"PEER", ip}};

                byte[] header = HttpUtil.createResponseHeader("OK", "200", fields);
                packet.getAddress().getHostAddress();

                DatagramPacket responsePacket = new DatagramPacket(header, header.length, peer);

                socket.send(responsePacket);
                panel.console("# Sent IP to peer.");
                break;
            case POST:
                if(packetInfo[1].equals("join")) {
                    panel.console("@ Peer requesting to join networking, waiting to receive their list.");


                    rdtReceive();

                    String receivedData = this.dumpData();
                    String[][] entries = HttpUtil.parseFields(receivedData);

                    database.deletePeer(packet.getAddress().getHostAddress());
                    database.addEntries(entries, packet.getAddress().getHostAddress());

                    panel.console("@ Peer entries added to database.");

                    String[][] ourEntries = database.getEntries();

                    String allEntries = "";

                    for(int i = 0; i < ourEntries.length; i++) {
                        allEntries += ourEntries[i][0] + ": " + ourEntries[i][1] + "\r\n";
                    }


                    rdtSend(allEntries.getBytes(), peer, "OK");

                    panel.console("# Sent all entries to peer.");
                }
                break;
            default:
                panel.console("@ Received an error out of context, doing nothing.");

        }

        database.printAllEntries();

    }

    public void run() {
        if(this.mode.equals("client")) {
            // Client RDT Send and connect;
            // make sure client.server = InetSocketAddress
            try {
                this.rdtPost();
            } catch (Exception e) { e.printStackTrace();}

        } else if(this.mode.equals("server")){

            while (running) {
                this.changeSlowMode(true);
                try {
                    this.waitFromBelow();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    public void getDatabase() {
        database.printAllEntries();
    }

}

