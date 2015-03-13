import edu.ccsu.networking.RDT10Receiver;
import edu.ccsu.networking.RDT10Sender;

/**
 * These classes contain a very simple example of a UDT send and receive similar
 * to the RDT 1.0 covered in class.
 * A receiver thread is started that begins listening on 49000 that waits
 * for packets to delivers.  A sending socket is then
 * opened for sending data to the receiver.
 *
 * With a real sender a large message will need to be broken into packets so
 * this demo also shows one way to break a larger message up this way.
 *
 * @author Chad Williams
 * Editied by Luke
 *
 * Reads from file.txt than passes the bytes to rdtSend
 */
public class Main {
    public static void main(String[] args) {

        RDT10Receiver receiverThread = null;
        try {
            // Start receiver
            receiverThread = new RDT10Receiver("Receiver", 49000);
            receiverThread.start();

            // Create sender
            byte[] targetAdddress = {127,0,0,1};
            RDT10Sender sender = new RDT10Sender();
            sender.startSender(targetAdddress,49000);

            for(int i=0;i<10;i++) {
                String data = "Here is the message I want to send";
                // Send the data
                sender.rdtSend(data.getBytes());

                // Sleeping simply for demo visualization purposes
                Thread.sleep(10000);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
