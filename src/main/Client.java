package main;
import edu.ccsu.networking.RDTClient;

import java.io.IOException;
import java.net.InetAddress;

public class Client {
    public static void main(String[] args) throws IOException, InterruptedException {


        boolean slowMode = false;

        if(args.length > 1 && args[1].equals("-slow")) {
            System.out.println("Client starting in slow mode...");
            slowMode = true;
        } else {
            System.out.println("Client starting...");
        }

        // Address of server
        InetAddress targetAddress = InetAddress.getByName(args[0]);

        RDTClient client = new RDTClient(targetAddress, slowMode);


        client.rdtRequest("data.txt");

    }
}
