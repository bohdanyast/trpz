package org.example.webbrowser.p2p;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Message structure for P2P communication
 */
public class P2PMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public enum MessageType {
        DISCOVER,           // Discovery of peers
        CONNECT,            // Connect request
        DISCONNECT,         // Disconnect notification
        SHARE_HISTORY,      // Share browsing history
        SHARE_BOOKMARK,     // Share bookmark
        SHARE_PAGE,         // Share current page
        REQUEST_PAGE,       // Request page from peer
        CHAT_MESSAGE,       // Text message between peers
        SYNC_REQUEST        // Request synchronization
    }
    
    private MessageType type;
    private String senderId;
    private String senderName;
    private Object payload;
    private LocalDateTime timestamp;
    
    public P2PMessage(MessageType type, String senderId, String senderName, Object payload) {
        this.type = type;
        this.senderId = senderId;
        this.senderName = senderName;
        this.payload = payload;
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters and setters
    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }
    
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    
    public Object getPayload() { return payload; }
    public void setPayload(Object payload) { this.payload = payload; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    @Override
    public String toString() {
        return String.format("[%s] %s from %s (%s)", 
            timestamp, type, senderName, senderId);
    }
}