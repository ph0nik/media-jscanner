package runner;

import java.nio.file.Path;
import java.util.List;

public interface TrackerExecutor {

    void startTracker();

    void stopTracker();

    boolean trackerStatus();

    public boolean compareTargetList(List<Path> otherTargetList);
}
