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
        notFoundHandler.setNext(badGatewayHandler);
        badGatewayHandler.setNext(serviceUnavailableHandler);
        serviceUnavailableHandler.setNext(successHandler);

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
}
