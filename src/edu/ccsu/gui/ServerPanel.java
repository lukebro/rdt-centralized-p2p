package edu.ccsu.gui;

import edu.ccsu.util.HttpUtil;
import edu.ccsu.networking.RDTServer;

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
    private static RDTServer ServerRunnable;

	private JTextArea activity;

	public ServerPanel() throws SocketException, UnknownHostException {

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
		add(northGrid, BorderLayout.NORTH);

		model.addColumn("File");
		model.addColumn("Peer");
		contentScroll = new JScrollPane(table);
		table.setPreferredScrollableViewportSize(new Dimension(500, 70));
		table.setFillsViewportHeight(true);

		centerGrid.setLayout(new BoxLayout(centerGrid, BoxLayout.Y_AXIS));
		centerGrid.add(contentScroll);
		centerGrid.setBorder(BorderFactory.createTitledBorder("Network Content"));
		add(centerGrid, BorderLayout.CENTER);

		activity =  new JTextArea("System Ready\n",5,20);
		activityScroll = new JScrollPane(activity);
		add (activityScroll, BorderLayout.SOUTH);

        ServerRunnable = new RDTServer(this);
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
            ServerRunnable.changeSlowMode(slowMode);

            Server = new Thread(ServerRunnable);

            Server.start();

        } catch(Exception e) {
            e.printStackTrace();
        }
        }
    }

}

