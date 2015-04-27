package edu.ccsu.main;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import edu.ccsu.gui.PeerPanel;

import javax.swing.JFrame;

public class PeerFrame {

	public static void main(String[] args) {

		JFrame frame = new JFrame ("Napster Client");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		frame.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e){
				
			}
		});
		
		PeerPanel panel = new PeerPanel();
		frame.getContentPane().add(panel);
		frame.pack();
		frame.setVisible(true);
		frame.setResizable(false);
	}
	

}
