package edu.ccsu.gui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.net.*;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.ccsu.networking.RDT;
import edu.ccsu.networking.Peer;

public class PeerPanel extends JPanel implements ConsolePanel {

	private JPanel northGrid, connectionGrid, networkGrid,remoteBorder,remoteSouth, localBox, centerGrid;
	
	private JRadioButton normal, slow;
	private boolean slowMode = false;
    private String shareFolder;
	
	private JLabel serverLabel = new JLabel("Directory IP:");
	public JTextField enterServerIP = new JTextField();
	Pattern p = Pattern.compile("[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}");
	
	private JButton chooseShareFolder, networkJoinLeave, downloadFiles, queryServer;
	
	private DefaultTableModel remoteModel = new DefaultTableModel();
	private DefaultTableModel localModel = new DefaultTableModel();
	private JTable remoteTable = new JTable(remoteModel){ public boolean isCellEditable(int row, int col) { return false; } };
	private JTable localTable = new JTable(localModel){ public boolean isCellEditable(int row, int col) { return false; } };
	private JTextArea activity;
	private JScrollPane remoteScroll, localScroll, activityScroll;
	
	final JFileChooser fileChooser = new JFileChooser();
	private File[] myFiles;
	private Peer peer;
	private Thread pt;
	private RDT client;
	
	private boolean online = false;

	public PeerPanel() {
	
		peer = new Peer(this);
		pt = new Thread(peer);

		setLayout(new BorderLayout());

		chooseShareFolder = new JButton("Choose Shared Directory");
		FileListener fileListnr = new FileListener();
		chooseShareFolder.addActionListener(fileListnr);
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		
		networkJoinLeave = new JButton("Join Network");
		PeerListener peerListnr = new PeerListener();
		networkJoinLeave.addActionListener(peerListnr);
		
		normal = new JRadioButton("Normal Mode", true);
		slow = new JRadioButton("Slow Mode");
		ButtonGroup mode = new ButtonGroup();
		mode.add(normal);
		mode.add(slow);
		ModeListener modeLstnr = new ModeListener();
		normal.addActionListener(modeLstnr);
		slow.addActionListener(modeLstnr);
		
		networkGrid = new JPanel();
		networkGrid.setLayout(new GridLayout(1, 4));
		networkGrid.setBorder(BorderFactory.createTitledBorder("Network Settings"));
		networkGrid.add(normal);
		networkGrid.add(slow);
		networkGrid.add(serverLabel);
		networkGrid.add(enterServerIP);
		
		connectionGrid = new JPanel();
		connectionGrid.setLayout(new GridLayout(2, 1));
		connectionGrid.add(networkGrid);
		connectionGrid.add(networkJoinLeave);
		
		northGrid = new JPanel();
		northGrid.setLayout(new GridLayout(1,2));
		northGrid.setBorder(BorderFactory.createTitledBorder("Application Setup"));
		northGrid.add(connectionGrid);
		northGrid.add(chooseShareFolder);

		add(northGrid, BorderLayout.NORTH);

		remoteModel.addColumn("Name");
		remoteModel.addColumn("Size");
		remoteScroll = new JScrollPane(remoteTable);
		remoteTable.setPreferredScrollableViewportSize(new Dimension(250, 70));
		remoteTable.setFillsViewportHeight(true);
		remoteTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		remoteBorder = new JPanel();
		remoteBorder.setLayout(new BorderLayout());
		remoteBorder.add(remoteScroll, BorderLayout.CENTER);
		remoteBorder.setBorder(BorderFactory.createTitledBorder("Available for Download"));
		downloadFiles = new JButton("Get Selected");
		SelectionListener selectListnr = new SelectionListener();
		downloadFiles.addActionListener(selectListnr);
		queryServer = new JButton("Sync with Directory");
		SyncListener syncListnr = new SyncListener();
		queryServer.addActionListener(syncListnr);
		remoteSouth = new JPanel();
		remoteSouth.setLayout(new GridLayout(1,2));
		remoteSouth.add(downloadFiles);
		remoteSouth.add(queryServer);
		remoteBorder.add(remoteSouth, BorderLayout.SOUTH);

		localModel.addColumn("Name");
		localModel.addColumn("Size");
		localScroll = new JScrollPane(localTable);
		localTable.setPreferredScrollableViewportSize(new Dimension(50, 70));
		localTable.setFillsViewportHeight(true);
		localBox = new JPanel();
		localBox.setLayout(new BoxLayout(localBox, BoxLayout.Y_AXIS));
		localBox.setBorder(BorderFactory.createTitledBorder("Local Files"));
		localBox.add(localScroll);

		centerGrid = new JPanel();
		centerGrid.setLayout(new GridLayout(1,2));
		centerGrid.add(remoteBorder);
		centerGrid.add(localBox);

		add(centerGrid, BorderLayout.CENTER);

		activity = new JTextArea("System Ready...\n",5,20);
		activity.setEditable(false);
		activityScroll = new JScrollPane(activity);

		add(activityScroll, BorderLayout.SOUTH);
		
	}
	
