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
    exports org.example.webbrowser.proxy;
    opens org.example.webbrowser.proxy to javafx.fxml;
    exports org.example.webbrowser.chain;
    opens org.example.webbrowser.chain to javafx.fxml;
    exports org.example.webbrowser.factory_template;
    opens org.example.webbrowser.factory_template to javafx.fxml;
    exports org.example.webbrowser.visitor;
    opens org.example.webbrowser.visitor to javafx.fxml;
}