package service;

import dao.MediaTrackerDao;
import dao.MediaTrackerDaoImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import scanner.MoviesFileScanner;
import util.CleanerService;
import util.CleanerServiceImpl;
import util.TextExtractTools;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MediaLinksServiceImplTest {

    private PropertiesService propertiesService;
    private MediaTrackerDao mediaTrackerDao;
    private CleanerService cleanerService;
    private MediaQueryService mediaQueryService;
    private MediaLinksServiceImpl mediaLinksService;
    private MoviesFileScanner moviesFileScanner;

    private FileService fileService;
    @BeforeEach
    void initService() {
        propertiesService = new PropertiesServiceImpl();
        mediaTrackerDao = new MediaTrackerDaoImpl();
        cleanerService = new CleanerServiceImpl();
        moviesFileScanner = new MoviesFileScanner();
        mediaQueryService = new MediaQueryService(mediaTrackerDao, moviesFileScanner, propertiesService);
        mediaLinksService = new MediaLinksServiceImpl();
    }

    @Test
    void getGroupName() {
        URL resourceAsStream = getClass().getClassLoader().getResource("movies_hd.txt");
        File testList = new File(resourceAsStream.getPath());
        try (Scanner sc = new Scanner(testList)){
            while (sc.hasNextLine()) {
                String temp = sc.nextLine();
                String groupName = TextExtractTools.getGroupName(temp);
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
                String s = TextExtractTools.checkForSpecialDescriptor(temp);
                System.out.println(s);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Test
    void checkCorrectNumberOfSpaces() {
        String fileName = "E:\\Filmy HD\\The.Strange.Vice.of.Mrs.Wardh.1971.RERIP.720p.BluRay.x264\\The.Strange.Vice.of.Mrs.Wardh.1971.RERIP.720p.BluRay.x264.mkv";
        String special = TextExtractTools.checkForSpecialDescriptor(fileName);
        String group = TextExtractTools.getGroupName(fileName);
        String specialWithGroup = (special + " " + group).trim();
        specialWithGroup = (specialWithGroup.trim().isEmpty()) ? "" : " - [" + specialWithGroup + "]";
        assertEquals(" - [720p x264]", specialWithGroup);

    }
}