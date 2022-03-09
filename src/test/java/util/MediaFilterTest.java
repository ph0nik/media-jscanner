package util;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

class MediaFilterTest {

    @Test
    public void validExtension() {
        Path path = Path.of("asd.mkv");
        Assertions.assertTrue(MediaFilter.validateExtension(path.toString()));

    }

    @Test
    public void invalidExtension() {
        Path path = Path.of("info.txt");
        Assertions.assertFalse(MediaFilter.validateExtension(path.toString()));
    }
    
    public void checkForDir() throws IOException {
        File file = new File("./test-folder/2");
//        System.out.println(file.getCanonicalPath());
        Assertions.assertFalse(MediaFilter.checkForEmptyDirectory(file));
    }

    @Test
    public void extractFileName() {
        String filenameWithExt = "Army.of.Darkness.1992.Director's.Cut.Hybrid.1080p.BluRay.DTS.x264-IDE.mkv";
        String filenameOnly = "Army.of.Darkness.1992.Director's.Cut.Hybrid.1080p.BluRay.DTS.x264-IDE";
        Assertions.assertEquals(filenameOnly, MediaFilter.getFileName(filenameWithExt));
    }
}