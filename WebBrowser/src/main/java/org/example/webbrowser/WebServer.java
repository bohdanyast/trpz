package org.example.webbrowser;

import java.util.ArrayList;
import java.util.List;

/**
 * For now handles local server
 */
public class WebServer {
    private String host;
    private List<String> resources;
    private List<WebPage> pages;
    
    public WebServer(String host) {
        this.host = host;
        this.resources = new ArrayList<>();
        this.pages = new ArrayList<>();
    }
    
    public String getHost() {
        return host;
    }
    
    public void setHost(String host) {
        this.host = host;
    }
    
    public List<String> getResources() {
        return resources;
    }
    
    public void addResource(String resource) {
        this.resources.add(resource);
    }
    
    public List<WebPage> getPages() {
        return pages;
    }
    
    public void addPage(WebPage page) {
        this.pages.add(page);
    }
    

    public HTTPResponse processRequest(HTTPRequest request) {
        HTTPResponse response = new HTTPResponse();
        
        String requestedUrl = request.getUrl();

        boolean resourceFound = false;
        WebPage foundPage = null;
        
        for (String resource : resources) {
            if (requestedUrl.contains(resource)) {
                resourceFound = true;
                break;
            }
        }
        
        // Searching for a needed page
        if (resourceFound && !pages.isEmpty()) {
            foundPage = pages.get(0);
        }
        
        // Answer forming
        if (foundPage != null) {
            response.setStatusCode(200);
            response.getHeaders().put("Content-Type", "text/html");
            response.getHeaders().put("Server", host);
            
            StringBuilder htmlContent = new StringBuilder();
            for (HTMLFile html : foundPage.getHtmlResources()) {
                htmlContent.append(html.getContent());
            }
            response.setBody(htmlContent.toString());
        } else {
            response.setStatusCode(404);
            response.getHeaders().put("Content-Type", "text/html");
            response.setBody("<html><body><h1>404 - Page Not Found</h1></body></html>");
        }
        
        return response;
    }

    public void sendResponse(HTTPResponse response) {
        System.out.println("Sending response from " + host);
        System.out.println("Status Code: " + response.getStatusCode());
        System.out.println("Headers: " + response.getHeaders());
        System.out.println("Body length: " + response.getBody().length() + " characters");
    }
}