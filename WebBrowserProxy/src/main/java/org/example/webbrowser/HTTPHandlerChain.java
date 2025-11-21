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

        // Link handlers in chain
        successHandler.setNext(notFoundHandler);
        notFoundHandler.setNext(badGatewayHandler);
        badGatewayHandler.setNext(serviceUnavailableHandler);

        // Set first handler
        firstHandler = notFoundHandler;
    }
    
    /**
     * Processes an HTTP response through the handler chain
     * 
     * @param response The HTTP response to process
     * @return true if response was handled by any handler in the chain
     */
    public boolean process(HTTPResponse response) {
        if (firstHandler == null) {
            return false;
        }
        return firstHandler.handle(response);
    }
}
