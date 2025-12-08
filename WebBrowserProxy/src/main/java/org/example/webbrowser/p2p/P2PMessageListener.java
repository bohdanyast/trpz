package org.example.webbrowser.p2p;

/**
 * Listener interface for P2P messages
 */
public interface P2PMessageListener {
    /**
     * Called when a P2P message is received
     * 
     * @param message Received message
     */
    void onMessageReceived(P2PMessage message);
}