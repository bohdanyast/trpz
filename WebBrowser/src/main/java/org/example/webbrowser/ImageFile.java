package org.example.webbrowser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

public class ImageFile {
    private String fileName;
    private String filePath;
    private String content; // Base64 encoded content
    
    public ImageFile(String fileName, String filePath) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.content = "";
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }

    public void loadImage() {
        try {
            byte[] fileContent = Files.readAllBytes(Paths.get(filePath));
            this.content = Base64.getEncoder().encodeToString(fileContent);
            System.out.println("Image file loaded: " + fileName);
        } catch (IOException e) {
            System.err.println("Error loading image file: " + fileName);
            e.printStackTrace();
        }
    }
}