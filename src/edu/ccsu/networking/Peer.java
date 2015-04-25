package edu.ccsu.networking;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Objects;

import edu.ccsu.gui.PeerPanel;
import edu.ccsu.structures.Entries;
import edu.ccsu.util.HttpUtil;

public class Peer implements Runnable {

	private String shareFolder = "";
	private Entries fileList = new Entries();
	private Entries remoteFiles = new Entries();
	private PeerPanel pp;

	public Peer(PeerPanel pp) {
		this.pp = pp;
	}

	public void run() {
		while(true) {
			Socket requestSocket;
			try {
				pp.getMessage("Waiting for requests...");
				requestSocket = new ServerSocket(1010).accept();
				takeRequest(requestSocket.getInetAddress(),requestSocket.getPort(),new BufferedReader(new InputStreamReader(requestSocket.getInputStream())).readLine());
			} catch (IOException e1) {e1.printStackTrace();}
		}
	}

	public void takeRequest(InetAddress ip, int port, String request) throws IOException{
		PeerServer ps = new PeerServer(ip, port, request);
		Thread pst = new Thread(ps);
		pst.start();
	}

	public void makeRequest (String song) throws UnknownHostException, IOException {
		pp.getMessage("Requesting address of " + song);
		/*
		 * Insert code to contact directory server for ip. 
		 * Pass to PeerClient constructor below.
		 */
		InetAddress ip = InetAddress.getByName("127.0.0.1");
		pp.getMessage("Downloading " + song + " from " + ip.toString());
		PeerClient pc = new PeerClient(ip, song);
		Thread pct = new Thread(pc);
		pct.start();
	}

	public void setFolder(String folder){
		this.shareFolder = folder;
	}
	public boolean folderSet(){
		if (shareFolder.isEmpty())
			return false;
		else
			return true;
	}

	public void addFile(String name, long size) throws UnknownHostException{
		fileList.addEntry(name, size);
	}

	public void removeFiles(){
		fileList.destroy();
	}

	public String getRemoteFiles(int entry){
		String selection;
		selection = remoteFiles.get(entry).getEntry();
		return selection;	
	}


	private class PeerServer extends Thread{

		InetAddress ip;
		int port;
		String request;


		public PeerServer(InetAddress ip, int port, String request){
			this.ip = ip;
			this.port = port;
			this.request = request;
		}

		public void run () {
			String file = request.split(" ")[1];
			pp.getMessage(file + " being shared with " + ip.toString());
			if (Paths.get(shareFolder +"/" + file).isAbsolute()){
				Socket peerSocket;
				try {
					peerSocket = new Socket(ip,port);
					byte[] packet = copyFile(file);
					DataOutputStream sendFile = new DataOutputStream(peerSocket.getOutputStream());
					peerSocket.getOutputStream().write(packet, 0, packet.length);;
					peerSocket.close();
					pp.getMessage("Done sending " + file + " to " + ip.toString());
				} catch (IOException e) {e.printStackTrace();}
			}
		}

		private byte[] copyFile(String fileName) throws IOException{
			File copy = new File(shareFolder + "/" + fileName);
			String[][] fields = {{"peer",fileName},{"size",Objects.toString(copy.getTotalSpace(), null)}};
			byte[] header = HttpUtil.createResponseHeader("OK", "202", fields);
			byte[] data = Files.readAllBytes(Paths.get(shareFolder + "/" + fileName));
			byte[] packet = HttpUtil.buildPacket(header, data);
			return packet;
		}

	}

	private class PeerClient extends Thread {

		InetAddress ip;
		String song;

		public PeerClient(InetAddress ip, String song) {
			this.ip = ip;
			this.song = song;
		}

		public void run() {
			Socket peerSocket;
			try {
				peerSocket = new Socket(ip,1010);
				byte[] packet = HttpUtil.buildPacket(HttpUtil.createRequestHeader("GET", song), null);
				DataOutputStream request = new DataOutputStream(peerSocket.getOutputStream());
				request.write(packet);
				BufferedReader reply = new BufferedReader(new InputStreamReader(peerSocket.getInputStream()));
				peerSocket.close();
				pp.getMessage("Finished downloading " + song);
				pp.listMyFiles();
			} catch (IOException e) {e.printStackTrace();}
		}
	}
}

