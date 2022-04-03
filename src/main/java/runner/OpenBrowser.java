package runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class OpenBrowser {

    private static final Logger LOG = LoggerFactory.getLogger(OpenBrowser.class);
    private static final String HOMEPAGE = "http://localhost:8081/jscanner";

    private static void openBrowserWithAddress(String address) {
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            try {
                desktop.browse(new URI(address));
            } catch (IOException | URISyntaxException e) {
                LOG.error(e.getMessage());
            }
        } else {
            Runtime runtime = Runtime.getRuntime();
            try {
                runtime.exec("rundll32 url.dll,FileProtocolHandler " + address);
            } catch (IOException e) {
                LOG.error(e.getMessage());
            }
        }
    }

    public static void startUserInterface() {
        openBrowserWithAddress(HOMEPAGE);
    }

}
