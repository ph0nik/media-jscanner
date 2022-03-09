import runner.MenuRunner;
import runner.RunnerService;
import runner.TrackerRunner;

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


    public static void main(String[] args) {
        if (args.length == 0)
            System.out.println("You need to provide at least one path");
        else {
            TrackerRunner trackerRunner = new TrackerRunner(args);
            MenuRunner menuRunner = new MenuRunner();
            RunnerService runnerService = new RunnerService(menuRunner, trackerRunner);
            runnerService.execute();
        }
    }

}
