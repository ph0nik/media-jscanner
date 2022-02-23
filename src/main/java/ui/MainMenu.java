package ui;

import dao.MediaTrackerDao;
import dao.MediaTrackerDaoImpl;
import linker.MediaLinksService;
import linker.MediaLinksServiceImpl;
import model.MediaLink;
import model.MediaQuery;
import model.QueryResult;

import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainMenu {

    private MediaLinksService mediaLinksService;
    private final Pattern p = Pattern.compile("^\\d$");

    public MainMenu(MediaTrackerDao dao) {
        mediaLinksService = new MediaLinksServiceImpl(dao);
    }

    //TODO tests
    public void getMainMenu() {

        System.out.println(":: Main Menu ::");
        System.out.println("1. Show Query");
        System.out.println("2. Show Existing Links");
        System.out.println("3. Exit");
        System.out.print("Select option: ");
        boolean running = true;
        while (running) {
            Scanner sc = new Scanner(System.in);
            String s = sc.nextLine();
            Matcher m = p.matcher(s);
            if (m.find()) {
                int selection = Integer.parseInt(s);
                if (selection == 1) getQueryMenu();
                if (selection == 2) getExistingLinks();
                if (selection == 3) running = false;
            } else {
                System.out.println("Wrong number or illegal character");
            }

        }
        // TODO make exiting program without printing anything below command
//        getMainMenu();


    }

    private void close(){
        return;
    }

    public void getQueryMenu() {
        List<MediaQuery> allMediaQueries = mediaLinksService.getMediaQueryList();

        System.out.println(":: Media Query List ::");
        int index = 0;
        if (allMediaQueries.isEmpty()) {
            System.out.println("No queries found" +
                    "");
            getMainMenu();
        }
        for (MediaQuery mq : allMediaQueries) {
            System.out.println(index++);
            System.out.println(mq);
        }
        while (true) {
            Scanner sc = new Scanner(System.in);
            System.out.println("Input media query number: ");
            String s = sc.nextLine();
            Matcher m = p.matcher(s);
            if (m.find()) {
                int selection = Integer.parseInt(s);

                if (selection >= 0 && selection < allMediaQueries.size()) {
                    getResultsMenu(allMediaQueries.get(selection));
                }
            }
            System.out.println("Wrong number or illegal character");

        }
    }

    //TODO manage empty lists
    public void getResultsMenu(MediaQuery mediaQuery) {
        List<QueryResult> queryResults = mediaLinksService.executeMediaQuery("", mediaQuery);
        int selection = 0;
        System.out.println(":: Query Results Menu ::");
        System.out.println("Search Results for file [" + mediaQuery.getFilePath() + "]:");
        for (QueryResult qr : queryResults) {
            System.out.print("( " + selection + " ) ");
            System.out.println(qr);
            selection++;
        }
        System.out.print("( " + selection + " ) ");
        System.out.println("Custom query");
        while (true) {
            System.out.print("Select matching element or enter " + selection + "for custom query: ");
            Scanner sc = new Scanner(System.in);
            String s = sc.nextLine();
            Matcher m = p.matcher(s);
            if (m.find()) {
                int input = Integer.parseInt(s);
                if (input >= 0 && input < selection) {
                    mediaLinksService.createSymLink(queryResults.get(input));
                    getExistingLinks();
                }
                if (input == selection) {
                    System.out.println("Input custom query for this file:");
                    s = sc.nextLine();

                    getCustomSearchMenu(s, mediaQuery);
                }
            }
            System.out.println("Wrong number or illegal character");

        }

    }

    public void getCustomSearchMenu(String customPhrase, MediaQuery mediaQuery) {
        List<QueryResult> queryResults = mediaLinksService.executeMediaQuery(customPhrase, mediaQuery);
        int selection = 0;
        System.out.println(":: Custom Query Menu ::");
        System.out.println("Search results:");
        for (QueryResult qr : queryResults) {
            System.out.print(selection++);
            System.out.println(qr);
        }
        while (true) {
            Scanner sc = new Scanner(System.in);
            System.out.print("Select matching element: ");
            String s = sc.nextLine();
            Matcher m = p.matcher(s);
            if (m.find()) {
                int input = Integer.parseInt(s);

                if (input >= 0 && input < selection) {
                    mediaLinksService.createSymLink(queryResults.get(input));
                    getExistingLinks();
                }
            }
            System.out.println("Wrong number or illegal character");

        }
    }

    public void getExistingLinks() {
        List<MediaLink> mediaLinks = mediaLinksService.getMediaLinks();
        System.out.println(":: Existing Links ::");
        for (MediaLink ml : mediaLinks) {
            System.out.println(ml);
        }
        getMainMenu();

    }

    public static void main(String[] args) {
        MediaTrackerDao dao = new MediaTrackerDaoImpl();
        MainMenu menu = new MainMenu(dao);
        menu.getMainMenu();
    }


}
