package edu.ccsu.util;

/**
 * Created by Lukasz Brodowski
 */
public class HttpUtil {

    private HttpUtil(){ }


    /*
     * Returns an ACK with either 0 or 1
     * @param int seq
     * @return ACK packet in byte[] form
     */
    public static byte[] createACK(int seq) {
        String[][] fields = {{"SEQ_NO", Integer.toString(seq)}};
        return createResponseHeader("ACK", "207", fields);
    }

    /*
     * Checks if packet is an ACK
     * @param packet in form of byte[]
     * @return boolean
     */
    public static boolean isACK(byte[] responsePacket) {
        String[] header = parseResponseHeader(responsePacket);

        if(header[0].equals("ACK") && header[1].equals("207"))
            return true;
        else
            return false;
    }

    /*
     * Gets the sequence number from ACK packet
     * @param ACK in the form of a byte[] packet
     * @return Seq number or -1 on fail.
     */
    public static int getSeq(byte[] responsePacket) {
        String[] header = parseResponseHeader(responsePacket);

        String[][] fields = parseFields(header[2]);

        if(fields[0][0].equals("SEQ_NO"))
            return Integer.parseInt(fields[0][1]);
        else
            return -1;
    }

    /**
     * Creates an HTTP response header
     * @param status HTTP status
     * @param code HTTP status code
     * @param fields all other fields
     * @return byte[] header
     */
    public static byte[] createResponseHeader(String status, String code, String[][] fields) {
        String header;
        header = "HTTP/1.1" + " " + status + " " + code + "\r\n";

        if(fields != null)
            for(int i = 0; i < fields.length; i++)
                header += fields[i][0] + ": " + fields[i][1] + "\r\n";

        header += "\r\n";

        return header.getBytes();
    }

        public static byte[] createResponseHeader(String status, String code) {
            return createResponseHeader(status, code, null);
        }

    /*
     * Creates an HTTP request header
     * @param String method - request method
     * @param String param - parameter for request method
     * @param String[][] fields - all other fields EX: String[][] fields = {{"Content-Type", "text/plain"},{"Timestamp", TIMESTAMP}};
     */
    public static byte[] createRequestHeader(String method, String param, String[][] fields) {
        String header;
        if(param == null)
            header = method + " HTTP/1.1\r\n";
        else
            header = method + " " + param + " HTTP/1.1\r\n";

        if(fields != null)
            for(int i = 0; i < fields.length; i++)
                header += fields[i][0] + ": " + fields[i][1] + "\r\n";

        header += "\r\n";

        return header.getBytes();
    }

        public static byte[] createRequestHeader(String method, String param) {
            return createRequestHeader(method, param, null);
        }

    /*
     * Parses header of request packet
     * @param HTTP packet in form of byte[]
     * @return [0] = method, [1] = parameter, [2] = all fields (to get fields, use HTTP.parseFields, and pass along index [2]
     */
    public static String[] parseRequestHeader(byte[] packet) {
        String[] line = (new String(packet).split("\r\n\r\n"))[0].split("\r\n"); // split header from body, then header to line to line
        String[] r = new String[3];
        String[] firstLine = line[0].split(" ");
        r[0] = firstLine[0];
        r[1] = (firstLine[1].equals("HTTP/1.1"))? null : firstLine[1];
        r[2] = "";

        if(line.length > 1)
            for(int i = 1; i < line.length; i++)
                r[2] += line[i] + "\r\n";

        return r;
    }

    /**
     * Parses header of response packet
     * @param packet HTTP response packet
     * @return [0] = status, [1] = code, [2] = all fields
     */
    public static String[] parseResponseHeader(byte[] packet) {
        String[] line = (new String(packet).split("\r\n\r\n"))[0].split("\r\n");
        String[] r = new String[3];
        String[] firstLine = line[0].split(" ");
        r[0] = firstLine[1];
        r[1] = firstLine[2];
        r[2] = "";

        if(line.length > 1)
            for(int i = 1; i < line.length; i++)
                r[2] += line[i] + "\r\n";

        return r;
    }

    /*
     * Parses fields of a HTTP packet
     * @param String[] of fields from parseHeader expected [2]
     */
    public static String[][] parseFields(String fields) {
        String[] field = fields.split("\r\n");
        String[][] r = new String[field.length][2];

        for(int i = 0; i < field.length; i++)
            r[i] = field[i].split(": ");

        return r;
    }

    /*
     * Combines header and data with no regard for size
     * @param header byte[] + data byte[]
     * @return Combined byte[] with header and data
     */
    public static byte[] buildPacket(byte[] header, byte[] data) {
        byte[] packet = new byte[header.length + data.length];

        System.arraycopy(header, 0, packet, 0, header.length);
        System.arraycopy(data, 0, packet, header.length, data.length);

        return packet;
    }

    /*
     * Strips header from packet and returns data
     * @param packet in byte[]
     * @return byte[] data of packet
     */
    public static byte[] getData(byte[] packet) {
        // Since bytes and characters should have 1 to 1 mapping using String for EASY manipulation
        String data[] = new String(packet).split("\r\n\r\n");
        String r = "";

        // Start at 1 since 0 is header
        for(int i = 1; i < data.length; i++) {
            r += data[i];
            if(i != data.length-1) r += "\r\n\r\n"; // Just in case actual data has \r\n\r\n in it
        }

        return r.getBytes();
    }


}
