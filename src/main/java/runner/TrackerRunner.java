package runner;

import dao.MediaTrackerDao;
import dao.MediaTrackerDaoImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.MediaTrackerService;
import service.PropertiesServiceImpl;
import util.CleanerService;
import util.CleanerServiceImpl;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchService;
import java.util.List;

public class TrackerRunner implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(TrackerRunner.class);

    private final List<Path> targetFolderList;
    private final MediaTrackerDao mediaTrackerDao;
    private final CleanerService cleanerService;

    public TrackerRunner(){
        PropertiesServiceImpl props = new PropertiesServiceImpl();
        targetFolderList = props.getTargetFolderList();
        mediaTrackerDao = new MediaTrackerDaoImpl();
        cleanerService = new CleanerServiceImpl();
    }

    public List<Path> getTargetFolderList() {
        return targetFolderList;
    }

    @Override
    public void run() {
        LOG.info("[ tracker ] starting...");
        MediaTrackerService mediaTrackerService = new MediaTrackerService(mediaTrackerDao, cleanerService);
        WatchService watchService;
        try {
            watchService = FileSystems.getDefault().newWatchService();
            mediaTrackerService.watch(watchService, targetFolderList);
            /*
            * If provided with malformed paths in properties file watch service will automatically close.
            * */
            LOG.info("[ tracker ] closing...");
        } catch (IOException | InterruptedException e) {
            if (e instanceof  InterruptedException) {
                /*
                * In case of thread interruption.
                * */
                LOG.info("[ tracker ] closing...");
            } else {
                /*
                * In case of any other exception.
                * */
                LOG.error(e.getMessage(), e);
            }
        }
    }

}
