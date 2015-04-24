package edu.ccsu.gui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import edu.ccsu.main.PeerFrame;
import edu.ccsu.networking.RDTClient;
import edu.ccsu.networking.Peer;

public class PeerPanel extends JPanel {

	private JPanel northGrid, modeGrid,remoteBorder,remoteSouth, localBox, centerGrid;
	
	private JRadioButton normal, slow;
	private boolean slowMode = false;
	
	private JButton chooseShareFolder, networkJoinLeave, downloadFiles, queryServer;
	
	private DefaultTableModel remoteModel = new DefaultTableModel();
	private DefaultTableModel localModel = new DefaultTableModel();
	private JTable remoteTable = new JTable(remoteModel){public boolean isCellEditable(int row, int col){return false;}};
	private JTable localTable = new JTable(localModel){public boolean isCellEditable(int row, int col){return false;}};
	private JTextArea activity;
	private JScrollPane remoteScroll, localScroll, activityScroll;
	
	final JFileChooser fileChooser = new JFileChooser();
	private File[] myFiles;
	private Peer peer;
	private Thread pt;

	public PeerPanel() {
	
		peer = new Peer(this);
		pt = new Thread(peer);

		setLayout (new BorderLayout());

		chooseShareFolder = new JButton("Choose Shared Directory");
		FileListener fileListnr = new FileListener();
		chooseShareFolder.addActionListener(fileListnr);
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		
		networkJoinLeave = new JButton("Join/Leave Network");
		//ServerListener srvrListnr = new ServerListener();
		PeerListener peerListnr = new PeerListener();
		//networkJoinLeave.addActionListener(srvrListnr);
		networkJoinLeave.addActionListener(peerListnr);
		
		normal = new JRadioButton("Normal", true);
		slow = new JRadioButton("Slow");
		ButtonGroup mode = new ButtonGroup();
		mode.add(normal);
		mode.add(slow);
		ModeListener modeLstnr = new ModeListener();
		normal.addActionListener(modeLstnr);
		slow.addActionListener(modeLstnr);
		
		modeGrid = new JPanel();
		modeGrid.setLayout(new GridLayout(1,2));
		modeGrid.setBorder(BorderFactory.createTitledBorder("Transport Speed"));
		modeGrid.add(normal);
		modeGrid.add(slow);
		
		northGrid = new JPanel();
		northGrid.setLayout(new GridLayout(1,3));
		northGrid.setBorder(BorderFactory.createTitledBorder("Application Setup"));
		northGrid.add(chooseShareFolder);
		northGrid.add(networkJoinLeave);
		northGrid.add(modeGrid);

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

		activity = new JTextArea("System Ready",5,20);
		activityScroll = new JScrollPane(activity);

		add(activityScroll, BorderLayout.SOUTH);
	}
	
	public void getMessage(String message){
		activity.append("\n" + message);
	}
	
	public void listMyFiles(){
		for (int i=0; i<localModel.getRowCount(); i++){
			localModel.removeRow(i);
			peer.removeFile(i);
		}
		File folder = fileChooser.getSelectedFile();
		peer.setFolder(folder.getAbsolutePath());
		activity.append("\n" + folder.getAbsolutePath() + " set as Shared Directory");
		myFiles = folder.listFiles();
		for(File file : myFiles){
			if(file.getName().endsWith(".mp3")){
				localModel.addRow(new Object[]{file.getName(),file.getTotalSpace()});
				try {
					peer.addFile(file.getName(),file.getTotalSpace());
				} catch (UnknownHostException e1) {e1.printStackTrace();}
			}
		}
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
	
	private class FileListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			int returnVal = fileChooser.showOpenDialog(new JFrame());
			if (returnVal == JFileChooser.APPROVE_OPTION)
				listMyFiles();
			 else {}
		}
	}

	private class ServerListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if(slowMode = true) {
				activity.append("\nClient starting in slow mode...");
			} else {
				activity.append("\nClient starting...");
			}
			// Address of server
			InetAddress targetAddress;
			try {
				targetAddress = InetAddress.getByName("127.0.0.1");
				RDTClient client = new RDTClient(targetAddress, slowMode);
				client.rdtRequest("data.txt");
			} catch (Exception e1) {
				e1.printStackTrace();
			}

		}
	}

	
	private class PeerListener implements ActionListener {
		public void actionPerformed(ActionEvent e){
			if (!peer.folderSet())
				activity.append("\nPLEASE SELECT A FOLDER TO SHARE");
			else{
				pt.start();
			}
		}

	}
	
	private class SelectionListener implements ActionListener {
		public void actionPerformed(ActionEvent e){
			int row = remoteTable.getSelectedRows()[0];
			try {
				peer.makeRequest(peer.getRemoteFiles(row));
			} catch (IOException e1) {e1.printStackTrace();}
		}
	}
}