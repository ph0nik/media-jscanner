package model;

public class MediaStub {

    private Long stubId;
    private String title;
    private String format;
    private String version;
    private String description;

    public Long getStubId() {
        return stubId;
    }

    public void setStubId(Long stubId) {
        this.stubId = stubId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "MediaStub{" +
                "stubId=" + stubId +
                ", title='" + title + '\'' +
                ", format='" + format + '\'' +
                ", version='" + version + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
