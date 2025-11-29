module org.example.webbrowser {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires java.desktop;
    requires java.compiler;
    requires org.jsoup;
    requires jdk.httpserver;
    requires jdk.jsobject;

    opens org.example.webbrowser to javafx.fxml;
    exports org.example.webbrowser;
}