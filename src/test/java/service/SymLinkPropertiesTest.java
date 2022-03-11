package service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SymLinkPropertiesTest {

    private SymLinkProperties symLinkProperties;
    private final String defaultLinks = "./test-folder/movies-source/";
    private final String defaultTarget = "./test-folder/movies-target/";

    @BeforeEach
    void initProperties() {
        symLinkProperties = new SymLinkProperties();
    }

    @Test
    void getLinksFolder() {
        Path linksFolder = symLinkProperties.getLinksFolder();
        assertEquals(Path.of(defaultLinks), linksFolder);
    }

    @Test
    void getTargetFolder() {
        List<Path> targetFolderList = symLinkProperties.getTargetFolderList();
        assertTrue(targetFolderList.contains(Path.of(defaultTarget)));
    }

    @Test
    void setUserFolder() {
        String userPath = "d:\\movies\\";

        symLinkProperties.setLinksPath(Path.of(userPath));
        Path linksFolder = symLinkProperties.getLinksFolder();
        assertEquals(Path.of(userPath), linksFolder);
    }
}