	public void console(String message){
		activity.append(message + "\n");
		activity.selectAll();
	}
	
	public void listMyFiles(){
		peer.removeFiles();
		int count = localModel.getRowCount();
		if (count > 0){
			for (int i = count-1; i >= 0; i--){
				localModel.removeRow(i);
			}
		}
		File folder = fileChooser.getSelectedFile();
		peer.setFolder(folder.getAbsolutePath());
        shareFolder = folder.getAbsolutePath();
		console("\"" + folder.getAbsolutePath() + "\" set as Shared Directory.");
		myFiles = folder.listFiles();
		for(File file : myFiles){
			localModel.addRow(new Object[]{file.getName(),file.length()});
			try {
				peer.addFile(file.getName(),file.length());
			} catch (UnknownHostException e1) {e1.printStackTrace();}
		}
	}

    public void processEntries(String[][] entries) {
    	int count = remoteModel.getRowCount();
		if (count > 0){
			for (int i = count-1; i >= 0; i--){
				remoteModel.removeRow(i);
			}
		}
    	
    	for(int i = 0; i < entries.length; i++) {
    		remoteModel.addRow(new Object[]{entries[i][0],entries[i][1]});
    		}
    }


	private class ModeListener implements ActionListener {

		public void actionPerformed (ActionEvent event)
		{
			Object source = event.getSource();

			if (source == normal) {
                slowMode = false;
            } else {
                slowMode = true;
            }

			if (online) {
                client.changeSlowMode(slowMode);
            }
		}
	}
	
	private class FileListener implements ActionListener {

		public void actionPerformed(ActionEvent e) {

			int returnVal = fileChooser.showOpenDialog(new JFrame());

			if (returnVal == JFileChooser.APPROVE_OPTION)
				listMyFiles();
		}

	}

	private class PeerListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {

			Matcher m = p.matcher(enterServerIP.getText());
			boolean validIP = m.matches();

			if(!peer.folderSet()) {

                console("Please select a folder of files first.");

            } else if (!validIP){

                console("Invalid server address.");

            } else {
				if (online){


					peer.leaveNetwork();

					InetSocketAddress targetAddress = new InetSocketAddress(enterServerIP.getText(), 2010);

                    try {
                        client = new RDT(3010, PeerPanel.this, peer.getList(), "disconnect", slowMode);
                    } catch (Exception e1) {}

                    client.server = targetAddress;
					Thread peerThread = new Thread(client);
					peerThread.start();

					online = false;
					networkJoinLeave.setText("DISCONNECTED");
                    networkJoinLeave.setEnabled(false);

				} else {

                    File downloads = new File(shareFolder + "/../downloads");
                    if (!downloads.exists())
                        downloads.mkdir();

					if(slowMode) {
						console("Client starting in slow mode...");
					} else {
						console("Client starting...");
					}
                    pt.start();
					online = true;
					networkJoinLeave.setText("Leave Network");
                    enterServerIP.setEditable(false);

					try {

						InetSocketAddress targetAddress = new InetSocketAddress(enterServerIP.getText(), 2010);
						client = new RDT(3010, PeerPanel.this, peer.getList(), "client", slowMode);
                        client.server = targetAddress;
						Thread peerThread = new Thread(client);
						peerThread.start();

					} catch (Exception e1) {
                        console("Could not connect to server.");
                    }
				}
			}
		}
	}
	

	private class SelectionListener implements ActionListener {

		public void actionPerformed(ActionEvent e){

			if(!online) {

				console("You must join the network before requesting files silly!");

			} else {

				if (remoteTable.getRowCount() == 0) {

                    console("No files available for download.");

                } else {

					int row;
					String file;

					try {

                        row = remoteTable.getSelectedRows()[0];
                        file = Objects.toString(remoteTable.getValueAt(row, 0));
                        console("Asking the server for a peer with the file \"" + file + "\".");
                        peer.makeRequest(file);

                    } catch(Exception ex) {
                        ex.printStackTrace();
                    }
                }
			}
		}
	}
	
	private class SyncListener implements ActionListener {
		public void actionPerformed(ActionEvent e){
			if (!online)
				console("Join network before attempting to sync");
			else {
				try {
					InetSocketAddress targetAddress = new InetSocketAddress(enterServerIP.getText(), 2010);
					client = new RDT(3010, PeerPanel.this, peer.getList(), "client", slowMode);
                    client.server = targetAddress;
					Thread peerThread = new Thread(client);
					peerThread.start();
				} catch (Exception e1){console("Could not connect to server.");}
			}
		}
	}
}