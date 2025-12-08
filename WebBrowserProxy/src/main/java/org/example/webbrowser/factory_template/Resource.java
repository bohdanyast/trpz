package org.example.webbrowser.factory_template;

import org.example.webbrowser.visitor.ResourceVisitor;

/**
 * Factory Method Pattern: Product Interface
 * 
 * Common interface for all types of web resources (HTML, CSS, JS, Images, etc.)
 * This allows uniform handling of different resource types
 */
public interface Resource {
    
    /**
     * Gets the file name of the resource
     * 
     * @return File name
     */
    String getFileName();
    
    /**
     * Gets the file path of the resource
     * 
     * @return File path or URL
     */
    String getFilePath();
    
    /**
     * Gets the resource type (HTML, CSS, JS, IMAGE, etc.)
     * 
     * @return Resource type as string
     */
    String getResourceType();
    
    /**
     * Loads the resource content
     * Implementation depends on specific resource type
     */
    void load();
    
    /**
     * Gets the content of the resource
     * 
     * @return Content as string
     */
    String getContent();
    
    /**
     * Checks if resource is loaded
     * 
     * @return true if loaded, false otherwise
     */
    boolean isLoaded();

    /**
     * Allows visitor to visit this resource
     *
     * @param visitor Visitor to accept
     */
    void accept(ResourceVisitor visitor);
}