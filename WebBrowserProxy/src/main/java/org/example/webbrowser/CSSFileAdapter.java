package org.example.webbrowser;

/**
 * Adapter Pattern: Adapts CSSFile to Resource interface
 * 
 * This allows CSSFile to be used through the common Resource interface
 * without modifying the original CSSFile class
 */
public class CSSFileAdapter implements Resource {
    
    private CSSFile cssFile;
    
    public CSSFileAdapter(CSSFile cssFile) {
        this.cssFile = cssFile;
    }
    
    @Override
    public String getFileName() {
        return cssFile.getFileName();
    }
    
    @Override
    public String getFilePath() {
        return cssFile.getFilePath();
    }
    
    @Override
    public String getResourceType() {
        return "CSS";
    }
    
    @Override
    public void load() {
        System.out.println("[CSSFileAdapter] Loading CSS: " + cssFile.getFileName());
        cssFile.loadCSS();
    }
    
    @Override
    public String getContent() {
        return cssFile.getContent();
    }
    
    @Override
    public boolean isLoaded() {
        return cssFile.getContent() != null && !cssFile.getContent().isEmpty();
    }
    
    /**
     * Provides access to the underlying CSSFile object
     * Used for backward compatibility
     * 
     * @return The wrapped CSSFile object
     */
    public CSSFile getCssFile() {
        return cssFile;
    }
}