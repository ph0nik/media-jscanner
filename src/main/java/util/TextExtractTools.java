package util;

import java.nio.file.Path;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextExtractTools {

    /*
     * Extracts release group name from file name if match for defined phrase is found.
     * Otherwise, returns empty string.
     * */
    public static String getGroupName(String path) {
        // "(?<![^cd\d])(-([a-zA-Z0-9]+))(\.\w+)?$"gm
        // "(?i)-(?!cd\d+)()([a-zA-Z0-9]+)(\.\w+)?$"gm
//        String regex = "-([a-zA-Z0-9]+)(\\.\\w+)?$";
        String regex = "(?i)\\W(?!cd\\d+)([a-zA-Z0-9]+)\\.\\w+$";
        Pattern p = Pattern.compile(regex);
        Matcher matcher = p.matcher(path);
        if (matcher.find() && matcher.groupCount() > 0) {
            // TODO exclude results as cd1 or cd2, cdx in general
            return matcher.group(1);
        }
        return "";
    }

    /*
     * Finds file name elements that indicate version of the movie or definition of video format.
     * If such element exists it's going to be extracted and returned formatted, in square brackets.
     * Otherwise, empty string is returned.
     * */
    public static String checkForSpecialDescriptor(String path) {
        Path fileName = Path.of(path).getFileName();
        String special2 = "(?i)((?:dir|inte|thea).+cut)|(dvdrip|tvrip|vhsrip)|(\\d{3,4}p)|(unrated|extended)|\\W(hdr)\\W";
        Pattern p2 = Pattern.compile(special2);
        Matcher matcher = p2.matcher(fileName.toString());
        SortedSet<String> sortedSet = new TreeSet<>();
        while (matcher.find()) {
            sortedSet.add(matcher.group()
                    .toLowerCase()
                    .replaceAll("[.]", " ")
                    .replaceAll("'", "")
                    .trim());
        }
        StringBuilder sb = new StringBuilder();
        for (String s : sortedSet) {
            sb.append(s).append(" ");
        }
        return sb.toString().trim();
    }

    /*
     * Extracts file extension.
     * */
    public static String getExtension(String filename) {
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    /*
    * Extracts imdb id from provided imdb link
    * */
    public static String getImdbIdFromLink(String imdbLink) {
        String imdbPattern = "^https:.+title\\/(tt\\d+)\\/";
        Pattern p = Pattern.compile(imdbPattern);
        Matcher matcher = p.matcher(imdbLink);
        if (matcher.find() && matcher.groupCount() > 0) return matcher.group(1);
        return "";

    }
    /*
     * Replaces all illegal characters within provided string with underscores
     * */
    public static String replaceIllegalCharacters(String title) {
        String illegalNames = "[#%&{}\\<>*?/$!\"+:@`|=]+";
        Pattern p = Pattern.compile(illegalNames);
        return p.matcher(title).replaceAll("_");
    }

}
