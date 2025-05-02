package model.form;

import java.nio.file.Path;

public class SourcePathDto {

    private Path sourcePath;
    private boolean existing;

    public SourcePathDto(Path sourcePath) {
        this.sourcePath = sourcePath;
        this.existing = sourcePath.toFile().exists();
    }

    public Path getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(Path sourcePath) {
        this.sourcePath = sourcePath;
    }

    public boolean isExisting() {
        return existing;
    }

    public void setExisting(boolean existing) {
        this.existing = existing;
    }
}
