package com.pucrs.pd;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class Server {
    public static final int DEFAULT_PORT = 4000;
    private final int id;
    private Node mySelf;
    private List<Node> nodes;
    private boolean isCoordinator;
    private Node lock;
    private Node currentCoordinator;
    private AtomicBoolean hasLock = new AtomicBoolean(false);

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Número de argumentos errado");
            return;
        }
        int id = Integer.parseInt(args[0]);
        try {
            new Server(id).run();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Server(int id) {
        this.id = id;
    }

    public void run() throws IOException {
        final Path path = Paths.get("config.txt");
        nodes = Files.lines(path)
                .map(Node::new)
                .collect(Collectors.toList());
        isCoordinator = id == nodes.size();
        mySelf = nodes.get(id - 1);
        currentCoordinator = nodes.get(nodes.size() - 1);

        if(isCoordinator) {
            new Thread(this::listenToNodes).start();
        } else {
            new Thread(this::send).start();
            new Thread(this::listemToCoordiantor).start();
        }

    }

    // listens the direct communications with this node
    void listenToNodes() {
        try {
            DatagramSocket serverSocket = new DatagramSocket(mySelf.getPort());

            final byte[] receiveData = new byte[1024];

            while (true) {
                final DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(receivePacket);

                final String receivedMessage = new String(receivePacket.getData(), receivePacket.getOffset(),
                        receivePacket.getLength());

                final String[] strings = receivedMessage.split(" ");
                final String code = strings[1];
                final int id = Integer.parseInt(strings[0]);
                final Node node = nodes.get(id - 1);

                switch (code) {
                    case Codes.REQ:
                        if (lock == null) {
                            sendToNode(node, Codes.GRANT);
                            lock = node;
                            System.out.println("Processo " + node.getId() + " ganhou o lock");
                            new Thread(() -> this.coordinatorUnlock(id)).start();
                        } else {
                            sendToNode(node, Codes.DENIED);
                        }
                        break;
                    case Codes.RELEASE:
                        lock = null;
                        System.out.println("Processo " + node.getId() + " liberou o lock");
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void sendToNode(Node node, String code) {
        try {
            byte[] sendData = (id + " " + code).getBytes();
            DatagramSocket socket = new DatagramSocket();
            InetAddress address = InetAddress.getByName(node.getHost());
            DatagramPacket sendPacket = new DatagramPacket(
                    sendData,
                    sendData.length,
                    address,
                    node.getPort());
            socket.send(sendPacket);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void listemToCoordiantor() {
        try {
            DatagramSocket serverSocket = new DatagramSocket(mySelf.getPort());

            final byte[] receiveData = new byte[1024];

            while (true) {
                final DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(receivePacket);

                final String receivedMessage = new String(receivePacket.getData(), receivePacket.getOffset(),
                        receivePacket.getLength());

                final String[] strings = receivedMessage.split(" ");
                final String code = strings[1];
                final int id = Integer.parseInt(strings[0]);
                System.out.println("current id " + id);

                switch (code) {
                    case Codes.GRANT:
                        System.out.println("Ganhei o lock");
                        Thread.sleep(2 * 1000);
                        System.out.println("Liberei o lock");
                        sendToNode(currentCoordinator, Codes.RELEASE);
                        hasLock.set(true);
                        break;
                    case Codes.DENIED:
                        System.out.println("Não ganhei o lock");
                        hasLock.set(false);
                        break;

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void coordinatorUnlock(int id) {
        try {
            Thread.sleep(2 * 1000);
            if (lock != null) {
                if (lock.getId() == id) {
                    lock = null;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void send() {
        while (true) {
            try {
                if (hasLock.get()) { continue; }
                int delay = 1;//new Random().nextInt(2);
                Thread.sleep( (2 + delay) * 1000);
                sendToNode(currentCoordinator, Codes.REQ);
                System.out.println("enviando requisição para o coordinador");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }
}
