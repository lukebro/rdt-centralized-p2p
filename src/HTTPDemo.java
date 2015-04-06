/**
 * Created by Lukasz Brodowski
 * Example of how HTTP utility class works.
 *
 * Note:
 * The HTTP util class just parses any packet you put in there
 * If we really wanted to be thorough we would test for HTTP/10.2 or whatever we want our version to be
 * But for the final project i don't think we have to worry about compatibilities like that...
 */

import edu.ccsu.util.HTTP;

public class HTTPDemo {
    public static void main(String[] args) {

        System.out.println("------ Sender ------ \n");
        /* --------- Sender ---------- */

        // settings fields in String[][] array
        String[][] fields = {{"Content-Type", "audio/mpeg"}, {"Date", "Tue, 15 Nov 1994 08:12:31 GMT"}};

        // create byte[] of header
        byte[] header = HTTP.createHeader("POST", "/user/give/HeartbeatSong-KellyClarkson.mp4", fields);

        // data (should be split up)
        byte[] data = new String("[Chorus]\n" +
                "This is my heartbeat song and I'm gonna play it\n" +
                "Been so long I forgot how to turn it up up up up all night\n" +
                "Oh up up all night long\n" +
                "\n" +
                "[Verse 1]\n" +
                "You, where the hell did you come from?\n" +
                "You were a different, different kind of fun\n" +
                "And still used to feel it now\n" +
                "Now, I got pins and needles on my tongue\n" +
                "Anticipating what's to come\n" +
                "Like a finger on a loaded gun\n" +
                "\n" +
                "I can feel it rising\n" +
                "Temperature inside me\n" +
                "Haven't felt for a long time\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "[Chorus]\n" +
                "This is my heartbeat song and I'm gonna play it\n" +
                "Been so long I forgot how to turn it up up up up all night\n" +
                "Oh up up all night long\n" +
                "This is my heartbeat song and I'm gonna play it\n" +
                "Turn it on\n" +
                "But I know you can take it up up up up all night\n" +
                "Oh up up all night long\n" +
                "\n" +
                "Source: http://www.directlyrics.com/kelly-clarkson-heartbeat-lyrics.html").getBytes();

        // For demo just pack all data into 1 packet
        byte[] packet = HTTP.buildPacket(header, data);

        // Send data out here
        System.out.println("Sending packet:");
        System.out.println(new String(packet));

        /* ------------- Receiver --------- */
        System.out.println("\n\n\n");
        System.out.println("\n\n\n------ Receiver ------ \n");
        // packet is received and converted to byte[]

        // read packet header
        String[] packetInfo = HTTP.parseHeader(packet);

        // packetInfo[0] = POST
        // packetInfo[1] = /user/give/ByeByeBye-NSYNC.mp4
        // packetInfo[2] = all fields, still need to be parsed!
        System.out.println("Received Packet Info:");
        for(int i = 0; i < packetInfo.length; i++)
            System.out.println(packetInfo[i]);

        // now parse fields if needed
        String[][] allFields = HTTP.parseFields(packetInfo[2]);

        System.out.println("\n\n\n");
        System.out.println("All fields:");
        for(int i = 0; i < allFields.length; i++) {
            System.out.println(allFields[i][0] + ": " + allFields[i][1]);
        }


        // Do whatever you need to do with method/parameter/fields from HTTP

        // Get data and deliver
        System.out.println("\n\n\n");
        System.out.println("Get data:");
        byte[] packetData = HTTP.getData(packet);

        System.out.println(new String(packetData));


        // Also more methods for ACKs, but those will only be used in UDP
        // Method for creating ACK packet / getting sequence number from ACK packet are there
    }
}
