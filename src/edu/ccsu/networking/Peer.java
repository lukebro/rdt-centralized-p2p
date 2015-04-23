package edu.ccsu.networking;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import edu.ccsu.structures.Entry;
import edu.ccsu.util.HttpUtil;

public class Peer implements Runnable {

	private String shareFolder;
	private ArrayList<Entry> fileList = new ArrayList<Entry>();
	private ArrayList<Entry> remoteFiles = new ArrayList<Entry>();

	public Peer() {
		run();
	}
	
	public void run() {
		
	}
	
	public void takeRequest(InetAddress ip, int port, String request) throws IOException{
		PeerServer ps = new PeerServer(ip, port, request);
		}
	
	public void makeRequest (String song) throws UnknownHostException, IOException {
		/*
		 * Insert code to contact directory server for ip. 
		 * Pass to PeerClient constructor below.
		 */
		InetAddress ip = InetAddress.getByName("127.0.0.1");
		PeerClient pc = new PeerClient(ip, song);
	}
	
	public void setFolder(String folder){
		this.shareFolder = folder;
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
		run();
	}
	
	public void run () {
		String song = request.split(" ")[1];
		if (Paths.get(shareFolder +"/" + song).isAbsolute()){
			Socket peerSocket;
			try {
				peerSocket = new Socket(ip,port);
			copyFile(shareFolder +"/" + song);
			DataOutputStream sendFile = new DataOutputStream(peerSocket.getOutputStream());
			peerSocket.close();
			} catch (IOException e) {e.printStackTrace();}
		}
	}
	
	private byte[] copyFile(String fileName) throws IOException{
		byte[] data = Files.readAllBytes(Paths.get(fileName));
		return data;
	}
	
}

private class PeerClient extends Thread {
	
	InetAddress ip;
	String song;

	public PeerClient(InetAddress ip, String song) {
		this.ip = ip;
		this.song = song;
		run();
	}
	
	public void run() {
		Socket peerSocket;
		try {
			peerSocket = new Socket(ip,6789);
		byte[] packet = HttpUtil.buildPacket(HttpUtil.createHeader("GET", song),"0".getBytes());
		DataOutputStream request = new DataOutputStream(peerSocket.getOutputStream());
		request.write(packet);
		InetAddress myIP = peerSocket.getLocalAddress();
		int myPort = peerSocket.getLocalPort();
		peerSocket.close();
		} catch (IOException e) {e.printStackTrace();}
	}
	
}

}

