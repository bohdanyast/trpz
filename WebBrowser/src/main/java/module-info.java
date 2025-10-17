module org.example.webbrowser {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;


    opens org.example.webbrowser to javafx.fxml;
    exports org.example.webbrowser;
}