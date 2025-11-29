package org.example.webbrowser;

/**
 * Adapter for CSSFile to implement Resource interface
 *
 * This adapter allows existing CSSFile class to work with the new Resource interface
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
     * Delegates to underlying CSSFile
     */
    @Override
    public void accept(ResourceVisitor visitor) {
        visitor.visit(cssFile);
    }

    /**
     * Gets the underlying CSSFile object
     *
     * @return CSSFile instance
     */
    public CSSFile getCssFile() {
        return cssFile;
    }
}