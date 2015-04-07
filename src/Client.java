import edu.ccsu.networking.RDTClient;

import java.io.IOException;

public class Client {
    public static void main(String[] args) throws IOException, InterruptedException {


        boolean slowMode = false;

        if(args.length > 0 && args[0].equals("-slow")) {
            System.out.println("Client starting in slow mode...");
            slowMode = true;
        } else {
            System.out.println("Client starting...");
        }

        byte[] targetAdddress = {127,0,0,1};
        RDTClient client = new RDTClient(targetAdddress, 3500, 3600, slowMode);

        client.rdtReceive();

    }
}
