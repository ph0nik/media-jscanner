package model;

public class LinksPathForm {

    private String linksFilePath;
    private boolean moveContent;

    public String getLinksFilePath() {
        return linksFilePath;
    }

    public void setLinksFilePath(String linksFilePath) {
        this.linksFilePath = linksFilePath;
    }

    public boolean isMoveContent() {
        return moveContent;
    }

    public void setMoveContent(boolean moveContent) {
        this.moveContent = moveContent;
    }

    @Override
    public String toString() {
        return "LinksPathForm{" +
                "linksFilePath='" + linksFilePath + '\'' +
                ", moveContent=" + moveContent +
                '}';
    }
}
