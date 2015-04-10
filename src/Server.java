import edu.ccsu.networking.RDTServer;

import java.io.FileInputStream;
import java.io.File;
import java.io.IOException;

public class Server {
    public static void main(String[] args) throws InterruptedException, IOException {

        boolean slowMode = false;

        // Parse argument, and set Slow Mode to true if -slow is passed
        if(args.length > 0 && args[0].equals("-slow")) {
            System.out.println("Server starting in slow mode...");
            slowMode = true;
        } else {
            System.out.println("Server starting...");
        }


        RDTServer server = new RDTServer(slowMode);

        System.out.println("Reading data from data.txt.");
        byte[] data = readFromFile("data.txt");

        // Save data we read from data.txt into our object
        server.saveData(data);

        // Start server in infinite loop
        while(true) {

            server.waitFromBelow();

            System.out.println("Request finished sending, back to waiting form below.");
        }
    }

    /*
     * Read bytes from a file
     * @param Path to file
     * @return byte[] of file
     */
    private static byte[] readFromFile(String path) throws IOException {

        File file = new File(path);
        FileInputStream fileStream = new FileInputStream(file);

        byte[] data = new byte[ (int)file.length() ];

        fileStream.read(data,0,data.length);

        return data;
    }

}
