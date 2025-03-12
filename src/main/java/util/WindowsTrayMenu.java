package util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import runner.OpenBrowser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

//@Component
//@Profile("dev")
public class WindowsTrayMenu implements TrayMenu {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsTrayMenu.class);
    private static final String IMAGE_PATH = "image/multimedia.png";
    private static final String APP_NAME = "media-jscanner";
    private TrayIcon trayIcon;


    public void createTray() {
        if (!SystemTray.isSupported()) {
            LOG.error("[ tray ] System tray is not supported");
            return;
        }
        final PopupMenu popup = new PopupMenu();
        Image image = createImage(IMAGE_PATH, "tray icon");
        if (image == null) return;
        trayIcon = new TrayIcon(image, APP_NAME);
        trayIcon.setImageAutoSize(true);

        final SystemTray tray = SystemTray.getSystemTray();

        // Create a pop-up menu components
        MenuItem startItem = new MenuItem("Open UI");
        startItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                OpenBrowser.startUserInterface();
            }
        });
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

        //Add components to pop-up menu
        popup.add(startItem);
        popup.addSeparator();
        popup.add(exitItem);

        trayIcon.setPopupMenu(popup);

        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            LOG.error("TrayIcon could not be added.");
        }

    }

    /*
    * TODO change to event
    * */
    public void showMessage(String message) {
        trayIcon.displayMessage("message", message, TrayIcon.MessageType.NONE);
    }

    //Obtain the image URL
    private Image createImage(String path, String description) {
        URL imageURL = getClass().getClassLoader().getResource(path);
        if (imageURL == null) {
            LOG.error("Resource not found: {}", path);
            return null;
        } else {
            return (new ImageIcon(imageURL, description)).getImage();
        }
    }

}
