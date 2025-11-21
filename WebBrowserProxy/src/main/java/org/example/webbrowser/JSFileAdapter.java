package org.example.webbrowser;

/**
 * Adapter Pattern: Adapts JSFile to Resource interface
 * 
 * This allows JSFile to be used through the common Resource interface
 * without modifying the original JSFile class
 */
public class JSFileAdapter implements Resource {
    
    private JSFile jsFile;
    
    public JSFileAdapter(JSFile jsFile) {
        this.jsFile = jsFile;
    }
    
    @Override
    public String getFileName() {
        return jsFile.getFileName();
    }
    
    @Override
    public String getFilePath() {
        return jsFile.getFilePath();
    }
    
    @Override
    public String getResourceType() {
        return "JS";
    }
    
    @Override
    public void load() {
        System.out.println("[JSFileAdapter] Loading JavaScript: " + jsFile.getFileName());
        jsFile.loadJS();
    }
    
    @Override
    public String getContent() {
        return jsFile.getContent();
    }
    
    @Override
    public boolean isLoaded() {
        return jsFile.getContent() != null && !jsFile.getContent().isEmpty();
    }
    
    /**
     * Provides access to the underlying JSFile object
     * Used for backward compatibility
     * 
     * @return The wrapped JSFile object
     */
    public JSFile getJsFile() {
        return jsFile;
    }
}