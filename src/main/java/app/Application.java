package app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import runner.TrackerRunner;

@SpringBootApplication
public class Application {

    // TODO tracker trhows exception in thread
    // catch it and send message to controller
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
        Thread thread = new Thread(new TrackerRunner(args));
        thread.start();
    }
}
