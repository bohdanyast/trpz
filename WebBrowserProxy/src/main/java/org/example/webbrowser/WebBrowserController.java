package org.example.webbrowser;

import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.control.ListView;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import org.example.webbrowser.chain.*;
import org.example.webbrowser.p2p.*;
import org.example.webbrowser.factory_template.*;
import org.example.webbrowser.visitor.*;
import org.example.webbrowser.proxy.*;

import java.net.URL;
import java.util.ResourceBundle;

public class WebBrowserController implements Initializable {
    @FXML
    private WebView webView;

    @FXML
    private TextField textField;

    // P2P UI Components (add to FXML if you want)
    @FXML
    private TextArea p2pLogArea;

    @FXML
    private ListView<String> peerListView;

    @FXML
    private TextField peerIpField;

    @FXML
    private TextField peerPortField;

    private WebEngine webEngine;
    private WebHistory webHistory;

    private Browser browser;
    private AddressBar addressBar;
    private WebPage currentWebPage;

    // Local web server for testing
    private WebServer localServer;

    // Chain of Responsibility for HTTP response handling
    private HTTPHandlerChain handlerChain;

    // Visitor Pattern: Size calculator visitor
    private ResourceSizeCalculatorVisitor sizeCalculator;

    // P2P Node for peer-to-peer communication
    private P2PNode p2pNode;
    private String myNodeName;
    private int myP2PPort = 9000; // Default P2P port

    @FXML
    private Label p2pStatusLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private Label pageInfoLabel;

    @FXML
    private Label peerCountLabel;

    @FXML
    private TextField chatTextField;

    /**
     * Updates status bar labels
     */
    private void updateStatusBar() {
        if (p2pNode != null && p2pNode.isRunning()) {
            if (p2pStatusLabel != null) {
                int peerCount = p2pNode.getConnectedPeers().size();
                p2pStatusLabel.setText("P2P: Online (Port " + myP2PPort + ")");
                p2pStatusLabel.setStyle("-fx-text-fill: green;");

                if (peerCountLabel != null) {
                    peerCountLabel.setText(peerCount + " peer" + (peerCount != 1 ? "s" : ""));
                }
            }
        }

        if (currentWebPage != null && pageInfoLabel != null) {
            int totalResources = currentWebPage.getAllResources().size();
            pageInfoLabel.setText(totalResources + " resources");
        }
    }

    /**
     * Sends chat message from text field
     */
    @FXML
    public void sendChatFromTextField() {
        if (chatTextField == null) return;

        String message = chatTextField.getText().trim();
        if (!message.isEmpty()) {
            sendChatMessage(message);
            chatTextField.clear();
        }
    }


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        webEngine = webView.getEngine();

        // Initialize new architecture
        browser = new Browser();
        addressBar = new AddressBar();
        browser.setAddressBar(addressBar);

        // Initialize Chain of Responsibility for HTTP handling
        handlerChain = new HTTPHandlerChain();

        // Initialize Visitor for resource size calculation
        sizeCalculator = new ResourceSizeCalculatorVisitor();

        // Initialize P2P Node
        initializeP2P();

        // Initialize local test web server
        initializeLocalServer();

