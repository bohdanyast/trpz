package org.example.webbrowser.visitor;

import org.example.webbrowser.factory_template.Resource;
import org.example.webbrowser.proxy.IImage;
import org.example.webbrowser.proxy.ImageProxy;

/**
 * Adapter for ImageProxy (IImage) to implement Resource interface
 *
 * This adapter allows ImageProxy to work with the new Resource interface
 */
public class ImageResourceAdapter implements Resource {

    private IImage imageProxy;

    public ImageResourceAdapter(IImage imageProxy) {
        this.imageProxy = imageProxy;
    }

    @Override
    public String getFileName() {
        return imageProxy.getFileName();
    }

    @Override
    public String getFilePath() {
        return imageProxy.getFilePath();
    }

    @Override
    public String getResourceType() {
        return "IMAGE";
    }

    @Override
    public void load() {
        imageProxy.loadImage();
    }

    @Override
    public String getContent() {
        return imageProxy.getContent();
    }

    @Override
    public boolean isLoaded() {
        return imageProxy.isLoaded();
    }

    /**
     * Delegates to underlying ImageProxy (if it's ImageProxy type)
     */
    @Override
    public void accept(ResourceVisitor visitor) {
        if (imageProxy instanceof ImageProxy) {
            visitor.visit((ImageProxy) imageProxy);
        }
    }

    /**
     * Gets the underlying IImage (ImageProxy) object
     *
     * @return IImage instance
     */
    public IImage getImageProxy() {
        return imageProxy;
    }

    /**
     * Displays the image (delegates to proxy)
     */
    public void display() {
        imageProxy.display();
    }
}