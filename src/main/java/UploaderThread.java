/**
 * Created by Pub on 19.03.2018.
 */

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import java.io.*;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class UploaderThread extends Thread {

    private static final String APP_ID = "167654585"; // ID вашего приложения
    private static final String ACCESS_TOKEN = "a9e682e5ca64692077bc5eab815927b93b8c0e2e3abb95fb61571d2d21b69459e78f7dcd577cbb9df8f1b";
    private static final String API_VERSION = "5.57"; // Последняя на данный момент
    private static final String VIDEO_SAVE_URL = "https://api.vk.com/method/video.save";

    private static final Logger log = Logger.getLogger("UploaderThread");
    static boolean midnightRestart = true;
    private int nextAd = 1;
    private String adText, adVideo;

    public void run() {
        System.out.println("UploaderThread.run()");

        Statement stmt;
        boolean dailyLimitExceeded = false;

        try {
            ResultSet rs;
            do {
                stmt = Util.connection.createStatement();
                rs = stmt.executeQuery(
                        "SELECT `url`, `stream_url`, `title`, `descr`, `thumb`, `breadcrumbs`, `quality`, `date` FROM `tbl_video` WHERE `video_id` IS NULL;"
                );

                queryNextAd();

                while (rs.next() && !dailyLimitExceeded) {
                    VideoData video = createVideoData(rs);
                    getVideoStream(video);
                    Integer encoderExitCode = runReencode(video, adVideo);

                    if(encoderExitCode == 0) {
                        YoutubeManager manager = new YoutubeManager();
                        dailyLimitExceeded = uploadVideo(video, manager);

                        if(video.getVideoId() != null) {
                            if (midnightRestart && (video.getThumb() != null)) {
                                manager.UploadThumbnail(video);
                            }

                            insertVideoIntoPlaylist(video, manager);
                            updateDb(video);
                            queryNextAd();

                            if(midnightRestart) {
                                if( !vkVideoPasteShare(video) ) {
                                    log.info("Disabling VK posting and thumbnail uploading.");
                                    midnightRestart = false;
                                }
                            }
                        }
                    }
                }
            }
            while(rs.first() && !dailyLimitExceeded);

            rs.close();
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }

    private VideoData createVideoData(ResultSet rs) throws SQLException {
        VideoData video = new VideoData(rs.getString("title"), rs.getString("descr"), new ArrayList<>());
        video.setDate(rs.getDate("date"));
        video.setUrl(rs.getString("url"));
        video.setStreamUrl(rs.getString("stream_url"));
        video.setThumb(rs.getString("thumb"));
        video.setBreadcrumbs(rs.getString("breadcrumbs"));
        video.setQuality(rs.getInt("quality"));
        return video;
    }

    private void insertVideoIntoPlaylist(VideoData video, YoutubeManager manager) throws IOException {
        if (video.getBreadcrumbs() != null) {
            String playlistId = manager.isPlaylistExists(
                    YoutubeManager.CHANNEL_ID[YoutubeManager.currentChannel],
                    video.getBreadcrumbs());
            if (playlistId == null) {
                playlistId = manager.insertPlaylist(video.getBreadcrumbs(), "");
            }
            manager.insertPlaylistItem(playlistId, video.getVideoId(), video.getTitle());
        }
    }

    private static String[] getCommand(String adFile, int quality, String path) {
        final String VIDEO_FOLDER = "video";

        String resolution = "";
        switch(quality) {
            case 360:
                resolution = "640:360";
                break;
            case 540:
                resolution = "960:540";
                break;
        }

        String[] cmd = new String[] {
                "ffmpeg", "-y", "-i", String.format("%s/%s", VIDEO_FOLDER, adFile), "-i", Main.SOURCE_VIDEO_FILENAME, "-filter_complex",
                String.format("[0:v]scale=%s[v0]; [v0][0:a][1:v][1:a]concat=n=2:v=1:a=1[v][a]", resolution),
                "-map", "[v]", "-map", "[a]", Main.ENCODED_VIDEO_FILENAME
        };

        return cmd;
    }

    private Integer runReencode(VideoData video, String adVideo) {
        String path = Util.getJarPath();
        String[] cmd = getCommand(adVideo, video.getQuality(), path);
        log.info(String.join(" ", cmd));
        return Util.runSystemCommand(cmd, path);
    }

    private boolean uploadVideo(VideoData video, YoutubeManager manager) throws IOException {
        while(video.getVideoId() == null) {
            try {
                manager.UploadVideo(video, adText);
            } catch (GoogleJsonResponseException e) {
                if(e.getDetails().getErrors().get(0).getReason().equals("dailyLimitExceeded")) {
                    return true;
                }
            }

            if(video.getVideoId() == null) {
                YoutubeManager.currentChannel++;
                if(YoutubeManager.currentChannel >= YoutubeManager.CHANNEL_ID.length)
                    YoutubeManager.currentChannel = 0;
                log.info(":::::Switching account on: " + YoutubeManager.CHANNEL_ID[YoutubeManager.currentChannel]);
                manager.auth(YoutubeManager.CHANNEL_ID[YoutubeManager.currentChannel]);
            }
        }

        return false;
    }

    private void updateDb(VideoData video) {
        String stat = null;
        try {
            PreparedStatement pstmt = Util.connection.prepareStatement("UPDATE `tbl_video` SET `video_id` = ?, `account` = ? WHERE `url` = ?");
            pstmt.setString(1, video.getVideoId());
            pstmt.setString(2, video.getAccount());
            pstmt.setString(3, video.getUrl());
            stat = pstmt.toString();
            /*int updatedRows = */pstmt.executeUpdate();
            //System.out.printf("Rows updated: %d\r\n", updatedRows);
        } catch (SQLException e) {
            log.info(stat);
            e.printStackTrace();
        }
    }

    private void getVideoStream(VideoData video) throws IOException {
        log.info("Downloading stream: " + video.getTitle());

        if(video.getThumb() != null) {
            byte[] responseBytes = Jsoup.connect(video.getThumb())
                    .userAgent(Main.USER_AGENT)
                    .maxBodySize(0)
                    .ignoreContentType(true)
                    .timeout(60000)
                    .execute()
                    .bodyAsBytes();
            FileOutputStream out = new FileOutputStream(new java.io.File(Main.THUMB_FILENAME));
            out.write(responseBytes);  // resultImageResponse.body() is where the image's contents are.
            out.close();
        }

        boolean videoDownloaded;
        do {
            RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(30 * 1000).build();
            HttpClient httpClient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();

            //HttpClient httpClient = HttpClients.createDefault();
            HttpGet request = new HttpGet(video.getStreamUrl());

            HttpResponse response = httpClient.execute(request);
            HttpEntity entity = response.getEntity();

            videoDownloaded = readStreamAndHandleErrors(entity);
        } while (!videoDownloaded);
    }

    private boolean readStreamAndHandleErrors(HttpEntity entity) throws IOException {
        boolean videoDownloaded = false;

        BufferedInputStream bis = new BufferedInputStream(entity.getContent());
        BufferedOutputStream bos = new BufferedOutputStream(
                new FileOutputStream(new File(Main.SOURCE_VIDEO_FILENAME)));

        int inByte;
        try {
            while ((inByte = bis.read()) != -1) bos.write(inByte);
            videoDownloaded = true;
        } catch (SocketException e) {
            log.info("java.net.SocketException: Connection reset  @stream read, repeating..");
            //System.out.println("java.net.SocketException: Connection reset  @stream read, repeating..");
        }
        catch (SocketTimeoutException e) {
            log.info("java.net.SocketTimeoutException: Read timed out, repeating..");
            //System.out.println("java.net.SocketTimeoutException: Read timed out, repeating..");
        }
        catch (ConnectionClosedException e) {
            log.info("ConnectionClosedException: " + e.getMessage());
            //System.out.println("ConnectionClosedException: " + e.getMessage());
        }

        bis.close();
        bos.close();

        return videoDownloaded;
    }

    private boolean vkVideoPasteShare(VideoData video) {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            String videoURL = "https://youtu.be/" + video.getVideoId();

            HttpPost httppost = new HttpPost(VIDEO_SAVE_URL);
            httppost.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

            List<BasicNameValuePair> params = new ArrayList<>(2);
            params.add(new BasicNameValuePair("group_id", APP_ID));
            params.add(new BasicNameValuePair("link", videoURL));
            //params.add(new BasicNameValuePair("name", video.getTitle()));
            params.add(new BasicNameValuePair("description",
                    String.format("%s\r\n\r\n%s %s\r\n\r\n%s", adText, video.getTitle(), videoURL, video.getDescr())));
            params.add(new BasicNameValuePair("wallpost", "1"));
            params.add(new BasicNameValuePair("access_token", ACCESS_TOKEN));
            params.add(new BasicNameValuePair("v", API_VERSION));
            httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

            HttpResponse response = httpClient.execute(httppost);

            String json = EntityUtils.toString(response.getEntity(), "UTF-8");
            log.info(json);
            JSONObject obj = new JSONObject(json);

            if(!obj.has("error")) {
                String upload_url = obj.getJSONObject("response").getString("upload_url");
                HttpGet httpget = new HttpGet(upload_url);
                response = httpClient.execute(httpget);
                json = EntityUtils.toString(response.getEntity(), "UTF-8");
                log.info(json);
            }
            else {
                // "error_code":214,"error_msg":"Access to adding post denied: you can only add 50 posts a day"
                if(obj.getJSONObject("error").getInt("error_code") == 214) {
                    return false;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }

    private void queryNextAd() {
        try {
            ResultSet rs;
            boolean doContinue = false;
            do{
                PreparedStatement stmt = Util.connection.prepareStatement(
                        "SELECT `text`, `video` FROM `ads` WHERE `id` = ?;");
                stmt.setInt(1, nextAd++);

                rs = stmt.executeQuery();

                if(!rs.next()) {
                    nextAd = 1;
                    doContinue = true;
                    rs.close();
                }
            } while(doContinue);

            adText = rs.getString("text");
            adVideo = rs.getString("video");
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
