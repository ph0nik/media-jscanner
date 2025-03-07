package util;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

public class CreateDummyFileTreeTest {

    @Test
    void readAndCreateFolderStructure() throws IOException {
        Path target = Path.of("E:\\Filmy SD\\");
        Path dummy = Path.of("R:\\media-jscanner-test\\movies-incoming\\");
        if (!dummy.toFile().exists()) {
            DummyStructureService dss = new DummyStructureService(target, dummy);
            dss.execute();
        } else {
            System.out.println("Dummy folder already exists");
        }

    }
}
