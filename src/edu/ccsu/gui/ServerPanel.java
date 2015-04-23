package edu.ccsu.gui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.File;
import edu.ccsu.networking.RDTServer;


public class ServerPanel extends JPanel {

	private JPanel northGrid = new JPanel();
	private JRadioButton normal, slow;
	private JButton spawnNetwork = new JButton ("Enable Server In Selected Mode");
	private boolean slowMode = false;
	private JPanel centerGrid = new JPanel();
	private DefaultTableModel model = new DefaultTableModel();
	private JTable table = new JTable(model){public boolean isCellEditable(int row, int col){return false;}};
	private JScrollPane contentScroll, activityScroll;

	private JTextArea activity;

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

		activity =  new JTextArea("System Ready",5,20);
		activityScroll = new JScrollPane(activity);
		add (activityScroll, BorderLayout.SOUTH);
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
				activity.append("\nServer starting in slow mode...");
			} else {
				activity.append("\nServer starting...");
			}


					try {
						RDTServer server = new RDTServer(slowMode);

						activity.append("\nReading data from data.txt.");
						byte[] data = readFromFile("data.txt");

						// Save data we read from data.txt into our object
						server.saveData(data);

						// Start server in infinite loop
						while(true) {

							server.waitFromBelow();

							activity.append("\nRequest finished sending, back to waiting form below.");
						}
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			}
	}
}

