package util;

import java.time.LocalDateTime;

public class MediaLinksServiceLastUpdate {

    private static volatile LocalDateTime latestUpdateTime;

    public static void setLatestUpdateTime() {
        latestUpdateTime = LocalDateTime.now();
    }

    public static LocalDateTime getLatestUpdateTime() {
        return latestUpdateTime;
    }
}