        // Listener for page loading
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                onPageLoaded();
                injectLinkHandler();
            } else if (newState == Worker.State.FAILED) {
                System.err.println("Page loading failed");
                browser.handleError(500);
            }
        });

        // Listen for location changes (navigation)
        webEngine.locationProperty().addListener((obs, oldLocation, newLocation) -> {
            if (newLocation != null && !newLocation.isEmpty()) {
                // Update text field with current URL
                textField.setText(extractOriginalUrl(newLocation));

                // Share with peers that we visited this page
                shareCurrentPageWithPeers();
            }
        });

        loadPage();

        // Start periodic status bar update
        javafx.animation.Timeline statusUpdater = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(
                        javafx.util.Duration.seconds(2),
                        event -> updateStatusBar()
                )
        );
        statusUpdater.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        statusUpdater.play();
    }

    /**
     * Initializes P2P node for peer-to-peer communication
     */
    private void initializeP2P() {
        myNodeName = "Browser-" + System.getProperty("user.name");
        int port = 9000 + (int) (Math.random() * 1000);

        try {
            p2pNode = new P2PNode(myNodeName, port);
            p2pNode.start();
            myP2PPort = port;

            System.out.println("\n========================================");
            System.out.println("   P2P NODE INITIALIZED");
            System.out.println("========================================");
            System.out.println("Node Name: " + myNodeName);
            System.out.println("Node ID: " + p2pNode.getNodeId());
            System.out.println("Port: " + myP2PPort);
            System.out.println("========================================\n");

            p2pNode.addMessageListener(this::handleP2PMessage);
            System.out.println("P2P message listener registered");

            // Update UI if exists
            if (p2pLogArea != null) {
                appendP2PLog("P2P Node started on port " + myP2PPort);
            }
        } catch (Exception e) {
            System.err.println("Failed to start P2P on port " + port + ", trying next...");
        }
    }

    /**
     * Handles incoming P2P messages
     * This method is called by P2PNode when message arrives
     */
    private void handleP2PMessage(P2PMessage message) {
//        System.out.println("\n========================================");
//        System.out.println("[P2P Message Received in Controller]");
//        System.out.println("From: " + message.getSenderName());
//        System.out.println("Type: " + message.getType());
//        System.out.println("Time: " + message.getTimestamp());
//        System.out.println("========================================");

        switch (message.getType()) {
            case CONNECT:
                handlePeerConnect(message);
                break;

            case DISCONNECT:
                handlePeerDisconnect(message);
                break;

            case SHARE_HISTORY:
                handleSharedHistory(message);
                break;

            case SHARE_BOOKMARK:
                handleSharedBookmark(message);
                break;

            case SHARE_PAGE:
                handleSharedPage(message);
                break;

            case CHAT_MESSAGE:
                handleChatMessage(message);
                break;

            default:
                System.out.println("Unknown message type: " + message.getType());
        }

        // Update UI
        if (p2pLogArea != null) {
            appendP2PLog(message.getType() + " from " + message.getSenderName());
        }
    }

    /**
     * Handles peer connection
     */
    private void handlePeerConnect(P2PMessage message) {
        PeerInfo peerInfo = (PeerInfo) message.getPayload();
        System.out.println("Peer connected: " + peerInfo);

        appendP2PLog("Peer connected: " + peerInfo.getPeerName());
        updatePeerList();
    }

    /**
     * Handles peer disconnection
     */
    private void handlePeerDisconnect(P2PMessage message) {
        System.out.println("Peer disconnected: " + message.getSenderName());

        appendP2PLog("Peer disconnected: " + message.getSenderName());
        updatePeerList();
    }

    /**
     * Handles shared browsing history
     */
    private void handleSharedHistory(P2PMessage message) {
        BrowsingHistoryEntry entry = (BrowsingHistoryEntry) message.getPayload();

        System.out.println("Received shared history:");
        System.out.println("  URL: " + entry.getUrl());
        System.out.println("  Title: " + entry.getTitle());
        System.out.println("  From: " + entry.getPeerName());
        System.out.println("  Time: " + entry.getVisitTime());

        appendP2PLog("Shared history: " + entry.getTitle() + " from " + entry.getPeerName());
    }

    /**
     * Handles shared bookmark
     */
    private void handleSharedBookmark(P2PMessage message) {
        BrowsingHistoryEntry bookmark = (BrowsingHistoryEntry) message.getPayload();

        System.out.println("Received shared bookmark:");
        System.out.println("  URL: " + bookmark.getUrl());
        System.out.println("  Title: " + bookmark.getTitle());
        System.out.println("  From: " + bookmark.getPeerName());

        appendP2PLog("Shared bookmark: " + bookmark.getTitle());
    }

    /**
     * Handles shared page
     */
    private void handleSharedPage(P2PMessage message) {
        String sharedUrl = (String) message.getPayload();

        System.out.println("Received shared page: " + sharedUrl);
        System.out.println("From: " + message.getSenderName());

        appendP2PLog(message.getSenderName() + " shared: " + sharedUrl);

        // Ask user if they want to open it
        // For now, just log it
    }

    /**
     * Handles chat message
     */
    private void handleChatMessage(P2PMessage message) {
        System.out.println("[DEBUG] handleChatMessage called"); // DEBUG

        String chatText = (String) message.getPayload();

        System.out.println("Chat from " + message.getSenderName() + ": " + chatText);

        appendP2PLog(message.getSenderName() + ": " + chatText);
    }

    /**
     * Shares current page with all peers
     */
    private void shareCurrentPageWithPeers() {
        if (p2pNode == null || !p2pNode.isRunning()) {
            return;
        }

        String currentUrl = addressBar.getUrl();
        if (currentUrl == null || currentUrl.isEmpty() || currentUrl.contains("test.com")) {
            return; // Don't share test.com or empty URLs
        }

        try {
            // Get page title
            String title = (String) webEngine.executeScript("document.title");
            if (title == null || title.isEmpty()) {
                title = currentUrl;
            }

            // Create history entry
            BrowsingHistoryEntry entry = new BrowsingHistoryEntry(
                    currentUrl,
                    title,
                    p2pNode.getNodeId(),
                    p2pNode.getNodeName()
            );

            // Create message
            P2PMessage message = new P2PMessage(
                    P2PMessage.MessageType.SHARE_HISTORY,
                    p2pNode.getNodeId(),
                    p2pNode.getNodeName(),
                    entry
            );

            // Broadcast to all peers
            p2pNode.broadcast(message);

            System.out.println("Shared page with peers: " + title);

        } catch (Exception e) {
            System.err.println("Failed to share page: " + e.getMessage());
        }
    }

    /**
     * Connects to a peer manually
     */
    @FXML
    public void connectToPeer() {
        if (p2pNode == null || !p2pNode.isRunning()) {
            appendP2PLog("P2P node is not running!");
            return;
        }

        String peerIp = peerIpField != null ? peerIpField.getText() : "127.0.0.1";
        String peerPortStr = peerPortField != null ? peerPortField.getText() : "9001";

        try {
            int peerPort = Integer.parseInt(peerPortStr);

            System.out.println("Connecting to peer: " + peerIp + ":" + peerPort);
            appendP2PLog("Connecting to " + peerIp + ":" + peerPort + "...");

            boolean success = p2pNode.connectToPeer(peerIp, peerPort);

            if (success) {
                appendP2PLog("Successfully connected to peer!");
                updatePeerList();
            } else {
                appendP2PLog("Failed to connect to peer");
            }

        } catch (NumberFormatException e) {
            appendP2PLog("Invalid port number!");
        }
    }

    /**
     * Broadcasts current page to all peers
     */
    @FXML
    public void broadcastCurrentPage() {
        if (p2pNode == null || !p2pNode.isRunning()) {
            appendP2PLog("P2P node is not running!");
            return;
        }

        String currentUrl = addressBar.getUrl();
        if (currentUrl == null || currentUrl.isEmpty()) {
            return;
        }

        P2PMessage message = new P2PMessage(
                P2PMessage.MessageType.SHARE_PAGE,
                p2pNode.getNodeId(),
                p2pNode.getNodeName(),
                currentUrl
        );

        p2pNode.broadcast(message);

        appendP2PLog("Broadcasted current page to all peers");
    }

    /**
     * Sends chat message to all peers
     */
    @FXML
    public void sendChatMessage(String message) {
        if (p2pNode == null || !p2pNode.isRunning()) {
            return;
        }

        P2PMessage chatMsg = new P2PMessage(
                P2PMessage.MessageType.CHAT_MESSAGE,
                p2pNode.getNodeId(),
                p2pNode.getNodeName(),
                message
        );

        p2pNode.broadcast(chatMsg);

        appendP2PLog("Me: " + message);
    }

    /**
     * Updates peer list in UI
     */
    private void updatePeerList() {
        if (peerListView == null || p2pNode == null) {
            return;
        }

        javafx.application.Platform.runLater(() -> {
            peerListView.getItems().clear();

            for (PeerInfo peer : p2pNode.getConnectedPeers().values()) {
                peerListView.getItems().add(peer.getPeerName() + " (" +
                        peer.getIpAddress() + ":" +
                        peer.getPort() + ")");
            }
        });
    }

    /**
     * Appends log message to P2P log area
     * MUST run on JavaFX thread
     */
    private void appendP2PLog(String message) {
        System.out.println("[DEBUG] appendP2PLog called: " + message); // DEBUG

        if (p2pLogArea == null) {
            System.out.println("[DEBUG] p2pLogArea is null!"); // DEBUG
            return;
        }

        javafx.application.Platform.runLater(() -> {
            try {
                String timestamp = java.time.LocalTime.now().toString();
                p2pLogArea.appendText("[" + timestamp + "] " + message + "\n");
                System.out.println("[DEBUG] Message added to log area"); // DEBUG
            } catch (Exception e) {
                System.err.println("[DEBUG] Error updating log area: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * Gets P2P connection info
     */
    public String getP2PInfo() {
        if (p2pNode == null || !p2pNode.isRunning()) {
            return "P2P: Offline";
        }

        return String.format("P2P: %s on port %d (%d peers)",
                myNodeName,
                myP2PPort,
                p2pNode.getConnectedPeers().size());
    }

    /**
     * Injects JavaScript to handle link clicks
     */
    private void injectLinkHandler() {
        try {
            JSObject window = (JSObject) webEngine.executeScript("window");
            window.setMember("javaController", this);

            String script = """
                (function() {
                    if (window.linkHandlerInstalled) return;
                    window.linkHandlerInstalled = true;
                    
                    document.addEventListener('click', function(e) {
                        var target = e.target;
                        while (target && target.tagName !== 'A') {
                            target = target.parentElement;
                        }
                        
                        if (target && target.tagName === 'A') {
                            var href = target.getAttribute('href');
                            
                            if (!href || 
                                href.startsWith('#') || 
                                href.startsWith('javascript:') ||
                                href.startsWith('mailto:') ||
                                href.startsWith('tel:')) {
                                return;
                            }
                            
                            if (target.getAttribute('target') === '_blank' || 
                                e.ctrlKey || e.metaKey) {
                                return;
                            }
                            
                            e.preventDefault();
                            e.stopPropagation();
                            
                            console.log('Navigating to: ' + href);
                            window.javaController.handleLinkClick(href);
                        }
                    }, true);
                    
                    console.log('Link handler installed successfully');
                })();
                """;

            webEngine.executeScript(script);
            System.out.println("Link handler injected successfully");

        } catch (Exception e) {
            System.err.println("Failed to inject link handler: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void handleLinkClick(String href) {
        System.out.println("\n=== Link Clicked ===");
        System.out.println("Link: " + href);

        try {
            String currentUrl = webEngine.getLocation();
            String absoluteUrl = resolveUrl(currentUrl, href);
            System.out.println("Resolved to: " + absoluteUrl);

            textField.setText(extractOriginalUrl(absoluteUrl));
            navigateToUrl(absoluteUrl);

        } catch (Exception e) {
            System.err.println("Error handling link click: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String resolveUrl(String baseUrl, String relativeUrl) {
        try {
            if (relativeUrl.startsWith("http://") || relativeUrl.startsWith("https://")) {
                return relativeUrl;
            }

            if (relativeUrl.startsWith("//")) {
                java.net.URL base = new java.net.URL(baseUrl);
                return base.getProtocol() + ":" + relativeUrl;
            }

            if (baseUrl.startsWith("file://")) {
                String originalUrl = extractOriginalUrl(baseUrl);
                java.net.URL base = new java.net.URL(originalUrl);
                java.net.URL resolved = new java.net.URL(base, relativeUrl);
                return resolved.toString();
            }

            java.net.URL base = new java.net.URL(baseUrl);
            java.net.URL resolved = new java.net.URL(base, relativeUrl);
            return resolved.toString();

        } catch (Exception e) {
            System.err.println("Failed to resolve URL: " + relativeUrl);
            return relativeUrl;
        }
    }

    private String extractOriginalUrl(String fileUrl) {
        if (!fileUrl.startsWith("file://")) {
            return fileUrl;
        }

        try {
            String path = fileUrl;
            int cacheIndex = path.indexOf("browser_cache");
            if (cacheIndex != -1) {
                String afterCache = path.substring(cacheIndex + "browser_cache".length() + 1);
                String domain = afterCache.split("/")[0];
                domain = domain.replace("_", ".");
                return "https://" + domain;
            }
        } catch (Exception e) {
            System.err.println("Failed to extract original URL from: " + fileUrl);
        }

        return fileUrl;
    }

    private void navigateToUrl(String url) {
        addressBar.setUrl(url);

        if (url.contains("test.com")) {
            handleLocalServerRequest(url);
        } else {
            loadRealWebsite(url);
        }
    }

    private void initializeLocalServer() {
        localServer = new WebServer("test.com");
        localServer.addResource("index.html");

        WebPage testPage = new WebPage();
        String testHTML = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Test Server - Success</title>
                <link rel="stylesheet" href="styles/main.css">
                <link rel="stylesheet" href="styles/theme.css">
                <style>
                    body { 
                        font-family: Arial; 
                        padding: 50px; 
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); 
                        color: white; 
                    }
                    h1 { font-size: 48px; }
                    p { font-size: 20px; }
                    a { color: #FFD700; text-decoration: underline; }
                    a:hover { color: #FFA500; }
                </style>
            </head>
            <body>
                <h1>Test Server Page</h1>
                <p>This page is served by the local WebServer!</p>
                <p>Status: 200 OK</p>
                <p>Handler: SuccessHandler (Chain of Responsibility)</p>
                <hr>
                <h2>Test Links:</h2>
                <img src="images/test.png" alt="Test">
                <img src="images/banner.jpg" alt="Banner">
                <p><a href="test.com/404">Test 404 Error</a></p>
                <p><a href="test.com/502">Test 502 Error</a></p>
                <p><a href="test.com/503">Test 503 Error</a></p>
                <p><a href="https://example.com">Go to Example.com</a></p>
                <script src="js/app.js"></script>
                <script src="js/utils.js"></script>
            </body>
            </html>
            """;
        testPage.setRawHTML(testHTML);
        testPage.parseHTML();
        localServer.addPage(testPage);

        System.out.println("Local WebServer initialized at: " + localServer.getHost());
        System.out.println("Test URLs:");
        System.out.println("  - test.com/index.html (200 OK)");
        System.out.println("  - test.com/404 (404 Not Found)");
        System.out.println("  - test.com/502 (502 Bad Gateway)");
        System.out.println("  - test.com/503 (503 Service Unavailable)");
        System.out.println("\nReal websites: Enter any URL (e.g., example.com)");
    }

    public void loadPage() {
        String inputUrl = textField.getText();

        if (inputUrl == null || inputUrl.trim().isEmpty()) {
            return;
        }

        if (inputUrl.contains("test.com")) {
            handleLocalServerRequest(inputUrl);
            return;
        }

        if (addressBar.validateURL(inputUrl)) {
            String fullUrl = inputUrl;
            if (!inputUrl.startsWith("http://") && !inputUrl.startsWith("https://")) {
                fullUrl = addressBar.getProtocol() + "://" + inputUrl;
            }

            addressBar.setUrl(fullUrl);
            loadRealWebsite(fullUrl);

        } else {
            browser.handleError(400);
            System.err.println("Invalid URL: " + inputUrl);
        }
    }

    private void loadRealWebsite(String url) {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("LOADING WEBSITE: " + url);
        System.out.println("=".repeat(50));

        try {
            HTTPRequest request = new HTTPRequest(url, "GET");
            HTTPResponse response = request.sendRequest();

            handlerChain.process(response);

            String fileUrl = response.getHeaders().get("X-File-URL");
            if (fileUrl != null) {
                webEngine.load(fileUrl);
            } else {
                webEngine.loadContent(response.getBody(), "text/html");
            }

            currentWebPage = new WebPage();
            currentWebPage.setRawHTML(response.getBody());
            currentWebPage.parseHTML();

            // VISITOR PATTERN: Calculate size of loaded page
            if (response.getStatusCode() == 200) {
                System.out.println("\n[WebBrowserController] Real website loaded, calculating size...");
                calculatePageSize();
            }

        } catch (Exception e) {
            browser.handleError(500);
        }
    }

    private void handleLocalServerRequest(String url) {
        System.out.println("\n=== Local Server Request ===");

        String fullUrl = url;
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            fullUrl = "http://" + url;
        }

        HTTPRequest request = new HTTPRequest(fullUrl, "GET");
        System.out.println("Client sending request to: " + fullUrl);

        HTTPResponse response;

        if (url.contains("/404")) {
            response = new HTTPResponse();
            response.setStatusCode(404);
            response.setBody("");
            response.getHeaders().put("Content-Type", "text/html");

        } else if (url.contains("/502")) {
            response = new HTTPResponse();
            response.setStatusCode(502);
            response.setBody("");
            response.getHeaders().put("Content-Type", "text/html");

        } else if (url.contains("/503")) {
            response = new HTTPResponse();
            response.setStatusCode(503);
            response.setBody("");
            response.getHeaders().put("Content-Type", "text/html");
            response.getHeaders().put("Retry-After", "30");

        } else {
            response = localServer.processRequest(request);
        }
        handlerChain.process(response);

        String displayContent = response.getBody();
        if (response.getStatusCode() == 200) {
            displayContent = replaceImagesWithProxies(displayContent);
        }

        webEngine.loadContent(displayContent, "text/html");

        currentWebPage = new WebPage();
        currentWebPage.setRawHTML(response.getBody());
        currentWebPage.parseHTML();

        // VISITOR PATTERN IN ACTION!
        if (response.getStatusCode() == 200) {
            calculatePageSize();
        }

        if (response.getStatusCode() == 200) {
            System.out.println("Page loaded successfully from local server");
            System.out.println("Handler used: " + response.getHeaders().get("X-Handled-By"));
        } else {
            System.out.println("Error page displayed with status code: " + response.getStatusCode());
            System.out.println("Handler used: " + response.getHeaders().get("X-Handled-By"));
            System.out.println("Error type: " + response.getHeaders().get("X-Error-Type"));
            browser.handleError(response.getStatusCode());
        }
    }

    /**
     * VISITOR PATTERN: Calculates page size using ResourceSizeCalculatorVisitor
     */
    private void calculatePageSize() {
        if (currentWebPage == null) {
            System.out.println("No page to calculate size for");
            return;
        }

        System.out.println("\n========================================");
        System.out.println("   VISITOR PATTERN: SIZE CALCULATION");
        System.out.println("========================================");

        sizeCalculator.reset();
        currentWebPage.acceptVisitor(sizeCalculator);
        sizeCalculator.printReport();

        System.out.println("Quick Stats:");
        System.out.println("  Total resources: " +
                (sizeCalculator.getHtmlCount() +
                        sizeCalculator.getCssCount() +
                        sizeCalculator.getJsCount() +
                        sizeCalculator.getImageCount()));
        System.out.println("  Total size: " + formatBytes(sizeCalculator.getTotalSize()));
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        }
    }

    private String replaceImagesWithProxies(String html) {
        if (html == null || html.isEmpty()) {
            return html;
        }

        java.util.regex.Pattern imgPattern = java.util.regex.Pattern.compile(
                "<img([^>]*?)src=[\"']([^\"']*)[\"']([^>]*?)>",
                java.util.regex.Pattern.CASE_INSENSITIVE
        );

        java.util.regex.Matcher matcher = imgPattern.matcher(html);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String beforeSrc = matcher.group(1);
            String originalSrc = matcher.group(2);
            String afterSrc = matcher.group(3);

            String fileName = originalSrc;
            int lastSlash = originalSrc.lastIndexOf('/');
            if (lastSlash >= 0) {
                fileName = originalSrc.substring(lastSlash + 1);
            }

            ImageProxy proxy = new ImageProxy(fileName, originalSrc);
            String placeholderDataURI = proxy.createPlaceholder();

            String replacement = "<img" + beforeSrc +
                    "src=\"" + placeholderDataURI + "\" " +
                    "data-original-src=\"" + originalSrc + "\"" +
                    afterSrc + ">";

            matcher.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    private void onPageLoaded() {
        try {
            String htmlContent = (String) webEngine.executeScript("document.documentElement.outerHTML");

            if (currentWebPage == null) {
                currentWebPage = new WebPage();
            }

            currentWebPage.setRawHTML(htmlContent);
            currentWebPage.parseHTML();

            // VISITOR PATTERN: Calculate size after page loaded
            System.out.println("\n[WebBrowserController] Page loaded via WebEngine, calculating size...");
            calculatePageSize();

        } catch (Exception e) {
            System.err.println("Error in onPageLoaded: " + e.getMessage());
        }
    }

    public void reload() {
        String currentUrl = addressBar.getUrl();
        if (currentUrl != null && !currentUrl.isEmpty()) {
            textField.setText(currentUrl);
            loadPage();
        } else {
            webEngine.reload();
        }
    }

    public Browser getBrowser() {
        return browser;
    }
}