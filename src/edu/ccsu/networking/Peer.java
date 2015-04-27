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
    private boolean requester;
    private ServerSocket serverSocket;

	public Peer(PeerPanel pp) {
		this.pp = pp;
        requester = true;
	}
	
	public Entries getList(){
		return fileList;
	}

	public void run() {

        try {
            this.serverSocket = new ServerSocket(4010);
        } catch(IOException e) {
            e.printStackTrace();
        }

        while(requester) {
			try {

                Socket clientSocket = this.serverSocket.accept();

                pp.console("Got a request, spawning new thread to let peer download file!");

                // Start new thread and pass socket to peer server = WOOOOOOOOOOOOOHOOOOOO bkz i finally figure it out
                new Thread(new PeerServer(clientSocket)).start();


			} catch (Exception e1) {e1.printStackTrace();}
		}

	}

    public void setRequester(boolean request) {
        this.requester = request;
    }
	
	public void leaveNetwork() throws IOException {
        setRequester(false);
		serverSocket.close();

		pp.console("Disconnected from network.");
	}

	public void makeRequest (String song) throws IOException, InterruptedException {
        byte[] header = HttpUtil.createRequestHeader("REQUEST", song);

        pp.console("Asking server who has the file: \"" + song + "\".");

        RDT server = new RDT(3010, pp, this.getList(), "client", false);
        InetSocketAddress serverAddress = new InetSocketAddress(pp.enterServerIP.getText(), 2010);

        String ip = server.rdtRequest(song, serverAddress);

        pp.console("Asking " + ip + " for the file \"" + song + "\".");

        new Thread(new PeerClient(ip, song)).start();
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

    private class PeerClient implements Runnable {

        private Socket peerSocket;
        private InputStream input;
        private OutputStream output;
        private String song;
        private String ip;

        public PeerClient(String ip, String song) {
            this.song = song;
            this.ip = ip;
            try {
                this.peerSocket = new Socket(ip, 4010);
                input  = this.peerSocket.getInputStream();
                output = this.peerSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * !!!THIS RUN METHOD NEEDS TO SAVE THE FILE INSTEAD OF PRINTING IT OUT!!!
         */
        public void run() {

            byte[] request = HttpUtil.createRequestHeader("GET", this.song);

            FileOutputStream fileO = null;
            try {
                fileO = new FileOutputStream(new File(shareFolder + "/../downloads/" + song));
            } catch (FileNotFoundException e) {

            }

            try {
                output.write(request);
            } catch (IOException e) {
                e.printStackTrace();
            }

            byte[] buffer = new byte[2048];

            pp.console("Downloading file \"" + this.song + "\" from " + this.ip + ".");

            try {
                int i = input.read(buffer);

                // Get rid of header
                byte[] header = HttpUtil.getData(buffer);

                fileO.write(header);

                while ((i = input.read(buffer)) != -1) {
                    fileO.write(buffer);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                peerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            pp.console("File \"" + this.song + "\" has finished downloading, it's in your downloads.");

        }

    }

	private class PeerServer implements Runnable {

        private Socket peerSocket;
        private InputStream input;
        private OutputStream output;

		public PeerServer(Socket client) {
            this.peerSocket = client;

            try {
                input  = this.peerSocket.getInputStream();
                output = this.peerSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
		}

		public void run() {

            byte[] request = new byte[128];

            try {
                input.read(request);
            } catch (IOException e) {
                e.printStackTrace();
            }

            String[] packetInfo = HttpUtil.parseRequestHeader(request);

            String fileName = packetInfo[1];

            pp.console("Sending \"" + fileName + "\" to client.");

            byte[] header = HttpUtil.createResponseHeader("OK", "202");

            if (Paths.get(shareFolder +"/" + fileName).isAbsolute()) {
                byte[] data = copyFile(fileName);

                byte[] responsePacket = HttpUtil.buildPacket(header,data);

                try {
                    output.write(responsePacket);
                    output.flush();
                    peerSocket.close();
                } catch (IOException e) { e.printStackTrace(); }

            }

		}

        private byte[] copyFile(String fileName) {
            byte[] data = new byte[0];
            try {
                data = Files.readAllBytes(Paths.get(shareFolder + "/" + fileName));
            } catch (IOException e) { e.printStackTrace(); }

            return data;
        }
	}
}

