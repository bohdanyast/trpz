package org.example.webbrowser;

/**
 * Calculates total size of all resources on a web page.
 * Visits each resource and accumulates size information.
 */
public class ResourceSizeCalculatorVisitor implements ResourceVisitor {
    
    private long totalSize;
    private long htmlSize;
    private long cssSize;
    private long jsSize;
    private long imageSize;
    
    private int htmlCount;
    private int cssCount;
    private int jsCount;
    private int imageCount;
    
    public ResourceSizeCalculatorVisitor() {
        this.totalSize = 0;
        this.htmlSize = 0;
        this.cssSize = 0;
        this.jsSize = 0;
        this.imageSize = 0;
        this.htmlCount = 0;
        this.cssCount = 0;
        this.jsCount = 0;
        this.imageCount = 0;
    }
    
    @Override
    public void visit(HTMLFile htmlFile) {
        long size = calculateSize(htmlFile.getContent());
        htmlSize += size;
        totalSize += size;
        htmlCount++;
        
        System.out.println("[ResourceSizeCalculator] HTML: " + htmlFile.getFileName() + 
                         " - " + formatSize(size));
    }
    
    @Override
    public void visit(CSSFile cssFile) {
        long size = calculateSize(cssFile.getContent());
        cssSize += size;
        totalSize += size;
        cssCount++;
        
        System.out.println("[ResourceSizeCalculator] CSS: " + cssFile.getFileName() + 
                         " - " + formatSize(size));
    }
    
    @Override
    public void visit(JSFile jsFile) {
        long size = calculateSize(jsFile.getContent());
        jsSize += size;
        totalSize += size;
        jsCount++;
    }
    
    @Override
    public void visit(ImageProxy imageProxy) {
        // For images, we calculate size even if not loaded (proxy pattern)
        // If loaded, get actual size; if not, estimate
        long size;
        if (imageProxy.isLoaded()) {
            size = calculateSize(imageProxy.getContent());
        } else {
            // Estimate size for unloaded images (placeholder size)
            size = 1024; // 1 KB placeholder
        }
        
        imageSize += size;
        totalSize += size;
        imageCount++;
    }
    
    /**
     * Calculates size of content in bytes
     * 
     * @param content Content string
     * @return Size in bytes
     */
    private long calculateSize(String content) {
        if (content == null || content.isEmpty()) {
            return 0;
        }
        // Size in bytes (UTF-8 encoding)
        return content.getBytes().length;
    }
    
    /**
     * Formats size in human-readable format
     * 
     * @param bytes Size in bytes
     * @return Formatted string (e.g., "15.5 KB")
     */
    private String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        }
    }
    
    /**
     * Gets total size of all resources
     * 
     * @return Total size in bytes
     */
    public long getTotalSize() {
        return totalSize;
    }
    
    /**
     * Gets total size of HTML resources
     * 
     * @return HTML size in bytes
     */
    public long getHtmlSize() {
        return htmlSize;
    }
    
    /**
     * Gets total size of CSS resources
     * 
     * @return CSS size in bytes
     */
    public long getCssSize() {
        return cssSize;
    }
    
    /**
     * Gets total size of JavaScript resources
     * 
     * @return JS size in bytes
     */
    public long getJsSize() {
        return jsSize;
    }
    
    /**
     * Gets total size of image resources
     * 
     * @return Image size in bytes
     */
    public long getImageSize() {
        return imageSize;
    }
    
    /**
     * Gets count of each resource type
     */
    public int getHtmlCount() { return htmlCount; }
    public int getCssCount() { return cssCount; }
    public int getJsCount() { return jsCount; }
    public int getImageCount() { return imageCount; }
    
    /**
     * Prints detailed size report
     */
    public void printReport() {

        if (htmlCount > 0) {
            System.out.println("HTML Files:   " + htmlCount + " files - " + formatSize(htmlSize));
        }
        
        if (cssCount > 0) {
            System.out.println("CSS Files:    " + cssCount + " files - " + formatSize(cssSize));
        }
        
        if (jsCount > 0) {
            System.out.println("JS Files:     " + jsCount + " files - " + formatSize(jsSize));
        }
        
        if (imageCount > 0) {
            System.out.println("Images:       " + imageCount + " files - " + formatSize(imageSize));
        }
    }
    
    /**
     * Resets calculator for new calculation
     */
    public void reset() {
        totalSize = 0;
        htmlSize = 0;
        cssSize = 0;
        jsSize = 0;
        imageSize = 0;
        htmlCount = 0;
        cssCount = 0;
        jsCount = 0;
        imageCount = 0;
    }
}