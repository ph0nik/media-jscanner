package service;

import dao.MediaTrackerDao;
import dao.MediaTrackerDaoImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import util.CleanerService;
import util.CleanerServiceImpl;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.Scanner;

class MediaLinksServiceImplTest {

    private PropertiesService propertiesService;
    private MediaTrackerDao mediaTrackerDao;
    private CleanerService cleanerService;
    private MediaQueryService mediaQueryService;
    private MediaLinksServiceImpl mediaLinksService;

    @BeforeEach
    void initService() {
        propertiesService = new PropertiesServiceImpl();
        mediaTrackerDao = new MediaTrackerDaoImpl();
        cleanerService = new CleanerServiceImpl();
        mediaQueryService = new MediaQueryService(mediaTrackerDao, cleanerService);
        mediaLinksService = new MediaLinksServiceImpl(mediaTrackerDao, propertiesService, cleanerService, mediaQueryService);
    }

    @Test
    void getGroupName() {
        URL resourceAsStream = getClass().getClassLoader().getResource("movies_hd.txt");
        File testList = new File(resourceAsStream.getPath());
        try (Scanner sc = new Scanner(testList)){
            while (sc.hasNextLine()) {
                String temp = sc.nextLine();
                String groupName = mediaLinksService.getGroupName(temp);
                if (!groupName.isEmpty()) {
                    System.out.println(temp + " | " + groupName);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Test
    void checkForSpecialDescriptor() {
        URL resourceAsStream = getClass().getClassLoader().getResource("movies_hd.txt");
        File testList = new File(resourceAsStream.getPath());
        try (Scanner sc = new Scanner(testList)){
            while (sc.hasNextLine()) {
                String temp = sc.nextLine();
                String s = mediaLinksService.checkForSpecialDescriptor(temp);
                System.out.println(s);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Test
    void checkCorrectNumberOfSpaces() {
        String fileName = "E:\\Filmy HD\\The.Strange.Vice.of.Mrs.Wardh.1971.RERIP.720p.BluRay.x264\\The.Strange.Vice.of.Mrs.Wardh.1971.RERIP.720p.BluRay.x264.mkv";
        String special = mediaLinksService.checkForSpecialDescriptor(fileName);
        String group = mediaLinksService.getGroupName(fileName);
        String specialWithGroup = (special + " " + group).trim();
        specialWithGroup = (specialWithGroup.trim().isEmpty()) ? "" : " - [" + specialWithGroup + "]";
        System.out.println(specialWithGroup);
    }
}