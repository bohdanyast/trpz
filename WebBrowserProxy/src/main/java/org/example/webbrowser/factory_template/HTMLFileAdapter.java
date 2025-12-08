package org.example.webbrowser.factory_template;

import org.example.webbrowser.HTMLFile;
import org.example.webbrowser.visitor.ResourceVisitor;

/**
 * Adapter for HTMLFile to implement Resource interface
 *
 * This adapter allows existing HTMLFile class to work with the new Resource interface
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
     * Delegates to underlying HTMLFile
     */
    @Override
    public void accept(ResourceVisitor visitor) {
        visitor.visit(htmlFile);
    }

    /**
     * Gets the underlying HTMLFile object
     *
     * @return HTMLFile instance
     */
    public HTMLFile getHtmlFile() {
        return htmlFile;
    }
}