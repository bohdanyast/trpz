package org.example.webbrowser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebPage {
    private List<HTMLFile> htmlResources;
    private List<CSSFile> cssResources;
    private List<JSFile> jsResources;
    private List<IImage> imageResources;

    // Unified list of all resources (Factory Method Pattern)
    private List<Resource> allResources;

    private String rawHTML;

    // Factory Method Pattern: використовуємо Creator
    private ResourceCreator resourceCreator;

    public WebPage() {
        this.htmlResources = new ArrayList<>();
        this.cssResources = new ArrayList<>();
        this.jsResources = new ArrayList<>();
        this.imageResources = new ArrayList<>();
        this.allResources = new ArrayList<>();

        this.resourceCreator = new ResourceFactory();
    }

    public List<HTMLFile> getHtmlResources() {
        return htmlResources;
    }

    public List<CSSFile> getCssResources() {
        return cssResources;
    }

    public List<JSFile> getJsResources() {
        return jsResources;
    }

    public List<IImage> getImageResources() {
        return imageResources;
    }

    /**
     * Gets all resources as unified list
     *
     * @return List of all resources
     */
    public List<Resource> getAllResources() {
        return allResources;
    }

    public void setRawHTML(String rawHTML) {
        this.rawHTML = rawHTML;
    }

    public String getRawHTML() {
        return rawHTML;
    }

    /**
     * Parses HTML code and extracts corresponding elements
     */
    public void parseHTML() {
        if (rawHTML == null || rawHTML.isEmpty()) {
            System.out.println("[WebPage] No HTML content to parse");
            return;
        }

        // Clear previous resources
        clearResources();

        // Extract and create CSS resources
        extractResources(
                "<link[^>]*href=[\"']([^\"']*\\.css)[\"'][^>]*>",
                "CSS"
        );

        // Extract and create JS resources
        extractResources(
                "<script[^>]*src=[\"']([^\"']*\\.js)[\"'][^>]*>",
                "JavaScript"
        );

        // Extract and create image resources
        extractResources(
                "<img[^>]*src=[\"']([^\"']*\\.(jpg|jpeg|png|gif|bmp|webp|svg))[\"'][^>]*>",
                "Image"
        );

        // Create main HTML resource
        createMainHTMLResource();
    }

    /**
     * Helper method to extract resources using regex pattern
     *
     * @param regexPattern Regex pattern to match resources
     * @param resourceTypeName Name of resource type for logging
     */
    private void extractResources(String regexPattern, String resourceTypeName) {
        Pattern pattern = Pattern.compile(regexPattern, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(rawHTML);

        int count = 0;
        while (matcher.find()) {
            String resourcePath = matcher.group(1);
            String fileName = extractFileName(resourcePath);

            Resource resource = resourceCreator.createResource(fileName, resourcePath);
            allResources.add(resource);

            // Add to specific lists for backward compatibility
            addToSpecificList(resource);
            count++;
        }

        if (count > 0) {
            System.out.println("[WebPage] Found " + count + " " + resourceTypeName + " resource(s)");
        }
    }

    /**
     * Adds resource to specific type list for backward compatibility
     *
     * @param resource Resource to add
     */
    private void addToSpecificList(Resource resource) {
        if (resource instanceof CSSFileAdapter) {
            cssResources.add(((CSSFileAdapter) resource).getCssFile());
        } else if (resource instanceof JSFileAdapter) {
            jsResources.add(((JSFileAdapter) resource).getJsFile());
        } else if (resource instanceof ImageResourceAdapter) {
            imageResources.add(((ImageResourceAdapter) resource).getImageProxy());
        } else if (resource instanceof HTMLFileAdapter) {
            htmlResources.add(((HTMLFileAdapter) resource).getHtmlFile());
        }
    }

    /**
     * Creates the main HTML resource using Factory Method
     */
    private void createMainHTMLResource() {
        Resource mainHTMLResource = resourceCreator.createResource("index.html", "index.html");

        // Встановлюємо вміст для головного HTML
        if (mainHTMLResource instanceof HTMLFileAdapter) {
            HTMLFile htmlFile = ((HTMLFileAdapter) mainHTMLResource).getHtmlFile();
            htmlFile.setContent(rawHTML);
            htmlResources.add(htmlFile);
        }

        allResources.add(mainHTMLResource);
    }

    /**
     * Clears all resource lists
     */
    private void clearResources() {
        allResources.clear();
        cssResources.clear();
        jsResources.clear();
        imageResources.clear();
        htmlResources.clear();
    }

    /**
     * Loads all resources using unified interface
     * Factory Method Pattern: all resources implement same interface
     */
    public void loadResources() {
        int loaded = 0;
        for (Resource resource : allResources) {
            // Skip images as they use lazy loading (Proxy pattern)
            if (!resource.getResourceType().equals("IMAGE")) {
                resource.load();
                loaded++;
            } else {
                System.out.println("[WebPage] Skipping image (lazy loading): " + resource.getFileName());
            }
        }

        System.out.println("[WebPage] Successfully loaded " + loaded + " resources (images excluded - lazy loading)");
    }

    /**
     * Displays all images (triggers lazy loading)
     */
    public void displayImages() {
        for (Resource resource : allResources) {
            if (resource instanceof ImageResourceAdapter) {
                ((ImageResourceAdapter) resource).display();
            }
        }
    }

    /**
     * Extracts filename from path
     *
     * @param path Full path or URL
     * @return Filename only
     */
    private String extractFileName(String path) {
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }

    /**
     * Allows visitor to visit all resources
     *
     * @param visitor Visitor to accept
     */
    public void acceptVisitor(ResourceVisitor visitor) {
        for (Resource resource : allResources) {
            resource.accept(visitor);
        }
    }
}