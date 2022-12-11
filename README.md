# media-jscanner

The tool designed to create additional layer between 
local video files collection and media management solutions 
such as Emby or Jellyfin. It helps user identify media before it gets
added to media collection.

It's based on user provided locations:
**links** and **target** folders. 

**Target** folders are source for media files, program will scan those folders and look for video files.

**Links** folder is where hard links are being created.

First, user needs to define both locations. All provided paths must be absolute paths and all of them must belong to the same volume.
In the ***new files*** tab ***scan*** button will initiate search for video files, which will return results list.
Then user can decide to either create new link for a given file or ignore it.
If there are multiple files in the same folder at the same level, user will be prompted to select
which of them are the parts of the same title. 

***Search*** button on each result will initiate web search for information about given video file. 
This will return list of movie titles that are possible matches for this file. In some cases additional 
search parameters are needed, so user may try search with custom query or movie title and year of production.

Next, user selects matching title from the list and based of this hard link is created using predefined naming pattern.
Current pattern matches Jellyfin requirements and is as follows:

```
/%movie_title% (%year_of_production%) [imdbid-%imdb_id%]/%movie_title% [%optional_info%].%extension%
```

Program will recognize some additional information like theatrical version or directors cut.

Program stores history of action on files, so it can ignore files that have been already linked or restore
original filename if necessary.

Technologies used:
- Spring Boot,
- Hibernate,
- H2 Database Engine,
- jsoup,
- Thymeleaf,
- Bootstrap.