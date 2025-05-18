package model;

public class StatusDto {

    // links [ 0 / 0 ] -- ignored [ 0 / 0 ]
    private final int currentLinks;
    private final String newlyAddedLinks;
    private final int currentIgnore;
    private final String newlyAddedIgnore;

    public StatusDto(
            int currentLinks,
            int currentIgnore,
            String newlyAddedLinks,
            String newlyAddedIgnore
    ) {
        this.currentLinks = currentLinks;
        this.newlyAddedLinks = newlyAddedLinks;
        this.currentIgnore = currentIgnore;
        this.newlyAddedIgnore = newlyAddedIgnore;
    }

    public int getCurrentLinks() {
        return currentLinks;
    }

    public String getNewlyAddedLinks() {
        return newlyAddedLinks;
    }

    public int getCurrentIgnore() {
        return currentIgnore;
    }

    public String getNewlyAddedIgnore() {
        return newlyAddedIgnore;
    }
}
