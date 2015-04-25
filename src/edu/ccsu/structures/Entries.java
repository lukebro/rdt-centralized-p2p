package edu.ccsu.structures;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Entries Data Structure for maintaining files, their sizes, and what peers have them
 * Both for Client & Server
 * Client does not store IPs, Server does
 */
public class Entries {

    /**
     * Synchronized Hashtable for keeping track of files and their size
     * as well as what peers have the file
     */
    private Map<String, Entry> Database = new ConcurrentHashMap<String, Entry>();

    /**
     * Add Entry to Database, if entry is already in adds peer to entry
     * @param name Name of song
     * @param size Size of song
     * @param ip IP of peer who has song
     */
    public void addEntry(String name, long size, String ip) {
        if(!Database.containsKey(name)) {
            Entry e = new Entry(size, ip);
            Database.put(name, e);
        } else {
            Database.get(name).addPeer(ip);
        }
    }

    /**
     * Override for no IP, use for Client
     * @param name Name of song
     * @param size Size of song
     */
    public void addEntry(String name, long size) {
        this.addEntry(name, size, null);
    }

    /**
     * Delete peer from database, if peer is only one who contains file delete file too
     * @param ip IP of peer to be deleted
     */
    public void deletePeer(String ip) {
        Set<String> keys = Database.keySet();
        for(String key: keys){
            Entry e = Database.get(key);
            e.deletePeer(ip);

            if(e.peerCount() == 0)
                Database.remove(key);
        }
    }

    /**
     * Destroy the database deleting all items
     */
    public void destroy() {
        Database.clear();
    }

    /**
     * Add entries from a field area of HTTP header
     * @param entries String[][] of fields from header
     */
    public void addEntries(String[][] entries, String ip) {
        for(int i = 0; i < entries.length; i++) {
            this.addEntry(entries[i][0], Long.parseLong(entries[i][1]), ip);
        }
    }

    /**
     * Returns all entries in database with first peer in entry
     * @return {{name, size}}
     */
    public String[][] getEntries() {
        Set<String> keys = Database.keySet();
        String[][] entries = new String[Database.size()][2];
        int i = 0;
        for(String key: keys){
            entries[i][0] = key;
            entries[i][1] = String.valueOf(Database.get(key).getSize());
            i++;
        }

        return entries;
    }

    /**
     * Print all entries in database
     */
    public void printAllEntries() {
        Set<String> keys = Database.keySet();
        for(String key: keys){
            System.out.println("Name: " + key + " " + Database.get(key));
        }
    }

    /**
     * Returns string of peer with file
     * @param name
     * @return peer ip
     */
    public String getPeer(String name) {
        return Database.get(name).getIp();
    }


    /**
     * Entry structure for maintaining peers and size of song
     */
    private class Entry {
        private List<String> ip;
        private long size;

        public Entry(long size, String ip) {
            this.ip = new ArrayList<String>();
            this.size = size;

            if(ip != null)
                this.ip.add(ip);
        }

        /**
         * Remove peer from ArrayList
         * @param ip IP of peer
         */
        public void deletePeer(String ip) {
            if(this.ip.contains(ip)) {
                this.ip.remove(ip);
            }
        }

        /**
         * Return the IP of a peer who has the file
         * @return IP of peer
         */
        public String getIp() { return this.ip.get(0); }

        /**
         * Return size of the file
         * @return long
         */
        public long getSize() { return this.size; }

        /**
         * Return the number of peers who have this file
         * @return int
         */
        public int peerCount() { return this.ip.size(); }

        /**
         * Add a peer to the array list
         * @param ip peer who has file
         */
        public void addPeer(String ip) { this.ip.add(ip); }

        /**
         * Override for printing all entries
         * @return String
         */
        public String toString() {
            if(this.ip.size() == 0)
                return new String("Size: " + this.size);
            else
                return new String("Size: " + this.size + " IP:" + this.ip.get(0));
        }
    }
}