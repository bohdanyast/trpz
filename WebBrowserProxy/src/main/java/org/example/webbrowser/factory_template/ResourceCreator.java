package org.example.webbrowser.factory_template;

/**
 * Factory Method Pattern: Creator (Abstract Creator)
 */
public abstract class ResourceCreator {
    
    /**
     * Factory Method: Abstract method that must be implemented by subclasses
     *
     * @param fileName Name of the file
     * @param filePath Path or URL of the file
     * @return Resource object (Product)
     */
    public abstract Resource createResource(String fileName, String filePath);
}