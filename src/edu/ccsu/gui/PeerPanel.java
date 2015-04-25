package edu.ccsu.gui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

import edu.ccsu.networking.RDTClient;
import edu.ccsu.networking.Peer;

public class PeerPanel extends JPanel implements ConsolePanel {

	private JPanel northGrid, networkGrid,remoteBorder,remoteSouth, localBox, centerGrid;
	
	private JRadioButton normal, slow;
	private boolean slowMode = false;
	
	private JLabel serverLabel = new JLabel("Directory IP: ");
	private JTextField enterServerIP = new JTextField();
	
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
	
	private boolean online = false;

    public void processEntries(String[][] entries) {}

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
		networkGrid.add(serverLabel);
		networkGrid.add(enterServerIP);
		networkGrid.add(normal);
		networkGrid.add(slow);
		
		northGrid = new JPanel();
		northGrid.setLayout(new GridLayout(2, 2));
		northGrid.setBorder(BorderFactory.createTitledBorder("Application Setup"));
		northGrid.add(networkGrid);
		northGrid.add(networkJoinLeave);
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
		
		remoteModel.addRow(new Object[]{"My file", 1000});
		
	}
	
	public void console(String message){
		activity.append(message + "\n");
		activity.selectAll();
	}
	
	public void listMyFiles(){
		peer.removeFiles();
		File folder = fileChooser.getSelectedFile();
		peer.setFolder(folder.getAbsolutePath());
		console(folder.getAbsolutePath() + " set as Shared Directory");
		myFiles = folder.listFiles();
		for(File file : myFiles){
			localModel.addRow(new Object[]{file.getName(),file.length()});
			try {
				peer.addFile(file.getName(),file.length());
			} catch (UnknownHostException e1) {e1.printStackTrace();}
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

	private class PeerListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if (!peer.folderSet()){
				console("PLEASE SELECT A FOLDER TO SHARE");
			}
			else{
				pt.start();
				if(slowMode = true) {
					console("Client starting in slow mode...");
				} else {
					console("Client starting...");
				}
				if (online){
					try {peer.leaveNetwork();} catch (IOException e1) {e1.printStackTrace();}
					online = false;
					networkJoinLeave.setText("Join Network");
				}
				else{	
					online = true;
					networkJoinLeave.setText("Leave Network");
					InetAddress targetAddress;
					try {
						targetAddress = InetAddress.getByName("127.0.0.1");//Update to server IP
						RDTClient client = new RDTClient(targetAddress, slowMode);
						//RDT Client should compile file list and send it to directory server
						//then populate remoteFileList with response
						// Will the server send updated list to other peers when new peer joins?
						client.rdtRequest("data.txt");//remove?
					} catch (Exception e1) {e1.printStackTrace();}
				}
			}
		}
	}

	private class SelectionListener implements ActionListener {
		public void actionPerformed(ActionEvent e){
			if (!online){
				console("You must join the network before requesting files.");
			}
			else {
				if (remoteTable.getRowCount() == 0)
					console("No files available for download");
				else {
					int row;
					String file;
					try {
						row = remoteTable.getSelectedRows()[0];
						file = Objects.toString(remoteTable.getValueAt(row, 0));
						console("Requesting " + file);
						peer.makeRequest(file);
					}
					catch(ArrayIndexOutOfBoundsException e1) {console("Please select a file");}
					catch (ConnectException e1) {e1.printStackTrace();}
					catch (UnknownHostException e1) {e1.printStackTrace();}
					catch (IOException e1) {e1.printStackTrace();}
				}
			}
		}
	}
}