package util;

import model.DeductedQuery;

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
        // TODO pass title and year to extract special descriptor
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

    /*
     * Checks if given path contains phrases that indicate bonus content
     * */
    public static boolean hasExtrasInName(String path) {
        Path of = Path.of(path);
        String regex = "(?i).+(interview|featurette|deleted)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(of.toString().toLowerCase());
        return matcher.find();
    }

    /*
     * Checks if given paths contains phrases "sample" or "trailer"
     * */
    public static boolean isSampleOrTrailer(String path) {
        String regex = "(?i).+(sample|trailer)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(path);
        return matcher.find();
    }

    /*
     * Extract movie title and production year from given path.
     * Files containing keyword sample or trailer are being ignored.
     * */
    public static DeductedQuery extractTitleAndYear(String path) {
        Path of = Path.of(path);
        Path fileName = of.getName(of.getNameCount() - 1);
        if (TextExtractTools.isSampleOrTrailer(path)) return null;
        String regex = "\\b^.+?\\d{4}\\b";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(fileName.toString());
        if (m.find()) {
            String group = m.group();
            String filtered = replaceIllegalCharacters(group);
            int i = filtered.length() - 4;
            int year = Integer.parseInt(filtered.substring(i));
            String title = filtered.substring(0, i).trim();
            return new DeductedQuery(title, year, path);
        }
        return null;
    }

    public static String extractTitleFromTvElement(String path) {
        Path fileName = Path.of(path).getFileName();
        String regex = "\\b^(.+?)([Ss]\\d{1,2}[eE]\\d{1,2}|\\d{1,2}x\\d{1,2})";
        Matcher m = Pattern.compile(regex)
                .matcher(fileName.toString());
        if (m.find() && m.groupCount() > 0) {
            return m.group(1).replace(".", " ");
        }
        return "";
    }

    public static int extractSeasonNumber(String path) {
        Path fileName = Path.of(path).getFileName();
        String regex = "\\b^.+?[Ss](?<s1>\\d{1,2})\\D*[Ee]\\d{1,2}|\\D+(?<s2>\\d{1,2})x\\d{1,2}";
        Matcher m = Pattern.compile(regex)
                .matcher(path);
        if (m.find() && m.groupCount() > 0) {
            if (m.group("s1") == null) return Integer.parseInt(m.group("s2"));
            return Integer.parseInt(m.group("s1"));
        }
        return -1;
    }

    public static int extractEpisodeNumber(String path) {
        String regex = "\\b^.+?[Ss]\\d{1,2}\\D*[Ee](?<s1>\\d{1,2})|\\D+\\d{1,2}x(?<s2>\\d{1,2})";
        Matcher m = Pattern.compile(regex)
                .matcher(path);
        if (m.find() && m.groupCount() > 0) {
            if (m.group("s1") == null) return Integer.parseInt(m.group("s2"));
            return Integer.parseInt(m.group("s1"));
        }
        return -1;
    }

}
