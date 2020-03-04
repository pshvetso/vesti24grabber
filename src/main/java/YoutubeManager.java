import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.util.Joiner;
import com.google.api.services.samples.youtube.cmdline.Auth;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;
import com.google.common.collect.Lists;

import java.io.*;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by Pub on 19.03.2018.
 */
public class YoutubeManager {
    private static final Logger log = Logger.getLogger("YoutubeManager");

    /**
     * Define a global variable that specifies the MIME type of the video
     * being uploaded.
     */
    private static final String VIDEO_FILE_FORMAT = "video/*";

    /**
     * Define a global variable that specifies the MIME type of the image
     * being uploaded.
     */
    private static final String IMAGE_FILE_FORMAT = "image/jpeg";

    /**
     * Application name.
     */
    private static final String APPLICATION_NAME = "youtube-manager";

    protected static final String[] CHANNEL_ID = {  "UCzgWe67JoVHlcg8uRhtRAxQ",     // Россия Сегодня
                                                    "UCwoeVRmTU58XbxStt2uqPQg",     // Россия Live
                                                    "UCENs0Cuo7yqoAdlFFvgHGUA"};    // Факты 24


    protected static int currentChannel = 0;

    /**
     * Define a global instance of a Youtube object, which will be used
     * to make YouTube Data API requests.
     */
    private YouTube youtube;

    public YoutubeManager() throws IOException {
        super();
        this.auth(CHANNEL_ID[currentChannel]);
    }

    protected void auth(String credentialDatastore) throws IOException {
        //Необходимо выводить текущий аккаунт, т.к. youtube может потребовать авторизоваться в браузере
        log.info("Authorizing: " + credentialDatastore);
        List<String> scopes = Lists.newArrayList("https://www.googleapis.com/auth/youtube");

        // Authorize the request.
        Credential credential = Auth.authorize(scopes, credentialDatastore);

        // This object is used to make YouTube Data API requests.
        youtube = new YouTube.Builder(Auth.HTTP_TRANSPORT, Auth.JSON_FACTORY, credential).setApplicationName(
                APPLICATION_NAME).build();
    }

