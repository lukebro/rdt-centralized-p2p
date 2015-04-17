package edu.ccsu.util;

public class Entry {
	
	public final String ip;
	public final String entry;
	public final long size;

	public Entry(String ip, String entry, long size) {
		this.ip = ip;
		this.entry = entry;
		this.size = size;
	}
	
	public String getHost(){
		return ip;
	}
	
	public String getEntry(){
		return entry;
	}
	
	public long getSize(){
		return size;
	}

}