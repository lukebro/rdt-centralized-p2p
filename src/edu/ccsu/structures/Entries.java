package edu.ccsu.structures;

public class Entries {
	



	private class Entry {
		private String name;
        private String ip;
        private long size;

        public Entry(String name, String ip, long size) {
            this.name = name;
            this.ip = ip;
            this.size = size;
        }

        public String getName() { return this.name; }
        public String getIp() { return this.ip; }
        public long getSize() { return this.size; }
	}
}