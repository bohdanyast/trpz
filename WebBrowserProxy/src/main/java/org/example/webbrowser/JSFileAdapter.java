package org.example.webbrowser;

/**
 * Adapter for JSFile to implement Resource interface
 *
 * This adapter allows existing JSFile class to work with the new Resource interface
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
        return "JAVASCRIPT";
    }

    @Override
    public void load() {
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
     * Delegates to underlying JSFile
     */
    @Override
    public void accept(ResourceVisitor visitor) {
        visitor.visit(jsFile);
    }

    /**
     * Gets the underlying JSFile object
     *
     * @return JSFile instance
     */
    public JSFile getJsFile() {
        return jsFile;
    }
}