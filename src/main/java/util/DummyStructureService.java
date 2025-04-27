package util;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

public class DummyStructureService {
    private Path dummyRootPath;
    private Path targetFilePath;

    public DummyStructureService(Path target, Path dummy) {
        dummyRootPath = dummy;
        targetFilePath = target;
    }

    public DummyStructureService() {
        dummyRootPath = Path.of("");
        targetFilePath = Path.of("");
    }

    public void execute() throws IOException {
        Files.walkFileTree(targetFilePath, new SimpleFileReader());
    }

    public void listPathsFromFile(String input, String rootFolder) throws IOException {
        String fileContent = Files.readString(Path.of(input));
        int range = 20;
        for (String line : fileContent.split("\n")) {

            Files.createDirectories(Path.of(rootFolder).resolve(Path.of(line.substring(2)).getParent()));
            try {
                Files.createFile(Path.of(rootFolder).resolve(Path.of(line.substring(2))));
            } catch (IOException e) {
                System.out.println(e);
            }
        }
    }

    class SimpleFileReader implements FileVisitor<Path> {

        void createDummyFile(Path path) throws IOException {
            Path fileName = path.getFileName();
            Path parent = path.getParent();

            Path dummyParentPath;
            if (targetFilePath.getNameCount() == parent.getNameCount()) {
                dummyParentPath = dummyRootPath;
            } else {
                Path subpath = parent.subpath(targetFilePath.getNameCount(), parent.getNameCount());
                dummyParentPath = dummyRootPath.resolve(subpath);
            }
            System.out.println(dummyParentPath.resolve(fileName));
            Files.createDirectories(dummyParentPath);
            Files.createFile(dummyParentPath.resolve(fileName));
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            createDummyFile(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            return FileVisitResult.CONTINUE;
        }
    }
}
