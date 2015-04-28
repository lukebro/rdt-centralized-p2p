package edu.ccsu.main;

import edu.ccsu.gui.ServerPanel;
import javax.swing.JFrame;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Server {

	/**
	 * Main method for starting the Server window
	 * @param args
	 * @throws SocketException
	 * @throws UnknownHostException
	 */
	public static void main (String[] args) throws SocketException, UnknownHostException {
		
		JFrame frame = new JFrame ("Napster Server");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		ServerPanel panel = new ServerPanel();
		frame.getContentPane().add(panel);
		frame.pack();
		frame.setVisible(true);
		frame.setResizable(false);
	}

}
