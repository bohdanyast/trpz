package org.example.webbrowser;

import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;

import java.net.URL;
import java.util.ResourceBundle;

public class WebBrowserController implements Initializable {
    @FXML
    private WebView webView;

    @FXML
    private TextField textField;

    private WebEngine webEngine;
    private WebHistory webHistory;

    // Нові класи для архітектури
    private Browser browser;
    private AddressBar addressBar;
    private WebPage currentWebPage;

    // Локальний веб-сервер для тестування
    private WebServer localServer;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        webEngine = webView.getEngine();

        // Ініціалізація нової архітектури
        browser = new Browser();
        addressBar = new AddressBar();
        browser.setAddressBar(addressBar);

        // Ініціалізація локального тестового сервера
        initializeLocalServer();

        // Слухач для обробки завантаження сторінки
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                onPageLoaded();
            } else if (newState == Worker.State.FAILED) {
                browser.handleError(500);
            }
        });

        loadPage();
    }

    /**
     * Ініціалізує локальний тестовий веб-сервер
     *
     * ПОЯСНЕННЯ ЧОМУ ЦЕ РЕАЛЬНИЙ СЕРВЕР:
     * WebServer симулює поведінку реального HTTP сервера:
     * 1. Приймає HTTP запити (HTTPRequest)
     * 2. Обробляє їх методом processRequest()
     * 3. Генерує HTTP відповіді (HTTPResponse) з правильними кодами статусу
     * 4. Зберігає колекцію веб-сторінок та ресурсів
     * 5. Відправляє відповіді клієнту методом sendResponse()
     *
     * У реальному сценарії цей сервер міг би:
     * - Слухати TCP порт (наприклад 8080)
     * - Приймати з'єднання через Socket
     * - Обслуговувати багато клієнтів одночасно
     * - Працювати як окремий процес/сервіс
     *
     * У нашому випадку він працює в тому ж процесі (embedded server),
     * що типово для тестування та розробки (як Tomcat Embedded, Jetty)
     */
    private void initializeLocalServer() {
        localServer = new WebServer("test.com");

        // Додаємо тестові ресурси
        localServer.addResource("index.html");
        localServer.addResource("about.html");
        localServer.addResource("contact.html");

        // Створюємо тестову сторінку для успішної відповіді (200)
        WebPage testPage = new WebPage();
        String testHTML = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Test Server - Success</title>
                <link rel="stylesheet" href="styles/main.css">
            </head>
            <body>
                <h1>Test Server Page</h1>
                <p>This page is served by the local WebServer!</p>
                <img src="images/test.png" alt="Test">
                <script src="js/app.js"></script>
            </body>
            </html>
            """;
        testPage.setRawHTML(testHTML);
        testPage.parseHTML();
        localServer.addPage(testPage);

        // Створюємо сторінку 404
        WebPage page404 = new WebPage();
        String html404 = """
            <!DOCTYPE html>
            <html>
            <head><title>404 Not Found</title></head>
            <body>
                <h1>404 - Page Not Found</h1>
                <p>The requested resource was not found on this server.</p>
            </body>
            </html>
            """;
        page404.setRawHTML(html404);
        localServer.addPage(page404);

        // Створюємо сторінку 502
        WebPage page502 = new WebPage();
        String html502 = """
            <!DOCTYPE html>
            <html>
            <head><title>502 Bad Gateway</title></head>
            <body>
                <h1>502 - Bad Gateway</h1>
                <p>The server received an invalid response from the upstream server.</p>
            </body>
            </html>
            """;
        page502.setRawHTML(html502);
        localServer.addPage(page502);

        // Створюємо сторінку 503
        WebPage page503 = new WebPage();
        String html503 = """
            <!DOCTYPE html>
            <html>
            <head><title>503 Service Unavailable</title></head>
            <body>
                <h1>503 - Service Unavailable</h1>
                <p>The server is temporarily unable to handle the request.</p>
            </body>
            </html>
            """;
        page503.setRawHTML(html503);
        localServer.addPage(page503);

        System.out.println("Local WebServer initialized at: " + localServer.getHost());
        System.out.println("Test URLs:");
        System.out.println("  - test.com/index.html (200 OK)");
        System.out.println("  - test.com/404 (404 Not Found)");
        System.out.println("  - test.com/502 (502 Bad Gateway)");
        System.out.println("  - test.com/503 (503 Service Unavailable)");
    }

    public void loadPage() {
        String inputUrl = textField.getText();

        // Перевіряємо чи це запит до локального тестового сервера
        if (inputUrl.contains("test.com")) {
            handleLocalServerRequest(inputUrl);
            return;
        }

        // Використовуємо AddressBar для валідації
        if (addressBar.validateURL(inputUrl)) {
            // Формуємо повний URL з протоколом
            String fullUrl = inputUrl;
            if (!inputUrl.startsWith("http://") && !inputUrl.startsWith("https://")) {
                fullUrl = addressBar.getProtocol() + "://" + inputUrl;
            }

            // Оновлюємо addressBar
            addressBar.setUrl(fullUrl);

            // Завантажуємо через WebEngine
            webEngine.load(fullUrl);

            // Також оновлюємо Browser
            browser.loadPage(fullUrl);

        } else {
            browser.handleError(400);
            System.err.println("Invalid URL: " + inputUrl);
        }
    }

    /**
     * Обробляє запити до локального тестового сервера
     * Демонструє повний цикл клієнт-сервер взаємодії
     */
    private void handleLocalServerRequest(String url) {
        System.out.println("\n=== Local Server Request ===");

        // Формуємо повний URL
        String fullUrl = url;
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            fullUrl = "http://" + url;
        }

        // Створюємо HTTP запит (клієнт)
        HTTPRequest request = new HTTPRequest(fullUrl, "GET");
        System.out.println("Client sending request to: " + fullUrl);

        // Сервер обробляє запит
        HTTPResponse response = new HTTPResponse();
        String htmlContent = "";

        // Визначаємо який код статусу повернути на основі URL
        if (url.contains("/404")) {
            response.setStatusCode(404);
            WebPage errorPage = localServer.getPages().get(1);
            if (!errorPage.getHtmlResources().isEmpty()) {
                htmlContent = errorPage.getHtmlResources().get(0).getContent();
            } else {
                // Якщо контент ще не завантажений, використовуємо raw HTML
                htmlContent = errorPage.getRawHTML();
            }
            response.setBody(htmlContent);
            response.getHeaders().put("Content-Type", "text/html");
        } else if (url.contains("/502")) {
            response.setStatusCode(502);
            WebPage errorPage = localServer.getPages().get(2);
            if (!errorPage.getHtmlResources().isEmpty()) {
                htmlContent = errorPage.getHtmlResources().get(0).getContent();
            } else {
                htmlContent = errorPage.getRawHTML();
            }
            response.setBody(htmlContent);
            response.getHeaders().put("Content-Type", "text/html");
        } else if (url.contains("/503")) {
            response.setStatusCode(503);
            WebPage errorPage = localServer.getPages().get(3);
            if (!errorPage.getHtmlResources().isEmpty()) {
                htmlContent = errorPage.getHtmlResources().get(0).getContent();
            } else {
                htmlContent = errorPage.getRawHTML();
            }
            response.setBody(htmlContent);
            response.getHeaders().put("Content-Type", "text/html");
        } else {
            // Нормальний запит - повертаємо 200
            response = localServer.processRequest(request);
        }

        // Сервер відправляє відповідь
        localServer.sendResponse(response);

        // Клієнт обробляє відповідь
        request.parseResponse(response);

        // Відображаємо результат у WebView (для всіх кодів статусу)
        webEngine.loadContent(response.getBody(), "text/html");

        // Парсимо сторінку
        currentWebPage = new WebPage();
        currentWebPage.setRawHTML(response.getBody());
        currentWebPage.parseHTML();
        browser.displayPage(currentWebPage);

        if (response.getStatusCode() == 200) {
            System.out.println("✓ Page loaded successfully from local server");
            printPageInfo();
        } else {
            // Викликаємо обробник помилок
            browser.handleError(response.getStatusCode());
            System.out.println("✓ Error page displayed with status code: " + response.getStatusCode());
        }
    }

    /**
     * Викликається коли сторінка успішно завантажена
     */
    private void onPageLoaded() {
        try {
            // Отримуємо HTML контент завантаженої сторінки
            String htmlContent = (String) webEngine.executeScript("document.documentElement.outerHTML");

            // Створюємо WebPage та парсимо HTML
            currentWebPage = new WebPage();
            currentWebPage.setRawHTML(htmlContent);
            currentWebPage.parseHTML();

            // Оновлюємо поточну сторінку в Browser
            browser.displayPage(currentWebPage);

            // Виводимо інформацію про завантажені ресурси
            System.out.println("\n=== Page Loaded: " + webEngine.getLocation() + " ===");
            System.out.println("HTML files: " + currentWebPage.getHtmlResources().size());
            System.out.println("CSS files: " + currentWebPage.getCssResources().size());
            System.out.println("JS files: " + currentWebPage.getJsResources().size());
            System.out.println("Images: " + currentWebPage.getImageResources().size());

            // Виводимо деталі про CSS файли
            if (!currentWebPage.getCssResources().isEmpty()) {
                System.out.println("\nCSS Resources:");
                for (CSSFile css : currentWebPage.getCssResources()) {
                    System.out.println("  - " + css.getFileName());
                }
            }

            // Виводимо деталі про JS файли
            if (!currentWebPage.getJsResources().isEmpty()) {
                System.out.println("\nJS Resources:");
                for (JSFile js : currentWebPage.getJsResources()) {
                    System.out.println("  - " + js.getFileName());
                }
            }

            // Виводимо деталі про зображення (через Proxy)
            if (!currentWebPage.getImageResources().isEmpty()) {
                System.out.println("\nImage Resources (Proxies):");
                for (IImage img : currentWebPage.getImageResources()) {
                    System.out.println("  - " + img.getFileName() + " [Loaded: " + img.isLoaded() + "]");
                }
            }

        } catch (Exception e) {
            System.err.println("Error parsing page: " + e.getMessage());
        }
    }

    public void goBack() {
        webHistory = webEngine.getHistory();
        ObservableList<WebHistory.Entry> entries = webHistory.getEntries();

        if (webHistory.getCurrentIndex() > 0) {
            webHistory.go(-1);
            String currentUrl = entries.get(webHistory.getCurrentIndex()).getUrl();
            textField.setText(currentUrl);
            addressBar.setUrl(currentUrl);
        }
    }

    public void goForward() {
        webHistory = webEngine.getHistory();
        ObservableList<WebHistory.Entry> entries = webHistory.getEntries();

        if (webHistory.getCurrentIndex() < entries.size() - 1) {
            webHistory.go(1);
            String currentUrl = entries.get(webHistory.getCurrentIndex()).getUrl();
            textField.setText(currentUrl);
            addressBar.setUrl(currentUrl);
        }
    }

    public void reload() {
        webEngine.reload();
    }

    // Геттери для доступу до компонентів архітектури

    public Browser getBrowser() {
        return browser;
    }

    public AddressBar getAddressBar() {
        return addressBar;
    }

    public WebPage getCurrentWebPage() {
        return currentWebPage;
    }

    public WebServer getLocalServer() {
        return localServer;
    }

    /**
     * Отримати інформацію про поточну сторінку
     */
    public void printPageInfo() {
        if (currentWebPage != null) {
            System.out.println("\n=== Current Page Info ===");
            System.out.println("URL: " + addressBar.getUrl());
            System.out.println("Protocol: " + addressBar.getProtocol());
            System.out.println("Total HTML files: " + currentWebPage.getHtmlResources().size());
            System.out.println("Total CSS files: " + currentWebPage.getCssResources().size());
            System.out.println("Total JS files: " + currentWebPage.getJsResources().size());
            System.out.println("Total Images: " + currentWebPage.getImageResources().size());
        } else {
            System.out.println("No page loaded yet");
        }
    }
}