package util;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

@Disabled
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


    void readPathsFromFile() throws IOException {
        String root = "R:\\media-jscanner-test\\local_movies";
        String path = "test-folder/local_movies.txt";
        DummyStructureService dss = new DummyStructureService();
        dss.listPathsFromFile(path, root);
    }
}
