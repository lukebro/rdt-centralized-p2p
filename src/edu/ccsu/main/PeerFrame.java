package edu.ccsu.main;

import edu.ccsu.gui.PeerPanel;
import javax.swing.JFrame;

public class PeerFrame {

	public static void main(String[] args) {

		JFrame frame = new JFrame ("Napster Client");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		PeerPanel panel = new PeerPanel();
		frame.getContentPane().add(panel);
		frame.pack();
		frame.setVisible(true);
		frame.setResizable(false);
	}

}
