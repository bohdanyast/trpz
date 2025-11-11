// ========== HTTPResponseHandler.java (Interface) ==========
package org.example.webbrowser;

/**
 * Chain of Responsibility Pattern: Handler Interface
 * 
 * This interface defines the contract for HTTP response handlers.
 * Each handler in the chain can process a specific HTTP status code
 * or pass the request to the next handler in the chain.
 */
public interface HTTPResponseHandler {
    
    /**
     * Sets the next handler in the chain
     * 
     * @param next The next handler to call if this handler cannot process the response
     */
    void setNext(HTTPResponseHandler next);
    
    /**
     * Handles the HTTP response
     * Each handler checks if it can process the given status code.
     * If yes, it handles the response and returns true.
     * If no, it passes the response to the next handler in the chain.
     * 
     * @param response The HTTP response to handle
     * @return true if the response was handled, false otherwise
     */
    boolean handle(HTTPResponse response);
}

// ========== AbstractHTTPHandler.java ==========
package org.example.webbrowser;

/**
 * Chain of Responsibility Pattern: Abstract Handler
 * 
 * Base class for all HTTP response handlers.
 * Implements the chain linking logic and provides a template for concrete handlers.
 */
public abstract class AbstractHTTPHandler implements HTTPResponseHandler {
    
    protected HTTPResponseHandler nextHandler;
    
    @Override
    public void setNext(HTTPResponseHandler next) {
        this.nextHandler = next;
    }
    
    @Override
    public boolean handle(HTTPResponse response) {
        if (canHandle(response)) {
            processResponse(response);
            return true;
        } else if (nextHandler != null) {
            return nextHandler.handle(response);
        }
        return false;
    }
    
    /**
     * Determines if this handler can process the given response
     * 
     * @param response The HTTP response
     * @return true if this handler can process the response
     */
    protected abstract boolean canHandle(HTTPResponse response);
    
    /**
     * Processes the HTTP response
     * This method is called only if canHandle() returns true
     * 
     * @param response The HTTP response to process
     */
    protected abstract void processResponse(HTTPResponse response);
}

// ========== SuccessHandler.java (200 OK) ==========
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

// ========== NotFoundHandler.java (404) ==========
package org.example.webbrowser;

/**
 * Concrete Handler for HTTP 404 Not Found responses
 * 
 * Handles cases where the requested resource does not exist on the server.
 * This is a client error (4xx series).
 */
public class NotFoundHandler extends AbstractHTTPHandler {
    
    @Override
    protected boolean canHandle(HTTPResponse response) {
        return response.getStatusCode() == 404;
    }
    
    @Override
    protected void processResponse(HTTPResponse response) {
        System.out.println("[NotFoundHandler] Processing HTTP 404 Not Found response");
        System.out.println("[NotFoundHandler] The requested resource was not found on the server");
        System.out.println("[NotFoundHandler] Displaying error page to user");
        
        // Enhance error response with additional information
        if (!response.getBody().contains("404")) {
            String errorPage = generateErrorPage();
            response.setBody(errorPage);
        }
        
        response.getHeaders().put("X-Handled-By", "NotFoundHandler");
        response.getHeaders().put("X-Error-Type", "Client Error");
    }
    
    /**
     * Generates a user-friendly 404 error page
     * 
     * @return HTML content for 404 error page
     */
    private String generateErrorPage() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>404 - Not Found</title>
                <style>
                    body { font-family: Arial; text-align: center; padding: 50px; }
                    h1 { color: #e74c3c; }
                </style>
            </head>
            <body>
                <h1>404 - Page Not Found</h1>
                <p>The page you are looking for does not exist.</p>
            </body>
            </html>
            """;
    }
}

// ========== BadGatewayHandler.java (502) ==========
package org.example.webbrowser;

/**
 * Concrete Handler for HTTP 502 Bad Gateway responses
 * 
 * Handles cases where the server received an invalid response from an upstream server.
 * This is a server error (5xx series).
 */
public class BadGatewayHandler extends AbstractHTTPHandler {
    
    @Override
    protected boolean canHandle(HTTPResponse response) {
        return response.getStatusCode() == 502;
    }
    
    @Override
    protected void processResponse(HTTPResponse response) {
        System.out.println("[BadGatewayHandler] Processing HTTP 502 Bad Gateway response");
        System.out.println("[BadGatewayHandler] The server received an invalid response from upstream");
        System.out.println("[BadGatewayHandler] This is typically a temporary issue");
        
        // Enhance error response
        if (!response.getBody().contains("502")) {
            String errorPage = generateErrorPage();
            response.setBody(errorPage);
        }
        
        response.getHeaders().put("X-Handled-By", "BadGatewayHandler");
        response.getHeaders().put("X-Error-Type", "Server Error");
        response.getHeaders().put("X-Retry-Recommended", "true");
    }
    
    /**
     * Generates a user-friendly 502 error page
     * 
     * @return HTML content for 502 error page
     */
    private String generateErrorPage() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>502 - Bad Gateway</title>
                <style>
                    body { font-family: Arial; text-align: center; padding: 50px; }
                    h1 { color: #f39c12; }
                </style>
            </head>
            <body>
                <h1>502 - Bad Gateway</h1>
                <p>The server received an invalid response. Please try again later.</p>
            </body>
            </html>
            """;
    }
}

