package edu.ccsu.gui;

import edu.ccsu.util.HttpUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.File;
import java.net.*;
import java.util.Arrays;

public class ServerPanel extends JPanel {

	private JPanel northGrid = new JPanel();
	private JRadioButton normal, slow;
	private JButton spawnNetwork = new JButton ("Enable Server In Selected Mode");
	private boolean slowMode = false;
	private JPanel centerGrid = new JPanel();
	private DefaultTableModel model = new DefaultTableModel();
	private JTable table = new JTable(model){public boolean isCellEditable(int row, int col){return false;}};
	private JScrollPane contentScroll, activityScroll;
    private static Thread Server;

	private JTextArea activity;

    private enum Methods {
        REQUEST, // Request list of files
        GET, // Get user with file
        ERROR // Error
    }

	public ServerPanel() {

		setLayout (new BorderLayout());

		normal = new JRadioButton ("Normal", true);
		slow = new JRadioButton ("Slow");

		ButtonGroup mode = new ButtonGroup();
		mode.add(normal);
		mode.add(slow);

		ModeListener modeLstnr = new ModeListener();
		normal.addActionListener(modeLstnr);
		slow.addActionListener(modeLstnr);

		NetworkListener networkListnr = new NetworkListener();
		spawnNetwork.addActionListener(networkListnr);

		northGrid.setLayout(new BoxLayout(northGrid, BoxLayout.X_AXIS));
		northGrid.add(normal);
		northGrid.add(Box.createRigidArea(new Dimension (1,0)));
		northGrid.add(slow);
		northGrid.add(Box.createRigidArea(new Dimension (50,0)));
		northGrid.add(spawnNetwork);
		northGrid.setBorder(BorderFactory.createTitledBorder("Transport Speed"));
		add (northGrid, BorderLayout.NORTH);

		model.addColumn("File");
		model.addColumn("Peer");
		contentScroll = new JScrollPane(table);
		table.setPreferredScrollableViewportSize(new Dimension(500, 70));
		table.setFillsViewportHeight(true);

		centerGrid.setLayout(new BoxLayout(centerGrid, BoxLayout.Y_AXIS));
		centerGrid.add(contentScroll);
		centerGrid.setBorder(BorderFactory.createTitledBorder("Network Content"));
		add (centerGrid, BorderLayout.CENTER);

		activity =  new JTextArea("System Ready\n",5,20);
		activityScroll = new JScrollPane(activity);
		add (activityScroll, BorderLayout.SOUTH);
	}

	public void console(String message){
		activity.append(message + "\n");
	}

	private static byte[] readFromFile(String path) throws IOException {

		File file = new File(path);
		FileInputStream fileStream = new FileInputStream(file);

		byte[] data = new byte[ (int)file.length() ];

		fileStream.read(data,0,data.length);

		return data;
	}

	private class ModeListener implements ActionListener {

		public void actionPerformed (ActionEvent event)
		{
			Object source = event.getSource();

			if (source == normal)
				slowMode = false;
			else
				slowMode = true;
		}
	}
	

	private class NetworkListener implements ActionListener {
        public void actionPerformed (ActionEvent event)
        {
        // Parse argument, and set Slow Mode to true if -slow is passed
            if(slowMode == true) {
                console("Server starting in slow mode...");
            } else {
                console("Server starting...");
            }

        try {
            RDTServer ServerRunnable = new RDTServer(slowMode);
            ServerRunnable.saveData(new String("This is a test case blah blah blah").getBytes());

            Server = new Thread(ServerRunnable);

            Server.start();

        } catch(Exception e) {
            e.printStackTrace();
        }
        }
    }


    public class RDTServer implements Runnable {

        private int sendingPortNumber = 5926;
        private DatagramSocket socket = null;
        private int packetSize = 128;
        private boolean slowMode = false;
        public byte[] savedData;

        /**
         * Set up a server used for RDT
         * assign all private variables to the values passed
         * if slow mode is enabled increase timeout
         *
         * @param slowMode enable/disable slow mode
         */
        public RDTServer(boolean slowMode) throws SocketException, UnknownHostException {
            this.sendingPortNumber = 5926;
            this.slowMode = slowMode;


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
                String[][] packetFields = {{"dFlag", "0"}};

                // Create a header with method POST and seq number
                byte[] packetHeader = HttpUtil.createHeader("POST", String.valueOf(seq), packetFields);

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
                    packetFields[0][1] = "1";
                    packetHeader = HttpUtil.createHeader("POST", String.valueOf(seq), packetFields);
                }


                // pass header + dat and build an HTTP packet
                byte[] builtPacket = HttpUtil.buildPacket(packetHeader, packetData);

                if(slowMode) {
                    console("Server sending packet #" + packetNumber + " of size " + builtPacket.length + " in 5 seconds");
                    Thread.sleep(5000);
                } else {
                    console("Server sending packet #" + packetNumber + " of size " + builtPacket.length);
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
                        console("Server waiting for ACK for packet #" + packetNumber + " with seq #" + seq);

                        // Wait to receive ACK
                        socket.receive(getACK);


                        // put received packet into receivingPacket byte[]
                        byte[] receivingPacket = Arrays.copyOf(getACK.getData(), getACK.getLength());

                        // if the packet is an ACK, get seq number
                        if(HttpUtil.isACK(receivingPacket)) {
                            int getSeq = HttpUtil.getSeq(receivingPacket);

                            // compare sequence numbers to see if correct ACK received
                            if(getSeq != seq) {
                                console("Received ACK with seq #" + getSeq + ", wrong seq number.");
                                continue;
                            } else {
                                console("Received ACK with seq #" + getSeq + ", correct seq number.");
                                seq = (seq == 0)? 1 : 0;
                                packetNumber++;
                                waiting = false;
                                break;
                            }
                        } else {
                            console("Received a packet that is not an ACK. Throwing it out.");
                        }
                    } catch (SocketTimeoutException e) {
                        // Runs when socket times out waiting for an ACK
                        console("Server timed out waiting for ACK for packet #" + packetNumber + ". Sending again.");
                        continue;
                    }
                }

            }

            console("Server done sending.");
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

            console("Server waiting for a request...");

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
            String[] packetInfo = HttpUtil.parseHeader(data);

            Methods method = Methods.valueOf(packetInfo[0].toUpperCase());

            switch(method) {
                case REQUEST:
                    console("Received a REQUEST, sending data to client.");
                    rdtSend(this.savedData, packet.getSocketAddress());
                    break;
                case GET:
                    console("GET not implemented yet.");
                    break;
                case ERROR:
                default:
                    console("Received an error out of context, doing nothing.");

            }

        }

        public void run() {
            while(true) {

                try {

                    this.waitFromBelow();

                } catch (Exception e) {

                    e.printStackTrace();

                }

                console("Request finished sending, back to waiting form below.");
            }
        }

    }




}

