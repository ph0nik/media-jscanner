//package runner;
//
//import java.util.concurrent.TimeUnit;
//
//public class RunnerService {
//
//    private Thread menuRunner;
//    private Thread trackerRunner;
//
//    public RunnerService(MenuRunner menuRunner, TrackerRunner trackerRunner) {
//        this.menuRunner = new Thread(menuRunner);
//        this.trackerRunner = new Thread(trackerRunner);
//    }
//
//    public void execute() {
//        Thread tracker = new Thread(trackerRunner);
//        Thread menu = new Thread(menuRunner);
//        // start tracker thread
//        tracker.start();
//        // wait for service to initialize
//        try {
//            TimeUnit.SECONDS.sleep(3);
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//            e.printStackTrace();
//        }
//        // run UI on second thread
//        menu.start();
//
//        try {
//            // wait for this thread to die
//            menu.join();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        // kill the tracker thread
//        tracker.interrupt();
//    }
//
//
//}
