#media-jscanner

This is tool designed to create extra layer between 
local video files collection and media management solutions 
such as Emby or Jellyfin. It uses two, user privided locations: 
**source** and **target** folder. 

**Target** folder is tha place where the video
files are being collected, program watches provided folder and all it's
subfolders and registers all changes within that location. For each file
that matches criteria for being media files, web search is performed.
The result is list of movie titles that match each element that have been found.

Program prompts user to choose correct title for each file, next based
on this data program creates symlinks of those files in **source** location.
Naming and organization in **source** folder is based of Jellyfin/Emby documentation,
to ensure that those tools will get the right data for each media file.

In case of original file being deleted media-jscanner will delete matching link and
all extra data that might be stored in the same folder (such as subtitles, posters, etc.).

Technologies used:
- tbd;