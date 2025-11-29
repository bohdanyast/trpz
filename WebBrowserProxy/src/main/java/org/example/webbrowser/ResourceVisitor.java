package org.example.webbrowser;

/**
 * Visitor Pattern: Visitor Interface
 * 
 * Defines operations that can be performed on different types of resources.
 * Each visit method handles a specific resource type.
 */
public interface ResourceVisitor {
    
    /**
     * Visit HTML file resource
     * 
     * @param htmlFile HTML file to visit
     */
    void visit(HTMLFile htmlFile);
    
    /**
     * Visit CSS file resource
     * 
     * @param cssFile CSS file to visit
     */
    void visit(CSSFile cssFile);
    
    /**
     * Visit JavaScript file resource
     * 
     * @param jsFile JS file to visit
     */
    void visit(JSFile jsFile);
    
    /**
     * Visit Image resource (via proxy)
     * 
     * @param imageProxy Image proxy to visit
     */
    void visit(ImageProxy imageProxy);
}