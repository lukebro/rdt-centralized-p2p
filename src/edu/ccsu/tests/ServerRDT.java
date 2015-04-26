package edu.ccsu.tests;

import edu.ccsu.gui.ConsolePanel;
import edu.ccsu.networking.RDT;
import edu.ccsu.structures.Entries;

import java.io.IOException;

public class ServerRDT implements ConsolePanel {

    private Entries database = new Entries();

    public ServerRDT() throws IOException, InterruptedException {


        RDT server = new RDT(2010, this, database, "server");
        Thread ok = new Thread(server);

        ok.start();
    }

    public void console(String message) {
        System.out.println("Server: " + message);
    }

    public void processEntries(String[][] entries) {}
}
