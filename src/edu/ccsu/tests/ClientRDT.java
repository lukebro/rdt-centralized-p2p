package edu.ccsu.tests;

import edu.ccsu.gui.ConsolePanel;
import edu.ccsu.networking.RDT;
import edu.ccsu.structures.Entries;

import java.io.IOException;
import java.net.InetSocketAddress;

public class ClientRDT implements ConsolePanel {

    public Entries database = new Entries();

    public ClientRDT() throws IOException, InterruptedException {


        database.addEntry("Bye bye bye.mp4", 1000);
        database.addEntry("Hey.mp4", 2000);
        database.addEntry("Welcome.mp4", 3000);
        database.addEntry("Hello world.mp4", 4000);


        RDT client = new RDT(3010, this, database);
        client.changeSlowMode(true);
        InetSocketAddress server = new InetSocketAddress("127.0.0.1", 2010);

        client.rdtPost(server);

        database.printAllEntries();

    }

    public void console(String message) {
        System.out.println("Client: " + message);
    }



    public void processEntries(String[][] entries) {
        for(int i = 0; i < entries.length; i++) {
            System.out.println(entries[i][0] + ": " + entries[i][1] + "\n");
        }
    }


}
