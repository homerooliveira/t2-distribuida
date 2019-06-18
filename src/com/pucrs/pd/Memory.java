package com.pucrs.pd;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;

public class Memory {

    public static void main (String[]args){
        new Memory();
    }

    public Memory() {
        listen();
    }

    void listen() {
        try {
            DatagramSocket serverSocket = new DatagramSocket(4010);

            final byte[] receiveData = new byte[1024];

            try(PrintWriter print = new PrintWriter(new File("output.txt"))) {

                while (true) {
                    try {
                        final DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                        serverSocket.receive(receivePacket);

                        final String receivedMessage = new String(receivePacket.getData(), receivePacket.getOffset(),
                                receivePacket.getLength());

                        final String[] strings = receivedMessage.split(" ");
                        final String code = strings[2];
                        final int id = Integer.parseInt(strings[1]);

                        System.out.println("[" + id + " " + code + "]");
                        print.println("[" + id + " " + code + "]");
                        print.flush();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
