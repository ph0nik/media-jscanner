import runner.MenuRunner;
import runner.TrackerRunner;

import java.util.concurrent.TimeUnit;

public class Main {

    /*
    * TODO
    *  1. scan files
    *  2. filter file names
    * 3. add matching files to db
    * 4. send notification
    * 5. select file to search
    * 6. send search request for file name
    * 7. prompt user with suggestions
    * 8. make selection and create file link with given data
    *
    * */

    // TODO use Daemon thread?
    public static void main(String[] args) {
        Thread tracker = new Thread(new TrackerRunner());
        Thread menu = new Thread(new MenuRunner(tracker));
        tracker.start();
        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
        menu.start();



    }

}
