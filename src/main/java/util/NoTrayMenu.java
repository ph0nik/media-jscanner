package util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
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
