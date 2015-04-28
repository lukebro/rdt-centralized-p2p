package edu.ccsu.gui;

import edu.ccsu.networking.RDT;
import edu.ccsu.structures.Entries;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.File;
import java.net.*;

public class ServerPanel extends JPanel implements ConsolePanel {

	private JPanel northGrid = new JPanel();
	private JRadioButton normal, slow;
	private JButton spawnNetwork = new JButton ("Enable Network");
	private boolean slowMode = false;
	private JPanel centerGrid = new JPanel();
	private DefaultTableModel model = new DefaultTableModel();
	private JTable table = new JTable(model){public boolean isCellEditable(int row, int col){return false;}};
	private JScrollPane contentScroll, activityScroll;
	private RDT serverRunnable;
	private boolean online = false;
	private Thread server;
    private Entries database;

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
		northGrid.setBorder(BorderFactory.createTitledBorder("Server Mode"));
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

		activity =  new JTextArea("Server is ready to start.\nPress \"Enabled Network\" to start server.\n",5,20);
        activity.setEditable(false);
		activityScroll = new JScrollPane(activity);
		add (activityScroll, BorderLayout.SOUTH);

        database = new Entries();
	}

	public void console(String message){

        activity.append(message + "\n");
        activity.selectAll();
	}

    public void processEntries(String[][] entries) {
        model.setNumRows(0);
        for(int i = 0; i < entries.length; i++) {
            model.addRow(new Object[]{entries[i][0], entries[i][2]});
        }
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

			if (online)
				serverRunnable.changeSlowMode(slowMode);
		}
	}
	

	private class NetworkListener implements ActionListener {
		public void actionPerformed (ActionEvent event)
		{
			if (online){
				serverRunnable.running = false;
				serverRunnable.closeSocket();
				server = null;
				online = false;
                activity.setText(null);
				console("Server is shutting down.");
				spawnNetwork.setText("Enable Network");
                model.setNumRows(0);
                database.destroy();
			}
			else {
				// Parse argument, and set Slow Mode to true if -slow is passed
				if(slowMode == true) {
					console("Server starting up in slow mode.");
				} else {
					console("Server starting up.");}

				try {
					serverRunnable = new RDT(2010, ServerPanel.this, database, "server", slowMode);
                    serverRunnable.running = true;
					server = new Thread(serverRunnable);

                    server.start();
					spawnNetwork.setText("Disable Network");
					online = true;

				} catch(Exception e) {console("There was an error starting the network.");}
			}
		}
	}

}

