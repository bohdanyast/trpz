package org.example.webbrowser.p2p;

import java.io.Serializable;

/**
 * Information about a peer in the network
 */
public class PeerInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String peerId;
    private String peerName;
    private String ipAddress;
    private int port;
    private boolean isOnline;
    private long lastSeen;
    
    public PeerInfo(String peerId, String peerName, String ipAddress, int port) {
        this.peerId = peerId;
        this.peerName = peerName;
        this.ipAddress = ipAddress;
        this.port = port;
        this.isOnline = true;
        this.lastSeen = System.currentTimeMillis();
    }
    
    // Getters and setters
    public String getPeerId() { return peerId; }
    public void setPeerId(String peerId) { this.peerId = peerId; }
    
    public String getPeerName() { return peerName; }
    public void setPeerName(String peerName) { this.peerName = peerName; }
    
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    
    public boolean isOnline() { return isOnline; }
    public void setOnline(boolean online) { isOnline = online; }
    
    public long getLastSeen() { return lastSeen; }
    public void setLastSeen(long lastSeen) { this.lastSeen = lastSeen; }
    
    public void updateLastSeen() {
        this.lastSeen = System.currentTimeMillis();
    }
    
    @Override
    public String toString() {
        return String.format("%s (%s:%d) - %s", 
            peerName, ipAddress, port, isOnline ? "Online" : "Offline");
    }
}