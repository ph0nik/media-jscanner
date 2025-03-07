package model;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

class MediaQueryTest {

    @Test
    void getQueryFromFilename() {
        String filePath = "Seriale/The.Wire.S01.AC3.DVDRip.XviD-MEDiEVAL/The.Wire.S01E09.Game.Day.PROPER.AC3.DVDRip.XviD-MEDiEVAL/the.wire.s01e09.dvdrip.xvid-med.avi";
        String filePath2 = "The.Wire.S01.AC3.DVDRip.XviD-MEDiEVAL";
        Path path = Path.of(filePath);
        String fileName = path.getFileName().toString();
        String sub = fileName.substring(0, fileName.lastIndexOf(".")).replace(".", " ");
//        System.out.println(sub);
    }

}