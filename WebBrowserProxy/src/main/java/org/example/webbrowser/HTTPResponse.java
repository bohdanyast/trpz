package org.example.webbrowser;

import java.util.HashMap;
import java.util.Map;

public class HTTPResponse {
    private Integer statusCode;
    private Map<String, String> headers;
    private String body;
    
    public HTTPResponse() {
        this.statusCode = 0;
        this.headers = new HashMap<>();
        this.body = "";
    }
    
    public Integer getStatusCode() {
        return statusCode;
    }
    
    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }
    
    public Map<String, String> getHeaders() {
        return headers;
    }
    
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }
    
    public String getBody() {
        return body;
    }
    
    public void setBody(String body) {
        this.body = body;
    }

    public void handleRedirect() {
        if (statusCode >= 300 && statusCode < 400) {
            String location = headers.get("Location");
            if (location != null) {
                System.out.println("Redirect to: " + location);
            } else {
                System.out.println("Redirect status code " + statusCode + " without Location header");
            }
        }
    }

    public void handleError() {
        if (statusCode >= 400 && statusCode < 500) {
            System.err.println("Client Error " + statusCode + ": " + getErrorMessage(statusCode));
        } else if (statusCode >= 500) {
            System.err.println("Server Error " + statusCode + ": " + getErrorMessage(statusCode));
        }
    }

    private String getErrorMessage(Integer code) {
        return switch (code) {
            case 404 -> "Not Found";
            case 502 -> "Bad Gateway";
            case 503 -> "Service Unavailable";
            default -> "Unknown Error";
        };
    }
}