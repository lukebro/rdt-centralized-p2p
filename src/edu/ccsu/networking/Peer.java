package edu.ccsu.networking;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import edu.ccsu.gui.PeerPanel;
import edu.ccsu.structures.Entry;
import edu.ccsu.util.HttpUtil;

public class Peer implements Runnable {

	private String shareFolder = "";
	private ArrayList<Entry> fileList = new ArrayList<Entry>();
	private ArrayList<Entry> remoteFiles = new ArrayList<Entry>();
	private PeerPanel pp;

	public Peer(PeerPanel pp) {
		this.pp = pp;
	}

	public void run() {
		while(true) {
			Socket requestSocket;
			try {
				pp.getMessage("\nOnline and waiting to share.");
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
		fileList.add(new Entry(InetAddress.getLocalHost().toString(),name, size));
	}

	public void removeFile(int index){
		fileList.remove(index);
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
			String song = request.split(" ")[1];
			pp.getMessage(song + " being shared with " + ip.toString());
			if (Paths.get(shareFolder +"/" + song).isAbsolute()){
				Socket peerSocket;
				try {
					peerSocket = new Socket(ip,port);
					copyFile(song);
					DataOutputStream sendFile = new DataOutputStream(peerSocket.getOutputStream());
					peerSocket.getOutputStream().write(packet, 0, packet.length);;
					peerSocket.close();
					pp.getMessage("Done sending " + song + " to " + ip.toString());
				} catch (IOException e) {e.printStackTrace();}
			}
		}

		private byte[] copyFile(String fileName) throws IOException{
			File copy = new File(shareFolder + "/" + fileName);
			byte[] data = "HTTP/1.1 OK 202\r\nname: ".getBytes(). fileName.getBytes() "\r\nsize: ".getBytes()
					copy.getTotalSpace().getBytes()
					Files.readAllBytes(Paths.get(shareFolder + "/" + fileName));
			return data;
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
				byte[] packet = HttpUtil.buildPacket(HttpUtil.createHeader("GET", song),"0".getBytes());
				DataOutputStream request = new DataOutputStream(peerSocket.getOutputStream());
				request.write(packet);
				//InetAddress myIP = peerSocket.getLocalAddress();
				//int myPort = peerSocket.getLocalPort();
				//BufferedReader reply = new BufferedReader(New InputStream)
				peerSocket.close();
				pp.getMessage("Finished downloading " + song);
				pp.listMyFiles();
			} catch (IOException e) {e.printStackTrace();}
		}
	}
}

