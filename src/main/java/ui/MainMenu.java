package ui;

import model.MediaLink;
import model.MediaQuery;
import model.QueryResult;
import service.MediaLinksService;

import java.util.List;
import java.util.Scanner;

public class MainMenu {

    private MediaLinksService mediaLinksService;
    //    private final Pattern p = Pattern.compile("^\\d+$");
    private List<QueryResult> queryResults;

    public MainMenu(MediaLinksService mls) {
        this.mediaLinksService = mls;
        queryResults = List.of();
    }

    /*
    * Show main menu and prompt user
    * */
    public void getMainMenu() {
        System.out.println(":: Main Menu ::");
        System.out.println("( 1 ) Show Query");
        System.out.println("( 2 ) Show Existing Links");
        System.out.println("( 3 ) Exit program");
        System.out.print("Select option: ");

        Scanner sc = new Scanner(System.in);
        if (sc.hasNextInt()) {
            int s = Integer.parseInt(sc.nextLine());
            if (s == 1) getQueryMenu();
            if (s == 2) getExistingLinks();
            if (s == 3) return;
        } else {
            System.out.println("Wrong number or illegal character");
            getMainMenu();
        }
        return;
    }

    /*
    * Show elements awaiting in the queue and let user select
    * file to process
    * */
    public void getQueryMenu() {
        List<MediaQuery> allMediaQueries = mediaLinksService.getMediaQueryList();
        System.out.println(":: Media Query List ::");
        if (allMediaQueries.isEmpty()) {
            System.out.println("No queries found");
            getMainMenu();
        }

        int index = 1;
        for (MediaQuery mq : allMediaQueries) {
            System.out.println("( " + index + " ) " + mq.getFilePath());
            index++;
        }
        System.out.println("( " + index + " ) <- Return to main menu");
        System.out.println("Input media query number: ");

        int max = allMediaQueries.size();
        Scanner sc = new Scanner(System.in);
        if (sc.hasNextInt()) {
            int s = Integer.parseInt(sc.nextLine()) - 1;
            if (s >= 0 && s < max) {
                getResultsMenu(allMediaQueries.get(s));
            } else if (s == max) {
                getMainMenu();
            } else {
                System.out.println("Wrong number or illegal character");
                getQueryMenu();
            }
        } else {
            System.out.println("Wrong number or illegal character");
            getQueryMenu();
        }
    }

    /*
    * For selected file perform online search for matching titles.
    * Present returned results and prompt user to select which title
    * will be used to create symlink.
    * */
    public void getResultsMenu(MediaQuery mediaQuery) {
        if (queryResults.isEmpty()) {
            queryResults = mediaLinksService.executeMediaQuery("", mediaQuery);
        }
        int index = 1;
        System.out.println(":: Query Results Menu ::");
        System.out.println("Search Results for file [ " + mediaQuery.getFilePath() + " ]:");
        for (QueryResult qr : queryResults) {
            System.out.println("( " + index + " ) " + qr);
            index++;
        }
        System.out.println("( " + index + " ) Use custom query");
        index++;
        System.out.println("( " + index + " ) Go back to query list");

        int max = queryResults.size() + 1;
        System.out.print("Select matching element or option: ");
        Scanner sc = new Scanner(System.in);
        if (sc.hasNextInt()) {
            int s = Integer.parseInt(sc.nextLine()) - 1;
            if (s >= 0 && s < max - 2) {
                mediaLinksService.createSymLink(queryResults.get(s));
                queryResults = List.of();
                getExistingLinks();
            } else if (s == max - 1) {
                System.out.println("Input custom query for this file: ");
                String custom = sc.nextLine();
                queryResults = List.of();
                getCustomSearchMenu(custom, mediaQuery);
            } else if (s == max) {
                queryResults = List.of();
                getQueryMenu();
            } else {
                System.out.println("Wrong number or illegal character");
                getResultsMenu(mediaQuery);
            }
        } else {
            System.out.println("Wrong number or illegal character");
            getResultsMenu(mediaQuery);
        }
    }

    /*
    * Perform online search with provided custom query and present user
    * with results.
    * Prompt user to select title which will be used to create symlink.
    * */
    public void getCustomSearchMenu(String customPhrase, MediaQuery mediaQuery) {
        if (queryResults.isEmpty()) {
            queryResults = mediaLinksService.executeMediaQuery(customPhrase, mediaQuery);
        }
        int index = 1;
        System.out.println(":: Custom Query Menu ::");
        System.out.println("Search results:");
        for (QueryResult qr : queryResults) {
            System.out.println("( " + index + " )" + qr);
            index++;
        }
        System.out.println("( " + index + " ) Go back to query list");

        int max = queryResults.size();
        System.out.print("Select matching element or option: ");
        Scanner sc = new Scanner(System.in);
        if (sc.hasNextInt()) {
            int s = Integer.parseInt(sc.nextLine()) - 1;
            if (s >= 0 && s < max) {
                mediaLinksService.createSymLink(queryResults.get(s));
                queryResults = List.of();
                getExistingLinks();
            } else if (s == max) {
                queryResults = List.of();
                getQueryMenu();
            } else {
                System.out.println("Wrong number or illegal character");
                getCustomSearchMenu(customPhrase, mediaQuery);
            }
        } else {
            System.out.println("Wrong number or illegal character");
            getCustomSearchMenu(customPhrase, mediaQuery);
        }
    }

    /*
    * Show all existing symlinks.
    *
    * */
    public void getExistingLinks() {
        List<MediaLink> mediaLinks = mediaLinksService.getMediaLinks();
        System.out.println(":: Existing Links ::");
        if (mediaLinks.isEmpty()) {
            System.out.println("No links found");
        } else {
            for (MediaLink ml : mediaLinks) {
                System.out.println(ml);
            }
        }
        getMainMenu();
    }


}
