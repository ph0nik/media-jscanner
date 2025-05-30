package service;

import app.config.EnvValidator;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.FileSystem;
import java.nio.file.Path;

@SpringBootTest(classes = {PropertiesService.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PropertiesServiceImplTest {

    static PropertiesService propertiesService;
    static FileSystem fileSystem;
    static String dataFolder = "data";
    static String dataFile = "userFolders.properties";
    static Path dataPath;

    @BeforeAll
    void init() {
        createFileSystem();
        propertiesService = new PropertiesServiceImpl(
                new EnvValidator(null),
                fileSystem,
                dataPath.toString());
    }

    static void createFileSystem() {
        fileSystem = Jimfs.newFileSystem(Configuration.windows());
        Path next = fileSystem.getRootDirectories().iterator().next();
        dataPath = next.resolve(dataFolder).resolve(dataFile);
    }

    @Test
    void someTest() {

    }
}