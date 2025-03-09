package dao;

import service.exceptions.MissingFolderOrFileException;

import java.io.IOException;
import java.nio.file.Path;

public interface DatabaseMigrationService {

    public String backupDatabase(Path backupFolder) throws IOException, MissingFolderOrFileException;

    public void restoreDatabase(Path backupFolder, String backupFileName) throws IOException, MissingFolderOrFileException;
}
