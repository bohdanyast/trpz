package org.example.webbrowser.factory_template;

import org.example.webbrowser.*;
import org.example.webbrowser.proxy.ImageProxy;
import org.example.webbrowser.visitor.ImageResourceAdapter;

/**
 * Factory Method Pattern: Concrete Creator
 */
public class ResourceFactory extends ResourceCreator {

    /**
     * Creates appropriate Resource based on file extension
     *
     * @param fileName Name of the file
     * @param filePath Path or URL of the file
     * @return Resource object (wrapped in appropriate adapter)
     */
    @Override
    public Resource createResource(String fileName, String filePath) {
        String extension = getFileExtension(fileName);

        System.out.println("[ResourceFactory] Creating resource: " + fileName + " (type: " + extension + ")");

        switch (extension.toLowerCase()) {
            case "css":
                CSSFile cssFile = new CSSFile(fileName, filePath);
                return new CSSFileAdapter(cssFile);

            case "js":
                JSFile jsFile = new JSFile(fileName, filePath);
                return new JSFileAdapter(jsFile);

            case "jpg":
            case "jpeg":
            case "png":
            case "gif":
            case "bmp":
            case "webp":
            case "svg":
                // Use Proxy pattern for images (lazy loading)
                ImageProxy imageProxy = new ImageProxy(fileName, filePath);
                return new ImageResourceAdapter(imageProxy);

            case "html":
            case "htm":
                HTMLFile htmlFile = new HTMLFile(fileName, filePath, "");
                return new HTMLFileAdapter(htmlFile);

            default:
                System.out.println("[ResourceFactory] Warning: Unknown file type '" + extension + "', creating generic resource");
                // Fallback to CSS adapter for unknown types
                return new CSSFileAdapter(new CSSFile(fileName, filePath));
        }
    }

    /**
     * Extracts file extension from filename
     *
     * @param fileName Name of the file
     * @return File extension (without dot)
     */
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot + 1);
        }
        return "";
    }

    /**
     * Checks if a file is an image based on extension
     *
     * @param fileName Name of the file
     * @return true if image, false otherwise
     */
    public boolean isImage(String fileName) {
        String ext = getFileExtension(fileName).toLowerCase();
        return ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png") ||
                ext.equals("gif") || ext.equals("bmp") || ext.equals("webp") ||
                ext.equals("svg");
    }
}