// ========== ServiceUnavailableHandler.java (503) ==========
package org.example.webbrowser;

/**
 * Concrete Handler for HTTP 503 Service Unavailable responses
 * 
 * Handles cases where the server is temporarily unavailable (overloaded or down for maintenance).
 * This is a server error (5xx series) that is usually temporary.
 */
public class ServiceUnavailableHandler extends AbstractHTTPHandler {
    
    @Override
    protected boolean canHandle(HTTPResponse response) {
        return response.getStatusCode() == 503;
    }
    
    @Override
    protected void processResponse(HTTPResponse response) {
        System.out.println("[ServiceUnavailableHandler] Processing HTTP 503 Service Unavailable response");
        System.out.println("[ServiceUnavailableHandler] The server is temporarily unable to handle the request");
        System.out.println("[ServiceUnavailableHandler] This may be due to maintenance or overload");
        
        // Check for Retry-After header
        String retryAfter = response.getHeaders().get("Retry-After");
        if (retryAfter != null) {
            System.out.println("[ServiceUnavailableHandler] Server suggests retry after: " + retryAfter + " seconds");
        }
        
        // Enhance error response
        if (!response.getBody().contains("503")) {
            String errorPage = generateErrorPage(retryAfter);
            response.setBody(errorPage);
        }
        
        response.getHeaders().put("X-Handled-By", "ServiceUnavailableHandler");
        response.getHeaders().put("X-Error-Type", "Server Error");
        response.getHeaders().put("X-Retry-Recommended", "true");
    }
    
    /**
     * Generates a user-friendly 503 error page
     * 
     * @param retryAfter Suggested retry time (can be null)
     * @return HTML content for 503 error page
     */
    private String generateErrorPage(String retryAfter) {
        String retryMessage = retryAfter != null 
            ? "<p>Please try again in " + retryAfter + " seconds.</p>"
            : "<p>Please try again in a few moments.</p>";
            
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>503 - Service Unavailable</title>
                <style>
                    body { font-family: Arial; text-align: center; padding: 50px; }
                    h1 { color: #e67e22; }
                </style>
            </head>
            <body>
                <h1>503 - Service Unavailable</h1>
                <p>The server is temporarily unavailable.</p>
                """ + retryMessage + """
            </body>
            </html>
            """;
    }
}

// ========== DefaultHandler.java ==========
package org.example.webbrowser;

/**
 * Default Handler - catches all unhandled HTTP responses
 * 
 * This is the final handler in the chain that processes any response
 * not handled by previous handlers. It acts as a fallback.
 */
public class DefaultHandler extends AbstractHTTPHandler {
    
    @Override
    protected boolean canHandle(HTTPResponse response) {
        // This handler accepts any response
        return true;
    }
    
    @Override
    protected void processResponse(HTTPResponse response) {
        Integer statusCode = response.getStatusCode();
        
        System.out.println("[DefaultHandler] Processing HTTP " + statusCode + " response");
        System.out.println("[DefaultHandler] No specific handler found for this status code");
        
        // Categorize the response
        String category = categorizeStatusCode(statusCode);
        System.out.println("[DefaultHandler] Response category: " + category);
        
        // Generate generic error page if needed
        if (statusCode >= 400) {
            String errorPage = generateGenericErrorPage(statusCode, category);
            response.setBody(errorPage);
        }
        
        response.getHeaders().put("X-Handled-By", "DefaultHandler");
        response.getHeaders().put("X-Error-Category", category);
    }
    
    /**
     * Categorizes HTTP status code into general categories
     * 
     * @param statusCode The HTTP status code
     * @return String description of the category
     */
    private String categorizeStatusCode(Integer statusCode) {
        if (statusCode >= 200 && statusCode < 300) {
            return "Success";
        } else if (statusCode >= 300 && statusCode < 400) {
            return "Redirection";
        } else if (statusCode >= 400 && statusCode < 500) {
            return "Client Error";
        } else if (statusCode >= 500 && statusCode < 600) {
            return "Server Error";
        } else {
            return "Unknown";
        }
    }
    
    /**
     * Generates a generic error page for unhandled status codes
     * 
     * @param statusCode The HTTP status code
     * @param category The error category
     * @return HTML content for generic error page
     */
    private String generateGenericErrorPage(Integer statusCode, String category) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>""" + statusCode + """ - Error</title>
                <style>
                    body { font-family: Arial; text-align: center; padding: 50px; }
                    h1 { color: #95a5a6; }
                </style>
            </head>
            <body>
                <h1>""" + statusCode + """ - """ + category + """</h1>
                <p>An error occurred while processing your request.</p>
            </body>
            </html>
            """;
    }
}

// ========== HTTPHandlerChain.java ==========
package org.example.webbrowser;

/**
 * Chain of Responsibility Pattern: Chain Builder
 * 
 * This class builds and manages the chain of HTTP response handlers.
 * It provides a convenient way to create the handler chain and process responses.
 */
public class HTTPHandlerChain {
    
