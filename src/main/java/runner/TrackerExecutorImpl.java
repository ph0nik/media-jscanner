package runner;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class TrackerExecutorImpl implements TrackerExecutor, ApplicationRunner {



    @Override
    public void startTracker() {

    }

    @Override
    public void stopTracker() {

    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.out.println("aplication runner");
    }
}
