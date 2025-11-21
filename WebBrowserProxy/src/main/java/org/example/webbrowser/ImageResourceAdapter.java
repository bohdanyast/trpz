package org.example.webbrowser;

/**
 * Adapter Pattern: Adapts ImageProxy (IImage) to Resource interface
 * 
 * This allows ImageProxy to be used through the common Resource interface
 * Combines Proxy Pattern (for lazy loading) with Adapter Pattern
 */
public class ImageResourceAdapter implements Resource {
    
    private ImageProxy imageProxy;
    
    public ImageResourceAdapter(ImageProxy imageProxy) {
        this.imageProxy = imageProxy;
    }
    
    @Override
    public String getFileName() {
        return imageProxy.getFileName();
    }
    
    @Override
    public String getFilePath() {
        return imageProxy.getFilePath();
    }
    
    @Override
    public String getResourceType() {
        return "IMAGE";
    }
    
    @Override
    public void load() {
        System.out.println("[ImageResourceAdapter] Image proxy ready: " + imageProxy.getFileName());
    }
    
    @Override
    public String getContent() {
        // Images don't have text content - return metadata instead
        return "Image: " + imageProxy.getFileName() + " at " + imageProxy.getFilePath();
    }
    
    @Override
    public boolean isLoaded() {
        // Could check if the real image has been loaded through the proxy
        // For now, return false since images use lazy loading
        return false;
    }
    
    /**
     * Provides access to the underlying ImageProxy object
     * Used for backward compatibility and to access display() method
     * 
     * @return The wrapped ImageProxy object
     */
    public ImageProxy getImageProxy() {
        return imageProxy;
    }

    public void display() {
        imageProxy.display();
    }
}