    private HTTPResponseHandler firstHandler;
    
    /**
     * Constructs the default handler chain
     * Order: 404 -> 502 -> 503 -> 200 -> Default
     */
    public HTTPHandlerChain() {
        buildDefaultChain();
    }
    
    /**
     * Builds the default chain of handlers
     * The order matters: more specific handlers should come before generic ones
     */
    private void buildDefaultChain() {
        // Create handlers
        HTTPResponseHandler notFoundHandler = new NotFoundHandler();
        HTTPResponseHandler badGatewayHandler = new BadGatewayHandler();
        HTTPResponseHandler serviceUnavailableHandler = new ServiceUnavailableHandler();
        HTTPResponseHandler successHandler = new SuccessHandler();
        HTTPResponseHandler defaultHandler = new DefaultHandler();
        
        // Link handlers in chain
        notFoundHandler.setNext(badGatewayHandler);
        badGatewayHandler.setNext(serviceUnavailableHandler);
        serviceUnavailableHandler.setNext(successHandler);
        successHandler.setNext(defaultHandler);
        
        // Set first handler
        firstHandler = notFoundHandler;
        
        System.out.println("[HTTPHandlerChain] Handler chain initialized:");
        System.out.println("[HTTPHandlerChain] NotFoundHandler -> BadGatewayHandler -> ServiceUnavailableHandler -> SuccessHandler -> DefaultHandler");
    }
    
    /**
     * Processes an HTTP response through the handler chain
     * 
     * @param response The HTTP response to process
     * @return true if response was handled by any handler in the chain
     */
    public boolean process(HTTPResponse response) {
        if (firstHandler == null) {
            System.err.println("[HTTPHandlerChain] Error: Handler chain is not initialized");
            return false;
        }
        
        System.out.println("\n[HTTPHandlerChain] Starting chain processing for status code: " + response.getStatusCode());
        boolean handled = firstHandler.handle(response);
        
        if (handled) {
            System.out.println("[HTTPHandlerChain] Response successfully processed by chain");
        } else {
            System.out.println("[HTTPHandlerChain] Warning: Response was not handled by any handler");
        }
        
        return handled;
    }
    
    /**
     * Gets the first handler in the chain
     * Useful for custom chain modifications
     * 
     * @return The first handler in the chain
     */
    public HTTPResponseHandler getFirstHandler() {
        return firstHandler;
    }
    
    /**
     * Sets a custom first handler (for advanced usage)
     * 
     * @param handler The handler to set as first in chain
     */
    public void setFirstHandler(HTTPResponseHandler handler) {
        this.firstHandler = handler;
    }
}