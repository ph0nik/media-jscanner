package util;


import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

class MediaFilterTest {

    private Path filePath;

    @BeforeEach
    void setUpFileSystem() {
        FileSystem fileSystem = Jimfs.newFileSystem(Configuration.windows());
        filePath = fileSystem.getPath("");
    }

    @Test
    public void validExtension() throws IOException {
        String fileName = "asd.mkv";
        Path file = Files.createFile(filePath.resolve(fileName));
        Assertions.assertTrue(MediaFilter.validateExtension(file.toString()));
    }

    @Test
    public void invalidExtension() throws IOException {
        String fileName = "info.txt";
        Path file = Files.createFile(filePath.resolve(fileName));
        Assertions.assertFalse(MediaFilter.validateExtension(file.toString()));
    }

    @Test
    public void extractFileName() {
        String filenameWithExt = "Army.of.Darkness.1992.Director's.Cut.Hybrid.1080p.BluRay.DTS.x264-IDE.mkv";
        String filenameOnly = "Army.of.Darkness.1992.Director's.Cut.Hybrid.1080p.BluRay.DTS.x264-IDE";
        Assertions.assertEquals(filenameOnly, MediaFilter.getFileName(filenameWithExt));
    }
}