package org.example.webbrowser;

/**
 * Concrete Handler for HTTP 200 OK responses
 * 
 * Handles successful HTTP responses (status code 200).
 * This is typically the final handler in the chain for successful requests.
 */
public class SuccessHandler extends AbstractHTTPHandler {
    
    @Override
    protected boolean canHandle(HTTPResponse response) {
        return response.getStatusCode() == 200;
    }
    
    @Override
    protected void processResponse(HTTPResponse response) {
        System.out.println("[SuccessHandler] Processing HTTP 200 OK response");
        System.out.println("[SuccessHandler] Content length: " + response.getBody().length() + " characters");
        System.out.println("[SuccessHandler] Response handled successfully");
        
        // Mark response as successfully processed
        response.getHeaders().put("X-Handled-By", "SuccessHandler");
    }
}
