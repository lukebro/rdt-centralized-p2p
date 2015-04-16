
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import edu.ccsu.util.HttpUtil;

//import Entry;
//import HttpUtil;

public class Peer {

	private String shareFolder;
	private ArrayList<Entry> fileList = new ArrayList<Entry>();
	private ArrayList<Entry> remoteFiles = new ArrayList<Entry>();

	public Peer() {
		// TODO Auto-generated constructor stub
	}
	
	public void takeRequest(InetAddress ip, int port, String request) throws IOException{
		String song = request.split(" ")[1];
		if (Paths.get(shareFolder +"/" + song).isAbsolute()){
			Socket peerSocket = new Socket(ip,port);
			// Build Packet
			DataOutputStream sendFile = new DataOutputStream(peerSocket.getOutputStream());
			peerSocket.close();
		}
		}
	
	public void makeRequest (ArrayList<Entry> songList) throws UnknownHostException, IOException {
		byte[] noData = "0".getBytes();
		for (Entry entry : songList){
			Socket peerSocket = new Socket(entry.getHost(),6789);
			byte[] packet = HttpUtil.buildPacket(HttpUtil.createHeader("GET", entry.getEntry()),noData);
			DataOutputStream request = new DataOutputStream(peerSocket.getOutputStream());
			request.write(packet);
			InetAddress myIP = peerSocket.getLocalAddress();
			int myPort = peerSocket.getLocalPort();
			peerSocket.close();
			
		}
	}
	
	public void setFolder(String folder){
		this.shareFolder = folder;
	}
	
	public void addFile(String name, long size){
		fileList.add(new Entry("127.0.0.1",name, size));
	}
	
	public void removeFile(int index){
		fileList.remove(index);
	}
	
	private byte[] copyFile(String fileName) throws IOException{
		byte[] data = Files.readAllBytes(Paths.get(shareFolder + "/" + fileName));
		return data;
	}
	
	public ArrayList<Entry> getRemoteFiles(int[] entries){
		ArrayList<Entry> selection = new ArrayList<Entry>();
		for (int i = 0; i < entries.length; i++) {
			selection.add(remoteFiles.get(entries[i]));
		}
		return selection;	
	}

}

