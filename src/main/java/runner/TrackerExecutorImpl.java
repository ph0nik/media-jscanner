package runner;

import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Component
public class TrackerExecutorImpl implements TrackerExecutor {

    private Future<?> submit;
    private boolean status;
    private ExecutorService executorService;

    @Override
    public void startTracker() {
        executorService = Executors.newSingleThreadExecutor();
        status = true;
        submit = executorService.submit(new TrackerRunner());
    }

    @Override
    public void stopTracker() {
        if (submit != null && !submit.isCancelled()) {
            status = false;
            submit.cancel(true);
        }
    }

    @Override
    public boolean trackerStatus() {
        return status;
    }

}
