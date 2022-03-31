package app;

import runner.MenuRunner;
import runner.RunnerService;
import runner.TrackerRunner;

public class ConsoleRunner {

    public static void main(String[] args) {
        RunnerService runnerService = new RunnerService(new MenuRunner(), new TrackerRunner());
        runnerService.execute();
    }

}
