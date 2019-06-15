package com.pucrs.pd;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class Server {
    public static final int DEFAULT_PORT = 4000;


    public static void main(String[] args) {

    }

    public Server() {
    }

    public void run() {
//        new Thread(this::listen).start();
//        new Thread(this::sendSignal).start();
    }

    // listens the direct communications with this node
    void listen() {
        try {
            DatagramSocket serverSocket = new DatagramSocket(DEFAULT_PORT);

            final byte[] receiveData = new byte[1024];

            while (true) {
                final DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(receivePacket);

                final String receivedMessage = new String(receivePacket.getData(), receivePacket.getOffset(),
                        receivePacket.getLength());

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    void sendToNode(ResponseRequest response, String nodeIp) {
//        try {
//            String json = new Gson().toJson(response);
//            byte[] sendData = json.getBytes(Charset.forName("utf8"));
//
//            DatagramSocket socket = new DatagramSocket();
//            InetAddress address = InetAddress.getByName(nodeIp);
//            DatagramPacket sendPacket = new DatagramPacket(
//                    sendData,
//                    sendData.length,
//                    address,
//                    DEFAULT_PORT);
//
//            socket.send(sendPacket);
//            socket.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
}
