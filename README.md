# CS 490 Final Project Group 10
## Team members
Lukasz Brodowski & Kevin Daley

## Implementation instructions

1. Compile all the files, and put the entire directory on separate computers.

2. Make sure there is a `data.txt` inside the same directory as Server.class.

3. Start the server on one computer: `java Server -slow`

4. Start the client on another computer with the IP address being the Server computer's IP: `java Client 192.168.1.1 -slow`

5. To lose the packet of data being sent, disconnect when Server notifies that the it will be sending next packer in 5 seconds.

6. To lose the ACK, disconnect when Client says it will ACK back in 5 seconds.

7. After the Server is done sending, it will flag the last packet.

8. On receiving the flagged/last packet the Client will display the contents of `data.txt`, the build of all the packets sent

9. To run normally, omit the `-slow` in both commands.

10. Enjoy RDT.