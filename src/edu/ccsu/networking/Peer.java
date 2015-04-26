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
import edu.ccsu.networking.RDT;

public class Peer implements Runnable {

	private String shareFolder = "";
	private Entries fileList = new Entries();
	private Socket requestSocket;
	private PeerPanel pp;

	public Peer(PeerPanel pp) {
		this.pp = pp;
	}
	
	public Entries getList(){
		return fileList;
	}

	public void run() {
		while(true) {
			try {
				pp.console("Waiting for requests...");
				requestSocket = new ServerSocket(1010).accept();
				takeRequest(requestSocket.getInetAddress(),requestSocket.getPort(),new BufferedReader(new InputStreamReader(requestSocket.getInputStream())).readLine());
			} catch (IOException e1) {e1.printStackTrace();}
		}
	}
	
	public void leaveNetwork() throws IOException {
		requestSocket.close();
		pp.console("Signed Off.");
	}

	public void takeRequest(InetAddress ip, int port, String request) throws IOException{
		PeerServer ps = new PeerServer(ip, port, request);
		Thread pst = new Thread(ps);
		pst.start();
	}

	public void makeRequest (String song) throws UnknownHostException, IOException, ConnectException, InterruptedException {
		pp.console("Requesting address of " + song);

        /// Access to server socket address from PP
        InetSocketAddress server = new InetSocketAddress("127.0.0.1", 2010);

        // will fail if other threads exist
        RDT client = new RDT(3010, pp);

        String peerIp = client.rdtRequest(song, server);


		InetAddress ip = InetAddress.getByName(peerIp);
		pp.console("Downloading " + song + " from " + ip.toString());
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

	private class PeerServer extends Thread {

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
			pp.console(file + " being shared with " + ip.toString());
			if (Paths.get(shareFolder +"/" + file).isAbsolute()){
				Socket peerSocket;
				try {
					peerSocket = new Socket(ip,port);
					byte[] packet = copyFile(file);
					DataOutputStream sendFile = new DataOutputStream(peerSocket.getOutputStream());
					peerSocket.getOutputStream().write(packet, 0, packet.length);;
					peerSocket.close();
					pp.console("Done sending " + file + " to " + ip.toString());
				} catch (IOException e) {e.printStackTrace();}
			}
		}

		private byte[] copyFile(String fileName) throws IOException{
			File copy = new File(shareFolder + "/" + fileName);
			String[][] fields = {{"peer",fileName},{"size",Objects.toString(copy.length(), null)}};
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
				pp.console("Finished downloading " + song);
				pp.listMyFiles();
			} 
			catch (ConnectException e1){pp.console("The host of " + song + " is unavailable");}
			catch (IOException e) {pp.console("There was an error downloading " + song);}
		}
	}
}

