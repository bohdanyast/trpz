package org.example.webbrowser.p2p;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * P2P Node that acts as both server and client
 * Handles peer-to-peer communication for browser instances
 */
public class P2PNode {
    private String nodeId;
    private String nodeName;
    private int port;
    private ServerSocket serverSocket;
    private boolean running;
    
    private Map<String, PeerInfo> connectedPeers;
    private Map<String, Socket> peerSockets;
    
    private ExecutorService threadPool;
    private List<P2PMessageListener> messageListeners;
    
    public P2PNode(String nodeName, int port) {
        this.nodeId = UUID.randomUUID().toString();
        this.nodeName = nodeName;
        this.port = port;
        this.connectedPeers = new ConcurrentHashMap<>();
        this.peerSockets = new ConcurrentHashMap<>();
        this.threadPool = Executors.newCachedThreadPool();
        this.messageListeners = new CopyOnWriteArrayList<>();
        this.running = false;
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
                
                threadPool.execute(() -> handlePeerConnection(clientSocket));
                
            } catch (IOException e) {
                if (running) {
                    System.err.println("[P2PNode] Error accepting connection: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Handles connection with a peer
     */
    private void handlePeerConnection(Socket socket) {
        try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            
            while (running && !socket.isClosed()) {
                P2PMessage message = (P2PMessage) in.readObject();
                
                System.out.println("[P2PNode] Received: " + message);
                
                // Handle different message types
                switch (message.getType()) {
                    case CONNECT:
                        handleConnectMessage(message, socket);
                        break;
                    case DISCONNECT:
                        handleDisconnectMessage(message);
                        break;
                    case SHARE_HISTORY:
                    case SHARE_BOOKMARK:
                    case SHARE_PAGE:
                    case CHAT_MESSAGE:
                        notifyListeners(message);
                        break;
                    default:
                        System.out.println("[P2PNode] Unknown message type: " + message.getType());
                }
            }
            
        } catch (EOFException e) {
            System.out.println("[P2PNode] Peer disconnected");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[P2PNode] Error handling peer: " + e.getMessage());
        }
    }
    
    /**
     * Connects to another peer
     */
    public boolean connectToPeer(String ipAddress, int port) {
        try {
            Socket socket = new Socket(ipAddress, port);
            
            // Send connection message
            P2PMessage connectMsg = new P2PMessage(
                P2PMessage.MessageType.CONNECT,
                nodeId,
                nodeName,
                new PeerInfo(nodeId, nodeName, getLocalIP(), this.port)
            );
            
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.writeObject(connectMsg);
            out.flush();
            
            System.out.println("[P2PNode] Connected to peer: " + ipAddress + ":" + port);
            
            // Start listening to this peer
            threadPool.execute(() -> handlePeerConnection(socket));
            
            return true;
            
        } catch (IOException e) {
            System.err.println("[P2PNode] Failed to connect to " + ipAddress + ":" + port);
            return false;
        }
    }
    
    /**
     * Sends message to specific peer
     */
    public void sendToPeer(String peerId, P2PMessage message) {
        Socket socket = peerSockets.get(peerId);
        if (socket != null && !socket.isClosed()) {
            try {
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                out.writeObject(message);
                out.flush();
                
                System.out.println("[P2PNode] Sent to " + peerId + ": " + message.getType());
                
            } catch (IOException e) {
                System.err.println("[P2PNode] Failed to send to " + peerId);
                removePeer(peerId);
            }
        }
    }
    
    /**
     * Broadcasts message to all connected peers
     */
    public void broadcast(P2PMessage message) {
        System.out.println("[P2PNode] Broadcasting: " + message.getType() + 
                         " to " + connectedPeers.size() + " peers");
        
        for (String peerId : connectedPeers.keySet()) {
            sendToPeer(peerId, message);
        }
    }
    
    /**
     * Handles CONNECT message
     */
    private void handleConnectMessage(P2PMessage message, Socket socket) {
        PeerInfo peerInfo = (PeerInfo) message.getPayload();
        
        connectedPeers.put(peerInfo.getPeerId(), peerInfo);
        peerSockets.put(peerInfo.getPeerId(), socket);
        
        System.out.println("[P2PNode] Peer connected: " + peerInfo);
        
        notifyListeners(message);
    }
    
    /**
     * Handles DISCONNECT message
     */
    private void handleDisconnectMessage(P2PMessage message) {
        String peerId = message.getSenderId();
        removePeer(peerId);
        
        System.out.println("[P2PNode] Peer disconnected: " + peerId);
        
        notifyListeners(message);
    }
    
    /**
     * Removes peer from connected list
     */
    private void removePeer(String peerId) {
        connectedPeers.remove(peerId);
        Socket socket = peerSockets.remove(peerId);
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                // Ignore
            }
        }
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
                    if (now - peer.getLastSeen() > 60000) { // 1 minute timeout
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
        
        running = false;
        
        // Send disconnect to all peers
        P2PMessage disconnectMsg = new P2PMessage(
            P2PMessage.MessageType.DISCONNECT,
            nodeId,
            nodeName,
            null
        );
        broadcast(disconnectMsg);
        
        // Close all connections
        for (Socket socket : peerSockets.values()) {
            try {
                socket.close();
            } catch (IOException e) {
                // Ignore
            }
        }
        
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
    private void notifyListeners(P2PMessage message) {
        for (P2PMessageListener listener : messageListeners) {
            try {
                listener.onMessageReceived(message);
            } catch (Exception e) {
                System.err.println("[P2PNode] Listener error: " + e.getMessage());
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