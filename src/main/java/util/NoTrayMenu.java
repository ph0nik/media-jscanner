package util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//@Component
//@Profile("prod")
public class NoTrayMenu implements TrayMenu {

    private static final Logger LOG = LoggerFactory.getLogger(NoTrayMenu.class);

    @Override
    public void createTray() {
        LOG.info("[ tray_menu ] No support for tray menu.");
    }

    @Override
    public void showMessage(String message) {
        LOG.info("[ tray_menu ] message: {}", message);
    }
}
