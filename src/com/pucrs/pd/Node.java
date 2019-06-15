package com.pucrs.pd;

import java.util.Scanner;

public class Node {
    private int id;
    private String host;
    private int port;

    public Node(String input) {
        final Scanner scanner = new Scanner(input);
        scanner.useDelimiter(" ");
        id = scanner.nextInt();
        host = scanner.next();
        port = scanner.nextInt();
    }

    public int getId() {
        return id;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return "Node{" +
                "id=" + id +
                ", host='" + host + '\'' +
                ", port=" + port +
                '}';
    }
}
