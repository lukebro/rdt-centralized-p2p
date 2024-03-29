package edu.ccsu.gui;

import edu.ccsu.structures.Entries;

/**
 * Interface for ServerPanel + PeerPanel to use to share database and console with RDT and Peer files.
 */

public interface ConsolePanel {

    public void console(String message);

    public void processEntries(String[][] entries);
}
