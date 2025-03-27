package service;

import model.MediaLink;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;

@Service
public class LiveDataService {

    private Map<Path, List<UUID>> mediaQueriesByRootMap;
    private Future<List<MediaLink>> autoMatcherFutureTask;

    public LiveDataService() {
    }

    public Future<List<MediaLink>> getAutoMatcherFutureTask() {
        return autoMatcherFutureTask;
    }

    public void setAutoMatcherFutureTask(Future<List<MediaLink>> autoMatcherFutureTask) {
        this.autoMatcherFutureTask = autoMatcherFutureTask;
    }

    public Map<Path, List<UUID>> getMediaQueriesByRootMap() {
        return mediaQueriesByRootMap;
    }

    public void setMediaQueriesByRootMap(Map<Path, List<UUID>> mediaQueriesByRootMap) {
        this.mediaQueriesByRootMap = mediaQueriesByRootMap;
    }
}
