package ui;

import model.MediaLink;
import model.MediaQuery;
import model.QueryResult;
import service.MediaLinksService;

import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainMenu {

    private MediaLinksService mediaLinksService;
    private final Pattern p = Pattern.compile("^\\d+$");

    public MainMenu(MediaLinksService mls) {
        this.mediaLinksService = mls;
    }

    public void getMainMenu() {
        System.out.println(":: Main Menu ::");
        System.out.println("( 1 ) Show Query");
        System.out.println("( 2 ) Show Existing Links");
        System.out.println("( 3 ) Exit program");
        System.out.print("Select option: ");

        Scanner sc = new Scanner(System.in);
        String s = sc.nextLine();
        Matcher m = p.matcher(s);
        if (m.find()) {
            int selection = Integer.parseInt(s);
            if (selection == 1) getQueryMenu();
            if (selection == 2) getExistingLinks();
            if (selection == 3) return;
        } else {
            System.out.println("Wrong number or illegal character");
        }
        return;

    }

    public void getQueryMenu() {
        List<MediaQuery> allMediaQueries = mediaLinksService.getMediaQueryList();
        int index = 1;

        System.out.println(":: Media Query List ::");
        if (allMediaQueries.isEmpty()) {
            System.out.println("No queries found");
            getMainMenu();
        }
        for (MediaQuery mq : allMediaQueries) {
            System.out.println("( " + index + " ) " + mq.getFilePath());
            index++;
        }
        System.out.println("( " + index + " ) <- Return to main menu");
        System.out.println("Input media query number: ");
        boolean selectionStatus = true;
        while (selectionStatus) {
            Scanner sc = new Scanner(System.in);
            String s = sc.nextLine();
            Matcher m = p.matcher(s);
            if (m.find()) {
                int selection = Integer.parseInt(s) - 1;
                int max = allMediaQueries.size();
                if (selection >= 0 && selection < max) {
                    selectionStatus = false;
                    getResultsMenu(allMediaQueries.get(selection));
                }
                if (selection == max)
                    selectionStatus = false;
            } else {
                System.out.println("Wrong number or illegal character");
            }
        }
        getMainMenu();

    }

    public void getResultsMenu(MediaQuery mediaQuery) {
        List<QueryResult> queryResults = mediaLinksService.executeMediaQuery("", mediaQuery);
        int index = 1;
        System.out.println(":: Query Results Menu ::");
        System.out.println("Search Results for file [" + mediaQuery.getFilePath() + "]:");
        for (QueryResult qr : queryResults) {
            System.out.println("( " + index + " ) " + qr);
            index++;
        }
        System.out.println("( " + index + " ) Use custom query");
        index++;
        System.out.println("( " + index + " ) Go back to query list");
        boolean selectionStatus = true;
        while (selectionStatus) {
            System.out.print("Select matching element or option: ");
            Scanner sc = new Scanner(System.in);
            String s = sc.nextLine();
            Matcher m = p.matcher(s);
            if (m.find()) {
                int selection = Integer.parseInt(s) - 1;
                int max = queryResults.size() + 1;
                if (selection >= 0 && selection < max - 2) {
                    mediaLinksService.createSymLink(queryResults.get(selection));
                    selectionStatus = false;
                    getExistingLinks();
                }
                if (selection == max - 1) {
                    System.out.println("Input custom query for this file:");
                    selectionStatus = false;
                    s = sc.nextLine();
                    getCustomSearchMenu(s, mediaQuery);
                }
                if (selection == max) {
                    selectionStatus = false;
                }
            } else {
                System.out.println("Wrong number or illegal character");
            }
        }
        getQueryMenu();
    }

    public void getCustomSearchMenu(String customPhrase, MediaQuery mediaQuery) {
        List<QueryResult> queryResults = mediaLinksService.executeMediaQuery(customPhrase, mediaQuery);
        int index = 1;
        System.out.println(":: Custom Query Menu ::");
        System.out.println("Search results:");
        for (QueryResult qr : queryResults) {
            System.out.println("( " + index + " )" + qr);
            index++;
        }
        System.out.println("( " + index + " ) Go back to query list");
        boolean selectionStatus = true;
        while (selectionStatus) {
            System.out.print("Select matching element: ");
            Scanner sc = new Scanner(System.in);
            String s = sc.nextLine();
            Matcher m = p.matcher(s);
            if (m.find()) {
                int selection = Integer.parseInt(s) - 1;
                int max = queryResults.size();
                if (selection > 0 && selection <= max) {
                    mediaLinksService.createSymLink(queryResults.get(selection));
                    selectionStatus = false;
                    getExistingLinks();
                }
                if (selection == max) {
                    selectionStatus = false;
                }
            } else {
                System.out.println("Wrong number or illegal character");
            }
        }
        getQueryMenu();
    }

    public void getExistingLinks() {
        List<MediaLink> mediaLinks = mediaLinksService.getMediaLinks();
        System.out.println(":: Existing Links ::");
        if (mediaLinks.isEmpty()) System.out.println("No links found");
        for (MediaLink ml : mediaLinks) {
            System.out.println(ml);
        }
        getMainMenu();
    }


}
