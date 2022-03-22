package runner;

import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Component
public class TrackerExecutorImpl implements TrackerExecutor {

    private Future<?> submit;
    private ExecutorService executorService;
    private List<Path> currentTargetFolderList;

    /*
    * Compare list of paths from current properties file with
    * configuration that was loaded while starting tracker
    * last time.
    * */
    public boolean compareTargetList(List<Path> otherTargetList) {
        return currentTargetFolderList.equals(otherTargetList);
    }

    @Override
    public void startTracker() {
        /*
        * Create new single thread executor
        * */
        executorService = Executors.newSingleThreadExecutor();
        /*
        * Create new tracker runner object
        * */
        TrackerRunner trackerRunner = new TrackerRunner();
        /*
        * Get list of target folders loaded by tracker runner
        * */
        currentTargetFolderList = trackerRunner.getTargetFolderList();
        /*
        * Run thread
        * */
        submit = executorService.submit(trackerRunner);
    }

    @Override
    public void stopTracker() {
        if (submit != null && !submit.isDone()) {
            submit.cancel(true);
        }
    }

    @Override
    public boolean trackerStatus() {
        return !submit.isDone();
    }

}
