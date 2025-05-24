package model.duplicates;

public class DuplicateMediaLinkDto {

    private String existingLinkFileName;
    private String existingParentFolder;
    private String newLinkFileName;
    private String newSourcePath;
    private String oldSourcePath;
    private long existingFileSize;
    private long newFileSize;

    public DuplicateMediaLinkDto(
            String existingLinkFileName,
            String existingParentFolder,
            String newSourcePath,
            String oldSourcePath,
            long existingFileSize,
            long newFileSize
    ) {
        this.existingLinkFileName = existingLinkFileName;
        this.existingParentFolder = existingParentFolder;
        this.newSourcePath = newSourcePath;
        this.oldSourcePath = oldSourcePath;
        this.existingFileSize = existingFileSize;
        this.newFileSize = newFileSize;
        this.newLinkFileName = existingLinkFileName;
    }

    public String getExistingParentFolder() {
        return existingParentFolder;
    }

    public String getNewLinkFileName() {
        return newLinkFileName;
    }

    public void setNewLinkFileName(String newLinkPath) {
        this.newLinkFileName = newLinkPath;
    }

    public String getExistingLinkFileName() {
        return existingLinkFileName;
    }

    public String getOriginalPath() {
        return newSourcePath;
    }

    public String getExistingFileSizeGb() {
        return getGigabytes(existingFileSize);
    }

    public long getExistingFileSize() {
        return existingFileSize;
    }

    public String getNewSourcePath() {
        return newSourcePath;
    }

    public String getOldSourcePath() {
        return oldSourcePath;
    }

    public String getNewFileSizeGb() {
        return getGigabytes(newFileSize);
    }

    public long getNewFileSize() {
        return newFileSize;
    }

    String getGigabytes(long bytes) {
        return String.format("%.2f GB", bytes / 1024.0 / 1024.0 / 1024.0);
    }

    @Override
    public String toString() {
        return "DuplicateMediaLinkDto{" +
                "existingLinkFileName='" + existingLinkFileName + '\'' +
                ", existingParentFolder='" + existingParentFolder + '\'' +
                ", newLinkFileName='" + newLinkFileName + '\'' +
                ", newSourcePath='" + newSourcePath + '\'' +
                ", oldSourcePath='" + oldSourcePath + '\'' +
                ", existingFileSize=" + existingFileSize +
                ", newFileSize=" + newFileSize +
                '}';
    }
}