    /**
     * Upload the user-selected video to the user's YouTube channel. The code
     * looks for the video in the application's project folder and uses OAuth
     * 2.0 to authorize the API request.
     *
     * @param video video data for download.
     */
    public void UploadVideo(VideoData video, String adText) throws GoogleJsonResponseException {

        // This OAuth 2.0 access scope allows an application to upload files
        // to the authenticated user's YouTube channel, but doesn't allow
        // other types of access.
        //List<String> scopes = Lists.newArrayList("https://www.googleapis.com/auth/youtube.upload");

        try {
            log.info("Uploading: " + video);

            // Add extra information to the video before uploading.
            Video videoObjectDefiningMetadata = new Video();

            // Set the video to be publicly visible. This is the default
            // setting. Other supporting settings are "unlisted" and "private."
            VideoStatus status = new VideoStatus();
            status.setPrivacyStatus("public");
            videoObjectDefiningMetadata.setStatus(status);

            // Most of the video's metadata is set on the VideoSnippet object.
            VideoSnippet snippet = new VideoSnippet();

            // This code uses a Calendar instance to create a unique name and
            // description for test purposes so that you can easily upload
            // multiple files. You should remove this code from your project
            // and use your own standard names instead.
            Calendar cal = Calendar.getInstance();
            snippet.setTitle(video.getTitle());
            snippet.setDescription(String.format("%s\n\n%s", adText, video.getDescr()));

            // Set the keyword tags that you want to associate with the video.
            snippet.setTags(video.getTags());

            // Add the completed snippet object to the video resource.
            videoObjectDefiningMetadata.setSnippet(snippet);

            InputStreamContent mediaContent = new InputStreamContent(VIDEO_FILE_FORMAT,
                    new FileInputStream(new File(Main.ENCODED_VIDEO_FILENAME)));

            Video returnedVideo = null;
            while (true) {
                // Insert the video. The command sends three arguments. The first
                // specifies which information the API request is setting and which
                // information the API response should return. The second argument
                // is the video resource that contains metadata about the new video.
                // The third argument is the actual video content.
                YouTube.Videos.Insert videoInsert;
                try {
                    videoInsert = youtube.videos()
                            .insert("snippet,statistics,status", videoObjectDefiningMetadata, mediaContent);
                } catch (IOException e) {
                    System.err.println("IOException: " + e.getMessage());
                    e.printStackTrace();
                    break;
                }

                // Set the upload type and add an event listener.
                MediaHttpUploader uploader = videoInsert.getMediaHttpUploader();

                // Indicate whether direct media upload is enabled. A value of
                // "True" indicates that direct media upload is enabled and that
                // the entire media content will be uploaded in a single request.
                // A value of "False," which is the default, indicates that the
                // request will use the resumable media upload protocol, which
                // supports the ability to resume an upload operation after a
                // network interruption or other transmission failure, saving
                // time and bandwidth in the event of network failures.
                uploader.setDirectUploadEnabled(false);

                MediaHttpUploaderProgressListener progressListener = new MediaHttpUploaderProgressListener() {
                    public void progressChanged(MediaHttpUploader uploader) throws IOException {
                        switch (uploader.getUploadState()) {
                            case INITIATION_STARTED:
                                log.info("Initiation Started");
                                break;
                            case INITIATION_COMPLETE:
                                log.info("Initiation Completed");
                                break;
                            case MEDIA_IN_PROGRESS:
                                log.info("Upload in progress");
                                log.info("Upload percentage: "
                                        + uploader.getNumBytesUploaded());
                                //log.info("mediaContent.getLength() " + mediaContent.getLength());
                                break;
                            case MEDIA_COMPLETE:
                                log.info("Upload Completed!");
                                break;
                            case NOT_STARTED:
                                log.info("Upload Not Started!");
                                break;
                        }
                    }
                };
                uploader.setProgressListener(progressListener);

                try {
                    // Call the API and upload the video.
                    returnedVideo = videoInsert.execute();
                    break;
                } catch (GoogleJsonResponseException e) {
                    System.err.println("GoogleJsonResponseException code: " + e.getDetails().getCode() + " : "
                            + e.getDetails().getMessage());
                    e.printStackTrace();
                    //System.out.println(e.getDetails().getErrors().get(0).getReason().equals("dailyLimitExceeded"));
                    throw e;
                    //break;
                } catch (SocketTimeoutException e) {
                    System.err.println("SocketTimeoutException: " + e.getMessage());
                    e.printStackTrace();
                    continue;
                } catch (IOException e) {
                    System.err.println("IOException: " + e.getMessage());
                    e.printStackTrace();
                    continue;
                }
            }
            ;

            video.setVideoId(returnedVideo.getId());
            video.setAccount(CHANNEL_ID[currentChannel]);

            // Print data about the newly inserted video from the API response.
            log.info("\n================== Returned Video ==================\n");
            log.info("  - Id: " + returnedVideo.getId());
            log.info("  - Title: " + returnedVideo.getSnippet().getTitle());
            log.info("  - Tags: " + returnedVideo.getSnippet().getTags());
            log.info("  - Privacy Status: " + returnedVideo.getStatus().getPrivacyStatus());
            log.info("  - Video Count: " + returnedVideo.getStatistics().getViewCount());

        } catch (FileNotFoundException e) {
            log.info("SocketTimeoutException: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void UploadThumbnail(VideoData video) {
        // This OAuth 2.0 access scope allows for full read/write access to the
        // authenticated user's account.

        while (true)
            try {
                // Authorize the request.
                //Credential credential = Auth.authorize(scopes, "uploadthumbnail");

                // This object is used to make YouTube Data API requests.
            /*youtube = new YouTube.Builder(Auth.HTTP_TRANSPORT, Auth.JSON_FACTORY, credential).setApplicationName(
                    "youtube-cmdline-uploadthumbnail-sample").build();*/

                // Prompt the user to enter the video ID of the video being updated.
                String videoId = video.getVideoId();
                log.info("You chose " + videoId + " to upload a thumbnail.");

                // Prompt the user to specify the location of the thumbnail image.
                File imageFile = new File(Main.THUMB_FILENAME);
                //log.info("You chose " + imageFile + " to upload.");

                // Create an object that contains the thumbnail image file's
                // contents.
                InputStreamContent mediaContent = new InputStreamContent(
                        IMAGE_FILE_FORMAT, new BufferedInputStream(new FileInputStream(imageFile)));
                mediaContent.setLength(imageFile.length());

                // Create an API request that specifies that the mediaContent
                // object is the thumbnail of the specified video.
                YouTube.Thumbnails.Set thumbnailSet = youtube.thumbnails().set(videoId, mediaContent);

                // Set the upload type and add an event listener.
                MediaHttpUploader uploader = thumbnailSet.getMediaHttpUploader();

                // Indicate whether direct media upload is enabled. A value of
                // "True" indicates that direct media upload is enabled and that
                // the entire media content will be uploaded in a single request.
                // A value of "False," which is the default, indicates that the
                // request will use the resumable media upload protocol, which
                // supports the ability to resume an upload operation after a
                // network interruption or other transmission failure, saving
                // time and bandwidth in the event of network failures.
                uploader.setDirectUploadEnabled(false);

                // Set the upload state for the thumbnail image.
                MediaHttpUploaderProgressListener progressListener = new MediaHttpUploaderProgressListener() {
                    @Override
                    public void progressChanged(MediaHttpUploader uploader) throws IOException {
                        switch (uploader.getUploadState()) {
                            // This value is set before the initiation request is
                            // sent.
                            case INITIATION_STARTED:
                                log.info("Initiation Started");
                                break;
                            // This value is set after the initiation request
                            //  completes.
                            case INITIATION_COMPLETE:
                                log.info("Initiation Completed");
                                break;
                            // This value is set after a media file chunk is
                            // uploaded.
                            case MEDIA_IN_PROGRESS:
                                log.info("Upload in progress");
                                log.info("Upload percentage: " + uploader.getProgress());
                                break;
                            // This value is set after the entire media file has
                            //  been successfully uploaded.
                            case MEDIA_COMPLETE:
                                log.info("Upload Completed!");
                                break;
                            // This value indicates that the upload process has
                            //  not started yet.
                            case NOT_STARTED:
                                log.info("Upload Not Started!");
                                break;
                        }
                    }
                };
                uploader.setProgressListener(progressListener);

                // Upload the image and set it as the specified video's thumbnail.
                ThumbnailSetResponse setResponse = thumbnailSet.execute();

                // Print the URL for the updated video's thumbnail image.
                log.info("\n================== Uploaded Thumbnail ==================\n");
                log.info("  - Url: " + setResponse.getItems().get(0).getDefault().getUrl());

                break;
            } catch (GoogleJsonResponseException e) {
                System.err.println("GoogleJsonResponseException code: " + e.getDetails().getCode() + " : "
                        + e.getDetails().getMessage());
                e.printStackTrace();
                continue;
            } catch (IOException e) {
                System.err.println("IOException: " + e.getMessage());
                e.printStackTrace();
                continue;
            }

    }

    public PlaylistListResponse ListPlaylists(String channelId) {
        //YouTube youtube = getYouTubeService();

        try {
            HashMap<String, String> parameters = new HashMap<>();
            parameters.put("part", "snippet,contentDetails");
            parameters.put("channelId", channelId);
            parameters.put("maxResults", "50");

            YouTube.Playlists.List playlistsListByChannelIdRequest = youtube.playlists().list(parameters.get("part").toString());
            if (parameters.containsKey("channelId") && parameters.get("channelId") != "") {
                playlistsListByChannelIdRequest.setChannelId(parameters.get("channelId").toString());
            }

            if (parameters.containsKey("maxResults")) {
                playlistsListByChannelIdRequest.setMaxResults(Long.parseLong(parameters.get("maxResults").toString()));
            }

            PlaylistListResponse response = playlistsListByChannelIdRequest.execute();
            //log.info(response.toPrettyString());
            return response;

        } catch (GoogleJsonResponseException e) {
            e.printStackTrace();
            System.err.println("There was a service error: " + e.getDetails().getCode() + " : " + e.getDetails().getMessage());
        } catch (Throwable t) {
            t.printStackTrace();
        }

        return null;
    }

    public String isPlaylistExists(String channelId, String title) {
        PlaylistListResponse response = ListPlaylists(channelId);

        for (Playlist list : response.getItems()) {
            if (list.getSnippet().getTitle().equals(title)) {
                return list.getId();
            }
        }

        return null;
    }

    /**
     * Create a playlist and add it to the authorized account.
     */
    public String insertPlaylist(String title, String descr) throws IOException {

        // This code constructs the playlist resource that is being inserted.
        // It defines the playlist's title, description, and privacy status.
        PlaylistSnippet playlistSnippet = new PlaylistSnippet();
        playlistSnippet.setTitle(title);
        playlistSnippet.setDescription(descr);
        PlaylistStatus playlistStatus = new PlaylistStatus();
        playlistStatus.setPrivacyStatus("public");

        Playlist youTubePlaylist = new Playlist();
        youTubePlaylist.setSnippet(playlistSnippet);
        youTubePlaylist.setStatus(playlistStatus);

        // Call the API to insert the new playlist. In the API call, the first
        // argument identifies the resource parts that the API response should
        // contain, and the second argument is the playlist being inserted.
        YouTube.Playlists.Insert playlistInsertCommand =
                youtube.playlists().insert("snippet,status", youTubePlaylist);
        Playlist playlistInserted = playlistInsertCommand.execute();

        // Print data from the API response and return the new playlist's
        // unique playlist ID.
        log.info("New Playlist name: " + playlistInserted.getSnippet().getTitle());
        log.info(" - Privacy: " + playlistInserted.getStatus().getPrivacyStatus());
        log.info(" - Description: " + playlistInserted.getSnippet().getDescription());
        log.info(" - Posted: " + playlistInserted.getSnippet().getPublishedAt());
        log.info(" - Channel: " + playlistInserted.getSnippet().getChannelId() + "\n");
        return playlistInserted.getId();

    }

    /**
     * Create a playlist item with the specified video ID and add it to the
     * specified playlist.
     *
     * @param playlistId assign to newly created playlistitem
     * @param videoId    YouTube video id to add to playlistitem
     */
    public String insertPlaylistItem(String playlistId, String videoId, String snippetTitle) throws IOException {

        // Define a resourceId that identifies the video being added to the
        // playlist.
        ResourceId resourceId = new ResourceId();
        resourceId.setKind("youtube#video");
        resourceId.setVideoId(videoId);

        // Set fields included in the playlistItem resource's "snippet" part.
        PlaylistItemSnippet playlistItemSnippet = new PlaylistItemSnippet();
        playlistItemSnippet.setTitle(snippetTitle);
        playlistItemSnippet.setPlaylistId(playlistId);
        playlistItemSnippet.setResourceId(resourceId);

        // Create the playlistItem resource and set its snippet to the
        // object created above.
        PlaylistItem playlistItem = new PlaylistItem();
        playlistItem.setSnippet(playlistItemSnippet);

        // Call the API to add the playlist item to the specified playlist.
        // In the API call, the first argument identifies the resource parts
        // that the API response should contain, and the second argument is
        // the playlist item being inserted.
        YouTube.PlaylistItems.Insert playlistItemsInsertCommand =
                youtube.playlistItems().insert("snippet,contentDetails", playlistItem);
        PlaylistItem returnedPlaylistItem = playlistItemsInsertCommand.execute();

        // Print data from the API response and return the new playlist
        // item's unique playlistItem ID.

        log.info("New PlaylistItem name: " + returnedPlaylistItem.getSnippet().getTitle());
        log.info(" - Video id: " + returnedPlaylistItem.getSnippet().getResourceId().getVideoId());
        log.info(" - Posted: " + returnedPlaylistItem.getSnippet().getPublishedAt());
        log.info(" - Channel: " + returnedPlaylistItem.getSnippet().getChannelId());
        return returnedPlaylistItem.getId();

    }

    public void deleteCopyrighted() {
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("part", "contentDetails");
        //parameters.put("id", CHANNEL_ID[currentChannel]);

        YouTube.Channels.List channelRequest = null;
        try {
            channelRequest = youtube.channels().list(parameters.get("part"));
            if (parameters.containsKey("id") && parameters.get("id") != "") {
                channelRequest.setId(parameters.get("id"));
            }

            //channelsListByIdRequest.setOnBehalfOfContentOwner(CHANNEL_ID[currentChannel]);
            channelRequest.setMine(true);
            channelRequest.setFields("items/contentDetails,nextPageToken,pageInfo");

            ChannelListResponse response = channelRequest.execute();
            System.out.println(response);

            List<Channel> channelsList = response.getItems();

            if (channelsList != null) {
                // The user's default channel is the first item in the list.
                // Extract the playlist ID for the channel's videos from the
                // API response.
                String uploadPlaylistId =
                        channelsList.get(0).getContentDetails().getRelatedPlaylists().getUploads();

                // Retrieve the playlist of the channel's uploaded videos.
                YouTube.PlaylistItems.List playlistItemRequest =
                        youtube.playlistItems().list("id,contentDetails,snippet");
                playlistItemRequest.setPlaylistId(uploadPlaylistId);

                // Only retrieve data used in this application, thereby making
                // the application more efficient. See:
                // https://developers.google.com/youtube/v3/getting-started#partial
                playlistItemRequest.setFields(
                        "items(contentDetails/videoId,snippet/title,snippet/publishedAt),nextPageToken,pageInfo");

                playlistItemRequest.setMaxResults(50L);

                String nextToken = "";

                // Call the API one or more times to retrieve all items in the
                // list. As long as the API response returns a nextPageToken,
                // there are still more items to retrieve.
                do {
                    playlistItemRequest.setPageToken(nextToken);
                    PlaylistItemListResponse playlistItemResult = playlistItemRequest.execute();

                    //playlistItemList.addAll(playlistItemResult.getItems());

                    nextToken = playlistItemResult.getNextPageToken();

                    List<String> videoIds = new ArrayList<>();
                    // Merge video IDs
                    for (PlaylistItem searchResult : playlistItemResult.getItems()) {
                        videoIds.add(searchResult.getContentDetails().getVideoId());
                    }
                    Joiner stringJoiner = Joiner.on(',');
                    String videoId = stringJoiner.join(videoIds);
                    videoId = "xr7mKi7KrAU";

                    // Call the YouTube Data API's youtube.videos.list method to
                    // retrieve the resources that represent the specified videos.
                    // snippet, contentDetails
                    YouTube.Videos.List listVideosRequest = youtube.videos().list
                            //("snippet, contentDetails, recordingDetails, status, fileDetails, topicDetails, statistics, liveStreamingDetails, localizations, player, processingDetails, suggestions, topicDetails")
                            ("snippet, contentDetails")
                            .setId(videoId);
                    VideoListResponse listResponse = listVideosRequest.execute();

                    System.out.println(listResponse.getItems());

                    List<Video> videoList = listResponse.getItems();

                    if (videoList != null) {
                        prettyPrint(videoList.iterator());
                        deleteVideos(videoList.iterator());
                    }
                    //break;
                } while (nextToken != null);

                // Prints information about the results.
                //prettyPrint(playlistItemList.size(), playlistItemList.iterator());
            }

        } catch (GoogleJsonResponseException e) {
            e.printStackTrace();
            System.err.println("There was a service error: " + e.getDetails().getCode() + " : "
                    + e.getDetails().getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
     * Print information about all of the items in the playlist.
     *
     * @param size size of list
     *
     * @param iterator of Playlist Items from uploaded Playlist
     */
    private static void prettyPrint(int size, Iterator<PlaylistItem> playlistEntries) {
        System.out.println("=============================================================");
        System.out.println("\t\tTotal Videos Uploaded: " + size);
        System.out.println("=============================================================\n");

        while (playlistEntries.hasNext()) {
            PlaylistItem playlistItem = playlistEntries.next();
            System.out.println(" video name  = " + playlistItem.getSnippet().getTitle());
            System.out.println(" video id    = " + playlistItem.getContentDetails().getVideoId());
            System.out.println(" upload date = " + playlistItem.getSnippet().getPublishedAt());
            System.out.println("\n-------------------------------------------------------------\n");
        }
    }

    /*
     * Prints out all results in the Iterator. For each result, print the
     * title, video ID, location, and thumbnail.
     *
     * @param iteratorVideoResults Iterator of Videos to print
     *
     * @param query Search query (String)
     */
    private static void prettyPrint(Iterator<Video> iteratorVideoResults) {

        System.out.println("\n=============================================================");

        if (!iteratorVideoResults.hasNext()) {
            System.out.println(" There aren't any results for your query.");
        }

        while (iteratorVideoResults.hasNext()) {

            Video singleVideo = iteratorVideoResults.next();
            if(( singleVideo.getContentDetails().getRegionRestriction() == null )
                && !singleVideo.getContentDetails().getLicensedContent() )
                continue;

            Thumbnail thumbnail = singleVideo.getSnippet().getThumbnails().getDefault();
            //GeoPoint location = singleVideo.getRecordingDetails().getLocation();

            System.out.println(" Video Id:  " + singleVideo.getId());
            System.out.println(" Title:     " + singleVideo.getSnippet().getTitle());
            //System.out.println(" Location: " + location.getLatitude() + ", " + location.getLongitude());
            System.out.println(" Thumbnail: " + thumbnail.getUrl());
            if( singleVideo.getContentDetails().getRegionRestriction() != null )
                System.out.println(" contentDetails.regionRestriction.allowed[]: " + singleVideo.getContentDetails().getRegionRestriction().getAllowed());
            if( singleVideo.getContentDetails().getRegionRestriction() != null )
                System.out.println(" contentDetails.regionRestriction.blocked[]: " + singleVideo.getContentDetails().getRegionRestriction().getBlocked());
            if( singleVideo.getContentDetails().getLicensedContent() != null )
                System.out.println(" contentDetails.licensedContent: " + singleVideo.getContentDetails().getLicensedContent());
            System.out.println("\n-------------------------------------------------------------\n");
        }
    }

    private void deleteVideos(Iterator<Video> iteratorVideoResults) throws IOException {
        while (iteratorVideoResults.hasNext()) {
            Video singleVideo = iteratorVideoResults.next();
            if(( singleVideo.getContentDetails().getRegionRestriction() != null )
                    && (singleVideo.getContentDetails().getRegionRestriction().getBlocked() != null) ) {

                YouTube.Videos.Delete videosDeleteRequest = youtube.videos().delete(singleVideo.getId());
                videosDeleteRequest.execute();

            }
        }
    }
}
