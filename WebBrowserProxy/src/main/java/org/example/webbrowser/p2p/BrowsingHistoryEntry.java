package org.example.webbrowser.p2p;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Represents a browsing history entry that can be shared between peers
 */
public class BrowsingHistoryEntry implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String url;
    private String title;
    private LocalDateTime visitTime;
    private String peerId;
    private String peerName;
    
    public BrowsingHistoryEntry(String url, String title, String peerId, String peerName) {
        this.url = url;
        this.title = title;
        this.visitTime = LocalDateTime.now();
        this.peerId = peerId;
        this.peerName = peerName;
    }
    
    // Getters and setters
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public LocalDateTime getVisitTime() { return visitTime; }
    public void setVisitTime(LocalDateTime visitTime) { this.visitTime = visitTime; }
    
    public String getPeerId() { return peerId; }
    public void setPeerId(String peerId) { this.peerId = peerId; }
    
    public String getPeerName() { return peerName; }
    public void setPeerName(String peerName) { this.peerName = peerName; }
    
    @Override
    public String toString() {
        return String.format("[%s] %s visited %s - %s", 
            visitTime, peerName, title, url);
    }
}