package org.example.webbrowser;

/**
 * Adapter Pattern: Adapts HTMLFile to Resource interface
 * 
 * This allows HTMLFile to be used through the common Resource interface
 * without modifying the original HTMLFile class
 */
public class HTMLFileAdapter implements Resource {
    
    private HTMLFile htmlFile;
    
    public HTMLFileAdapter(HTMLFile htmlFile) {
        this.htmlFile = htmlFile;
    }
    
    @Override
    public String getFileName() {
        return htmlFile.getFileName();
    }
    
    @Override
    public String getFilePath() {
        return htmlFile.getFilePath();
    }
    
    @Override
    public String getResourceType() {
        return "HTML";
    }
    
    @Override
    public void load() {
        System.out.println("[HTMLFileAdapter] Loading HTML: " + htmlFile.getFileName());
        htmlFile.loadHTML();
    }
    
    @Override
    public String getContent() {
        return htmlFile.getContent();
    }
    
    @Override
    public boolean isLoaded() {
        return htmlFile.getContent() != null && !htmlFile.getContent().isEmpty();
    }
    
    /**
     * Provides access to the underlying HTMLFile object
     * Used for backward compatibility
     * 
     * @return The wrapped HTMLFile object
     */
    public HTMLFile getHtmlFile() {
        return htmlFile;
    }
}