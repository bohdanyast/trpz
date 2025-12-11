package org.example.webbrowser.p2p;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * P2P Node that acts as both server and client
 */
public class P2PNode {
    private String nodeId;
    private String nodeName;
    private int port;
    private ServerSocket serverSocket;
    private boolean running;

    private Map<String, PeerInfo> connectedPeers;
    private Map<String, PeerConnection> peerConnections;

    private ExecutorService threadPool;
    private List<P2PMessageListener> messageListeners;

    public P2PNode(String nodeName, int port) {
        this.nodeId = UUID.randomUUID().toString();
        this.nodeName = nodeName;
        this.port = port;
        this.connectedPeers = new ConcurrentHashMap<>();
        this.peerConnections = new ConcurrentHashMap<>();
        this.threadPool = Executors.newCachedThreadPool();
        this.messageListeners = new CopyOnWriteArrayList<>();
        this.running = false;
    }

    /**
     * Inner class to store peer connection streams
     */
    private static class PeerConnection {
        Socket socket;
        ObjectOutputStream out;
        ObjectInputStream in;

        PeerConnection(Socket socket) throws IOException {
            this.socket = socket;
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.out.flush();
            this.in = new ObjectInputStream(socket.getInputStream());
        }

        void close() {
            try {
                if (out != null) out.close();
                if (in != null) in.close();
                if (socket != null) socket.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    /**
     * Starts P2P node (server)
     */
    public void start() {
        if (running) {
            System.out.println("[P2PNode] Already running");
            return;
        }

        try {
            serverSocket = new ServerSocket(port);
            running = true;

            System.out.println("[P2PNode] Started on port " + port);
            System.out.println("[P2PNode] Node ID: " + nodeId);
            System.out.println("[P2PNode] Node Name: " + nodeName);

            // Accept incoming connections
            threadPool.execute(this::acceptConnections);

            // Start heartbeat
            threadPool.execute(this::sendHeartbeat);

        } catch (IOException e) {
            System.err.println("[P2PNode] Failed to start: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Accepts incoming peer connections
     */
    private void acceptConnections() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[P2PNode] New connection from: " +
                        clientSocket.getInetAddress().getHostAddress());

                threadPool.execute(() -> handleIncomingPeer(clientSocket));

            } catch (IOException e) {
                if (running) {
                    System.err.println("[P2PNode] Error accepting connection: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Handles incoming peer connection
     */
    private void handleIncomingPeer(Socket socket) {
        PeerConnection conn = null;
        String peerId = null;

        try {
            // Create connection streams
            conn = new PeerConnection(socket);

            System.out.println("[P2PNode] Waiting for CONNECT message...");

            // Wait for CONNECT message
            P2PMessage connectMsg = (P2PMessage) conn.in.readObject();

            if (connectMsg.getType() != P2PMessage.MessageType.CONNECT) {
                System.err.println("[P2PNode] First message must be CONNECT, got: " + connectMsg.getType());
                conn.close();
                return;
            }

            // Extract peer info
            PeerInfo peerInfo = (PeerInfo) connectMsg.getPayload();
            peerId = peerInfo.getPeerId();

            System.out.println("[P2PNode] Peer connected: " + peerInfo.getPeerName() +
                    " (ID: " + peerId + ")");

            // Store connection and peer info
            connectedPeers.put(peerId, peerInfo);
            peerConnections.put(peerId, conn);

            // Notify listeners
            notifyListeners(connectMsg);

            // FIXED: Send acknowledgment back
            P2PMessage ackMsg = new P2PMessage(
                    P2PMessage.MessageType.CONNECT,
                    nodeId,
                    nodeName,
                    new PeerInfo(nodeId, nodeName, getLocalIP(), port)
            );
            sendMessageDirect(conn, ackMsg);

            // Listen for messages from this peer
            listenToPeer(conn, peerId);

        } catch (Exception e) {
            System.err.println("[P2PNode] Error handling incoming peer: " + e.getMessage());
            e.printStackTrace();

            // Cleanup
            if (peerId != null) {
                removePeer(peerId);
            }
            if (conn != null) {
                conn.close();
            }
        }
    }

    /**
     * Connects to another peer
     */
    public boolean connectToPeer(String ipAddress, int port) {
        try {
            System.out.println("[P2PNode] Connecting to " + ipAddress + ":" + port);

            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(ipAddress, port), 5000); // 5 sec timeout

            // Create connection
            PeerConnection conn = new PeerConnection(socket);

            // Send CONNECT message
            P2PMessage connectMsg = new P2PMessage(
                    P2PMessage.MessageType.CONNECT,
                    nodeId,
                    nodeName,
                    new PeerInfo(nodeId, nodeName, getLocalIP(), this.port)
            );

            sendMessageDirect(conn, connectMsg);

            System.out.println("[P2PNode] CONNECT message sent, waiting for response...");

            // Wait for acknowledgment
            P2PMessage ackMsg = (P2PMessage) conn.in.readObject();

            if (ackMsg.getType() == P2PMessage.MessageType.CONNECT) {
                PeerInfo peerInfo = (PeerInfo) ackMsg.getPayload();
                String peerId = peerInfo.getPeerId();

                System.out.println("[P2PNode] Connection acknowledged by: " + peerInfo.getPeerName());

                // Store connection and peer info
                connectedPeers.put(peerId, peerInfo);
                peerConnections.put(peerId, conn);

                // Notify listeners
                notifyListeners(ackMsg);

                // Start listening to this peer
                threadPool.execute(() -> listenToPeer(conn, peerId));

                return true;
            } else {
                System.err.println("[P2PNode] Unexpected response type: " + ackMsg.getType());
                conn.close();
                return false;
            }

        } catch (Exception e) {
            System.err.println("[P2PNode] Failed to connect to " + ipAddress + ":" + port);
            System.err.println("[P2PNode] Error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Listens to messages from a peer
     */
    private void listenToPeer(PeerConnection conn, String peerId) {
        System.out.println("[P2PNode] Started listening to peer: " + peerId);

        try {
            while (running && !conn.socket.isClosed()) {
                System.out.println("[DEBUG] Waiting for message from " + peerId + "...");

                P2PMessage message = (P2PMessage) conn.in.readObject();

                System.out.println("[DEBUG] Message received!");
                System.out.println("[DEBUG] Type: " + message.getType());
                System.out.println("[DEBUG] From: " + message.getSenderName());

                System.out.println("[P2PNode] Received from " + peerId + ": " + message.getType());

                // Update last seen
                PeerInfo peer = connectedPeers.get(peerId);
                if (peer != null) {
                    peer.updateLastSeen();
                }

                // Handle message
                switch (message.getType()) {
                    case DISCONNECT:
                        System.out.println("[P2PNode] Peer disconnecting: " + peerId);
                        removePeer(peerId);
                        return;

                    default:
                        // Notify listeners
                        notifyListeners(message);
                }
            }

        } catch (EOFException e) {
            System.out.println("[P2PNode] Peer disconnected: " + peerId);
        } catch (Exception e) {
            System.err.println("[P2PNode] Error listening to peer " + peerId + ": " + e.getMessage());
        } finally {
            removePeer(peerId);
        }
    }

    /**
     * Sends message directly through connection (with proper flushing)
     */
    private void sendMessageDirect(PeerConnection conn, P2PMessage message) throws IOException {
        synchronized (conn.out) {
            System.out.println("[DEBUG] Sending message: " + message.getType());
            System.out.println("[DEBUG] Socket connected: " + !conn.socket.isClosed());
            System.out.println("[DEBUG] Output stream: " + (conn.out != null));

            conn.out.writeObject(message);
            conn.out.flush();
            conn.out.reset();

            System.out.println("[DEBUG] Message sent successfully");
        }
    }

    /**
     * Sends message to specific peer
     */
    public void sendToPeer(String peerId, P2PMessage message) {
        PeerConnection conn = peerConnections.get(peerId);

        if (conn == null || conn.socket.isClosed()) {
            System.err.println("[P2PNode] No connection to peer: " + peerId);
            removePeer(peerId);
            return;
        }

        try {
            sendMessageDirect(conn, message);
            System.out.println("[P2PNode] Sent to " + peerId + ": " + message.getType());

        } catch (IOException e) {
            System.err.println("[P2PNode] Failed to send to " + peerId + ": " + e.getMessage());
            removePeer(peerId);
        }
    }

    /**
     * Broadcasts message to all connected peers
     */
    public void broadcast(P2PMessage message) {
        int peerCount = connectedPeers.size();

        if (peerCount == 0) {
            System.out.println("[P2PNode] No peers to broadcast to");
            return;
        }

        System.out.println("[P2PNode] Broadcasting " + message.getType() +
                " to " + peerCount + " peer(s)");

        int successCount = 0;

        for (String peerId : new ArrayList<>(connectedPeers.keySet())) {
            try {
                sendToPeer(peerId, message);
                successCount++;
            } catch (Exception e) {
                System.err.println("[P2PNode] Failed to broadcast to " + peerId);
            }
        }

        System.out.println("[P2PNode] Broadcast complete: " + successCount + "/" + peerCount + " successful");
    }

    /**
     * Removes peer from connected list
     */
    private void removePeer(String peerId) {
        connectedPeers.remove(peerId);
        PeerConnection conn = peerConnections.remove(peerId);

        if (conn != null) {
            conn.close();
        }

        System.out.println("[P2PNode] Removed peer: " + peerId);
    }

    /**
     * Sends periodic heartbeat to all peers
     */
    private void sendHeartbeat() {
        while (running) {
            try {
                Thread.sleep(30000); // Every 30 seconds

                // Check peer connectivity
                List<String> deadPeers = new ArrayList<>();
                long now = System.currentTimeMillis();

                for (Map.Entry<String, PeerInfo> entry : connectedPeers.entrySet()) {
                    PeerInfo peer = entry.getValue();

                    // Check if connection is alive
                    PeerConnection conn = peerConnections.get(entry.getKey());
                    if (conn == null || conn.socket.isClosed()) {
                        deadPeers.add(entry.getKey());
                    } else if (now - peer.getLastSeen() > 60000) { // 1 minute timeout
                        deadPeers.add(entry.getKey());
                    }
                }

                // Remove dead peers
                for (String peerId : deadPeers) {
                    System.out.println("[P2PNode] Removing inactive peer: " + peerId);
                    removePeer(peerId);
                }

            } catch (InterruptedException e) {
                break;
            }
        }
    }

    /**
     * Gets local IP address
     */
    private String getLocalIP() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "127.0.0.1";
        }
    }

    /**
     * Stops P2P node
     */
    public void stop() {
        if (!running) {
            return;
        }

        System.out.println("[P2PNode] Stopping...");

        running = false;

        // Send disconnect to all peers
        P2PMessage disconnectMsg = new P2PMessage(
                P2PMessage.MessageType.DISCONNECT,
                nodeId,
                nodeName,
                null
        );

        try {
            broadcast(disconnectMsg);
            Thread.sleep(500); // Give time for messages to send
        } catch (InterruptedException e) {
            // Ignore
        }

        // Close all connections
        for (PeerConnection conn : peerConnections.values()) {
            conn.close();
        }
        peerConnections.clear();
        connectedPeers.clear();

        // Close server socket
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            // Ignore
        }

        threadPool.shutdown();

        System.out.println("[P2PNode] Stopped");
    }

    /**
     * Adds message listener
     */
    public void addMessageListener(P2PMessageListener listener) {
        messageListeners.add(listener);
    }

    /**
     * Notifies all listeners about new message
     */
    /**
     * Notifies all listeners about new message
     */
    private void notifyListeners(P2PMessage message) {
        System.out.println("[DEBUG] Notifying " + messageListeners.size() + " listener(s)");

        for (P2PMessageListener listener : messageListeners) {
            try {
                System.out.println("[DEBUG] Calling listener: " + listener.getClass().getSimpleName());
                listener.onMessageReceived(message);
                System.out.println("[DEBUG] Listener called successfully");
            } catch (Exception e) {
                System.err.println("[P2PNode] Listener error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // Getters
    public String getNodeId() { return nodeId; }
    public String getNodeName() { return nodeName; }
    public int getPort() { return port; }
    public Map<String, PeerInfo> getConnectedPeers() { return new HashMap<>(connectedPeers); }
    public boolean isRunning() { return running; }
}