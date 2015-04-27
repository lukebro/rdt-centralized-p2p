package edu.ccsu.tests;

import java.io.*;
import java.net.Socket;
import edu.ccsu.util.HttpUtil;

class TCPClient {


    public static void main(String[] args) throws Exception {

        byte[] header = HttpUtil.createRequestHeader("GET", "song");

        byte[] data = new String("This is my data HELLO!").getBytes();

        byte[] packet = HttpUtil.buildPacket(header, data);


        byte[] split = HttpUtil.getData(packet);


        System.out.println(new String(split));
    /*
        Socket clientSocket = new Socket("127.0.0.1", 4010);

        DataOutputStream output = new DataOutputStream(clientSocket.getOutputStream());
        DataInputStream input = new DataInputStream(clientSocket.getInputStream());

        byte[] packet = HttpUtil.createRequestHeader("GET", "this");


        output.write(packet);

        byte[] buffer = new byte[2048];

        int i;

        while ((i = input.read(buffer)) != -1) {
            System.out.print(new String(buffer));
        }



        clientSocket.close();

*/
    }
}