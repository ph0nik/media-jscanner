package model.path;

import java.nio.file.Path;

// TODO remove and get rid of user defined
public class FilePath {

    private Path path;
    private boolean userDefined;

    public FilePath(Path path, boolean userDefined) {
        this.path = path;
        this.userDefined = userDefined;
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public boolean isUserDefined() {
        return userDefined;
    }

    public void setUserDefined(boolean userDefined) {
        this.userDefined = userDefined;
    }

    public boolean isExisting() {
        return path.toFile().exists();
    }

    @Override
    public String toString() {
        return "FilePath{" +
                "path=" + path +
                ", userDefined=" + userDefined +
                ", existing=" + isExisting() +
                '}';
    }
}
