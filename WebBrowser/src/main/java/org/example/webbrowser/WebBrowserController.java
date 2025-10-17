package org.example.webbrowser;

import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;

import java.net.URL;
import java.util.ResourceBundle;

public class WebBrowserController implements Initializable {
    // JavaFX components and our own components
    @FXML
    private WebView webView;

    @FXML
    private TextField textField;

    private WebEngine webEngine;
    private WebHistory webHistory;

    private Browser browser;
    private AddressBar addressBar;
    private WebPage currentWebPage;

    // Local server for testing 404, 502, 503 mistakes handling
    private WebServer localServer;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        webEngine = webView.getEngine();

        browser = new Browser();
        addressBar = new AddressBar();
        browser.setAddressBar(addressBar);

        initializeLocalServer();

        // Handling page loading (now sout, in future the engine of launching will be changed)
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                onPageLoaded();
            } else if (newState == Worker.State.FAILED) {
                browser.handleError(500);
            }
        });
    }

    /**
     * Initializes local web server for testing 404, 502 and 503 errors
     */
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
            </head>
            <body>
                <h1>Test Server Page</h1>
                <p>This page is served by the local WebServer!</p>
                <img src="images/test.png" alt="Test">
                <script src="js/app.js"></script>
            </body>
            </html>
            """;
        testPage.setRawHTML(testHTML);
        testPage.parseHTML();
        localServer.addPage(testPage);

        WebPage page404 = new WebPage();
        String html404 = """
            <!DOCTYPE html>
            <html>
            <head><title>404 Not Found</title></head>
            <body>
                <h1>404 - Page Not Found</h1>
                <p>The requested resource was not found on this server.</p>
            </body>
            </html>
            """;
        page404.setRawHTML(html404);
        localServer.addPage(page404);

        WebPage page502 = new WebPage();
        String html502 = """
            <!DOCTYPE html>
            <html>
            <head><title>502 Bad Gateway</title></head>
            <body>
                <h1>502 - Bad Gateway</h1>
                <p>The server received an invalid response from the upstream server.</p>
            </body>
            </html>
            """;
        page502.setRawHTML(html502);
        localServer.addPage(page502);

        WebPage page503 = new WebPage();
        String html503 = """
            <!DOCTYPE html>
            <html>
            <head><title>503 Service Unavailable</title></head>
            <body>
                <h1>503 - Service Unavailable</h1>
                <p>The server is temporarily unable to handle the request.</p>
            </body>
            </html>
            """;
        page503.setRawHTML(html503);
        localServer.addPage(page503);

        System.out.println("Local WebServer initialized at: " + localServer.getHost());
        System.out.println("Test URLs:");
        System.out.println("  - test.com/index.html (200 OK)");
        System.out.println("  - test.com/404 (404 Not Found)");
        System.out.println("  - test.com/502 (502 Bad Gateway)");
        System.out.println("  - test.com/503 (503 Service Unavailable)");
    }

    /**
     * Loading a page through WebEngine (for now, further will be own resources)
     */
    public void loadPage() {
        String inputUrl = textField.getText();

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
            webEngine.load(fullUrl);
            browser.loadPage(fullUrl);

        } else {
            browser.handleError(400);
            System.err.println("Invalid URL: " + inputUrl);
        }
    }

    /**
     * Handling local server requests
     * @param url test.com/
     */
    private void handleLocalServerRequest(String url) {
        System.out.println("\nLocal Server Request");

        // Adding protocol if not provided
        String fullUrl = url;
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            fullUrl = "http://" + url;
        }

        HTTPRequest request = new HTTPRequest(fullUrl, "GET");
        System.out.println("Client sending request to: " + fullUrl);

        HTTPResponse response = new HTTPResponse();
        String htmlContent = "";

        // Setting status code and getting needed pages
        if (url.contains("/404")) {
            response.setStatusCode(404);
            WebPage errorPage = localServer.getPages().get(1);
            if (!errorPage.getHtmlResources().isEmpty()) {
                htmlContent = errorPage.getHtmlResources().get(0).getContent();
            } else {
                htmlContent = errorPage.getRawHTML();
            }
            response.setBody(htmlContent);
            response.getHeaders().put("Content-Type", "text/html");
        } else if (url.contains("/502")) {
            response.setStatusCode(502);
            WebPage errorPage = localServer.getPages().get(2);
            if (!errorPage.getHtmlResources().isEmpty()) {
                htmlContent = errorPage.getHtmlResources().get(0).getContent();
            } else {
                htmlContent = errorPage.getRawHTML();
            }
            response.setBody(htmlContent);
            response.getHeaders().put("Content-Type", "text/html");
        } else if (url.contains("/503")) {
            response.setStatusCode(503);
            WebPage errorPage = localServer.getPages().get(3);
            if (!errorPage.getHtmlResources().isEmpty()) {
                htmlContent = errorPage.getHtmlResources().get(0).getContent();
            } else {
                htmlContent = errorPage.getRawHTML();
            }
            response.setBody(htmlContent);
            response.getHeaders().put("Content-Type", "text/html");
        } else {
            response = localServer.processRequest(request);
        }

        localServer.sendResponse(response);

        request.parseResponse(response);

        webEngine.loadContent(response.getBody(), "text/html");

        currentWebPage = new WebPage();
        currentWebPage.setRawHTML(response.getBody());
        currentWebPage.parseHTML();
        browser.displayPage(currentWebPage);

        if (response.getStatusCode() == 200) {
            System.out.println("Page loaded successfully from local server");
        } else {
            browser.handleError(response.getStatusCode());
            System.out.println("Error page displayed with status code: " + response.getStatusCode());
        }
    }

    /**
     * Output overall info in console (for now, in future architecture will be changed)
     */
    public void onPageLoaded() {
        try {
            // Getting html from a webpage
            String htmlContent = (String) webEngine.executeScript("document.documentElement.outerHTML");

            currentWebPage = new WebPage();
            currentWebPage.setRawHTML(htmlContent);
            currentWebPage.parseHTML();

            browser.displayPage(currentWebPage);

            // Debugging
            System.out.println("\n=== Page Loaded: " + webEngine.getLocation() + " ===");
            System.out.println("HTML files: " + currentWebPage.getHtmlResources().size());
            System.out.println("CSS files: " + currentWebPage.getCssResources().size());
            System.out.println("JS files: " + currentWebPage.getJsResources().size());
            System.out.println("Images: " + currentWebPage.getImageResources().size());

            if (!currentWebPage.getCssResources().isEmpty()) {
                System.out.println("\nCSS Resources:");
                for (CSSFile css : currentWebPage.getCssResources()) {
                    System.out.println("  - " + css.getFileName());
                }
            }

            if (!currentWebPage.getJsResources().isEmpty()) {
                System.out.println("\nJS Resources:");
                for (JSFile js : currentWebPage.getJsResources()) {
                    System.out.println("  - " + js.getFileName());
                }
            }

        } catch (Exception e) {
            System.err.println("Error parsing page: " + e.getMessage());
        }
    }

    public void goBack() {
        webHistory = webEngine.getHistory();
        ObservableList<WebHistory.Entry> entries = webHistory.getEntries();

        if (webHistory.getCurrentIndex() > 0) {
            webHistory.go(-1);
            String currentUrl = entries.get(webHistory.getCurrentIndex()).getUrl();
            textField.setText(currentUrl);
            addressBar.setUrl(currentUrl);
        }
    }

    public void goForward() {
        webHistory = webEngine.getHistory();
        ObservableList<WebHistory.Entry> entries = webHistory.getEntries();

        if (webHistory.getCurrentIndex() < entries.size() - 1) {
            webHistory.go(1);
            String currentUrl = entries.get(webHistory.getCurrentIndex()).getUrl();
            textField.setText(currentUrl);
            addressBar.setUrl(currentUrl);
        }
    }

    public void reload() {
        webEngine.reload();
    }
}