package org.example.webbrowser.chain;

import org.example.webbrowser.HTTPResponse;

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
        response.getHeaders().put("X-Handled-By", "SuccessHandler");
    }
}
