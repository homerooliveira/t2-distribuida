    package com.pucrs.pd;

    import java.io.IOException;
    import java.net.DatagramPacket;
    import java.net.DatagramSocket;
    import java.net.InetAddress;
    import java.net.SocketTimeoutException;
    import java.nio.file.Files;
    import java.nio.file.Path;
    import java.nio.file.Paths;
    import java.util.List;
    import java.util.stream.Collectors;

    public class Server {
        private final int id;
        private Node mySelf;
        private List<Node> nodes;
        private boolean isCoordinator;
        private Node lock;
        private Node currentCoordinator;
        private boolean hasLock = false;
        private boolean hasElection = false;
        private boolean waitingElection = false;
        private Node memory = new Node("1 localhost 4010");

        public synchronized Node getLock() {
            return lock;
        }

        public synchronized void setLock(Node lock) {
            this.lock = lock;
        }

        public synchronized boolean getHasLock() {
            return hasLock;
        }

        public synchronized void setHasLock(boolean hasLock) {
            this.hasLock = hasLock;
        }

        public synchronized boolean getHasElection() {
            return hasElection;
        }

        public synchronized void setHasElection(boolean hasElection) {
            this.hasElection = hasElection;
        }

        public synchronized boolean getWaitingElection() {
            return waitingElection;
        }

        public synchronized void setWaitingElection(boolean waitingElection) {
            this.waitingElection = waitingElection;
        }

        public synchronized void setCurrentCoordinator (Node currentCoordinator){
            this.currentCoordinator = currentCoordinator;
        }

        public synchronized Node getCurrentCoordinator () {
            return currentCoordinator;
        }

        public static void main (String[]args){
            if (args.length != 1) {
                System.err.println("[NÚMERO DE ARGUMENTOS INVÁLIDO]");
                return;
            }
            int id = Integer.parseInt(args[0]);
            try {
                new Server(id).run();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public Server( int id){
            this.id = id;
        }

        public void run () throws IOException {
            final Path path = Paths.get("config.txt");
            nodes = Files.lines(path)
                    .map(Node::new)
                    .collect(Collectors.toList());


            isCoordinator = id == nodes.size();
            mySelf = nodes.get(id - 1);
            setCurrentCoordinator(nodes.get(nodes.size() - 1));

            new Thread(this::makeElection).start();
            new Thread(this::send).start();
            new Thread(this::listenToNodes).start();
        }

        private void sendToNode (Node node, String code){
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

        void listenToNodes() {
            try {
                DatagramSocket serverSocket = new DatagramSocket(mySelf.getPort());

                serverSocket.setSoTimeout(5 * 1000);
                final byte[] receiveData = new byte[1024];

                while (true) {
                    try {
                        final DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                        serverSocket.receive(receivePacket);

                        final String receivedMessage = new String(receivePacket.getData(), receivePacket.getOffset(),
                                receivePacket.getLength());

                        final String[] strings = receivedMessage.split(" ");
                        final String code = strings[1];
                        final int id = Integer.parseInt(strings[0]);

                        final Node node = nodes.get(id - 1);

                        switch (code) {
                            case Codes.GRANT:
                                setHasLock(true);
                                System.out.println("[GANHEI LOCK]");
                                sendToNode(memory, " LOCK");
                                new Thread(this::unlock).start();
                                break;
                            case Codes.DENIED:
                                System.out.println("[LOCK NEGADO]");
                                break;
                            case Codes.ELECTION:
                                sendToNode(node, Codes.WAITING_ELECTION);
                                new Thread(this::makeElection).start();
                                break;
                            case Codes.WAITING_ELECTION:
                                System.out.println("[ESPERANDO ELEIÇÃO]");
                                setWaitingElection(true);
                                break;
                            case Codes.END_ELECTION:
                                setHasElection(false);
                                setWaitingElection(false);
                                currentCoordinator = node;
                                isCoordinator = currentCoordinator.getId() == mySelf.getId();
                                break;
                            case Codes.REQ:
                                if (getLock() == null) {
                                    sendToNode(node, Codes.GRANT);
                                    setLock(node);
                                    System.out.println("[" + node.getId() + " GANHOU LOCK]");
                                    new Thread(() -> this.coordinatorUnlock(id)).start();
                                } else {
                                    sendToNode(node, Codes.DENIED);
                                }
                                break;
                            case Codes.RELEASE:
                                if (getLock() != null) {
                                    if (getLock().getId() == id) {
                                        setLock(null);
                                        System.out.println("[" + node.getId() + " LIBEROU LOCK]");
                                    }
                                }
                                break;
                        }
                    } catch (SocketTimeoutException e) {
                        if(!getHasElection()) {
                            new Thread(this::makeElection).start();
                        } else if (!getWaitingElection()) {
                            setHasElection(false);
                            currentCoordinator = mySelf;
                            System.out.println("[SOU O COORDENADOR]");
                            nodes.stream()
                                    .filter(n -> n.getId() != this.id)
                                    .forEach(node -> sendToNode(node, Codes.END_ELECTION));
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        void makeElection() {
        System.out.println("[PEDI ELEIÇÃO]");
            setHasElection(true);
            nodes.stream()
                    .filter(n -> n.getId() > this.id)
                    .forEach(node -> sendToNode(node, Codes.ELECTION));
        }

        void coordinatorUnlock ( int id){
            try {
                Thread.sleep(3 * 1000);
                if (getLock() != null) {
                    if (getLock().getId() == id) {
                        setLock(null);
                        System.out.println("[COOERDENADOR LIBEROU LOCK]");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        void unlock () {
            if (!getHasLock()) return;
            try {
                Thread.sleep(2 * 1000);
                setHasLock(false);
                sendToNode(getCurrentCoordinator(), Codes.RELEASE);
                sendToNode(memory, " RELEASE");
                System.out.println("[LIBEREI LOCK]");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        void send () {
            while (true) {
                try {
                    int delay = 1;//new Random().nextInt(2);
                    Thread.sleep((2 + delay) * 1000);
                    if (getHasLock() || getHasElection()) continue;
                    sendToNode(currentCoordinator, Codes.REQ);
                    System.out.println("[REQUISITEI LOCK]");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }
    }