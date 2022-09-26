# media-jscanner

This is tool designed to create additional layer between 
local video files collection and media management solutions 
such as Emby or Jellyfin. It uses two, user privided locations: 
**source** and **target** folder. 

**Target** folder is a place where the video
files are being collected, user may provide multiple locations for media files.
**Source** folder is a single place where symbolic links for all the media files are stored.

Program keeps data of all created symbolic links and files marked as ignored.
Every scan of target folders is filtered against current collection and files that match
given criteria are being presented to user as potential new additions.

After scan a web search is performed for each new media file. 
If the results are ambiguous or certain title is not found user can perform
custom search either using keywords related to the movie title or title and year of production.

User is presented with results and prompted to select correct movie title for given media file.
Based of user selection symbolic link is created.
File naming and organization in **source** folder is based of Jellyfin/Emby documentation,
to ensure that those tools will get the right data for each media file.

In case of original file being deleted media-jscanner will delete matching link and
all extra data that might be stored within the same folder (such as subtitles, posters, etc.).

By default, both paths are defined inside *mediafolder.properties* file.

mvn clean package -P dev

Technologies used:
- Spring Boot,
- Hibernate,
- H2 Database Engine,
- jsoup,
- Thymeleaf,
- Bootstrap.