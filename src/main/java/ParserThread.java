import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.*;
import java.text.BreakIterator;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Created by Pub on 19.03.2018.
 */
public class ParserThread implements Runnable {
    //private static final String URL_FORMAT = "https://player.vgtrk.com/iframe/datavideo/id/%d/sid/vh";
    private static final String URL_FORMAT = "https://player.vgtrk.com/iframe/datavideo/id/%d/sid/vesti";
    private static final String TITLE_APPEND_TEXT = " - Россия Сегодня";
    int VIDEO_ID_SEQ_START = 1815721;
    private static final int READ_FAILURES_LIMIT = 10;

    private static final ArrayList<String> skip_titles = new ArrayList<>();
    private static final Logger log = Logger.getLogger("ParserThread");
    private static UploaderThread thread;
    private Connection connection;

    ParserThread() {
        String url = "jdbc:mysql://localhost:3306/russia24-tv?useUnicode=true&characterEncoding=utf-8";
        String username = "root";
        String password = "";

        //log.setLevel(Level.ALL);
        log.info("Connecting database...");

        try {
            connection = DriverManager.getConnection(url, username, password);
            log.info("Database connected!");
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot connect the database!", e);
        }
    }

    void disconnect() {
        Util.disconnect(connection);
        connection = null;
    }

    public void run() {
        if((LocalDateTime.now().getHour() == 0) && (LocalDateTime.now().getMinute() <= 4) && (!UploaderThread.doAddThumb)) {
            log.info("Re-enabling VK posting and thumbnail uploading.");
            UploaderThread.doAddThumb = true;
        }

        try {
            //System.out.println("ParserThread.run()");
            Scanner in = null;
            in = new Scanner( new FileReader(Util.getJarPath() + "/copyrighted.txt") );
            while( in.hasNextLine() ) skip_titles.add( in.nextLine() );
            in.close();

            Statement stmt = null;

            int db_url_id = VIDEO_ID_SEQ_START;
            try {
                stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(
                        "SELECT SUBSTRING_INDEX(SUBSTRING(`url` FROM 46),'/',1) AS `url_id` FROM `tbl_video`"
                        + " ORDER BY CAST(SUBSTRING_INDEX(SUBSTRING(`url` FROM 46),'/',1) AS UNSIGNED) DESC LIMIT 0,1;");
                if (rs.next()) {
                    int url_id = Integer.parseInt(rs.getString("url_id"));
                    if(url_id > db_url_id)
                        db_url_id = url_id;
                }
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            //System.out.printf("DB Last url id: %d\r\n", db_url_id);

            int failures = READ_FAILURES_LIMIT;
            boolean startUpload = false;

            while(failures > 0) {
                VideoData video = readVideoData(++db_url_id);
                if(video != null) {
                    if(video.getStreamUrl() != null) startUpload = true;
                    failures = READ_FAILURES_LIMIT;
                }
                else {
                    failures--;
                };
            }

            if ( ((thread == null) || !thread.isAlive()) && startUpload ) {
                recheckMissedIds();
                log.info("Starting new Uploader Thread.");
                thread = new UploaderThread();
                thread.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void recheckMissedIds() throws IOException, InterruptedException {
        Statement stmt = null;

        try {
            stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT SUBSTRING_INDEX(SUBSTRING(`url` FROM 46),'/',1) AS `url_id` " +
                            "FROM `tbl_video` " +
                            "WHERE STRCMP(`url`, \"" + String.format(URL_FORMAT, VIDEO_ID_SEQ_START) + "\") = 1 " +
                            "ORDER BY CAST(SUBSTRING_INDEX(SUBSTRING(`url` FROM 46),'/',1) AS UNSIGNED) DESC " +
                            "LIMIT 100;");

            ArrayList<Integer> ids = new ArrayList<>();

            while (rs.next()) {
                ids.add( Integer.parseInt(rs.getString("url_id")) );
                //log.info(rs.getString("url_id"));
            }
            rs.close();

            int foundCount = 0,
                    noSource = 0,
                    missedCount = ids.get(0) - ids.get(ids.size()-1)+1 - ids.size();

            if(missedCount > 0) {
                for(int i = 0; i < ids.size() -1; i++) {
                    for(int id = ids.get(i+1) +1; id < ids.get(i); id++) {
                        //log.info("Missed " + id);
                        VideoData video = readVideoData(id);
                        if(video != null)
                            if(video.getStreamUrl() != null)
                                foundCount++;
                            else
                                noSource++;
                    }
                }

                log.info(String.format("Missed IDs count: %d, found %d new videos, %d no source.", missedCount, foundCount, noSource));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private VideoData readVideoData(int id) throws IOException, InterruptedException {
        String urlStr = String.format(URL_FORMAT, id);
        //log.info(String.format("Read video %s", urlStr));

        VideoData video = getVideo(urlStr);

        if (video == null)
            return null;

        for (String skipline : skip_titles) {
            Pattern pattern = Pattern.compile(skipline);
            if (pattern.matcher(video.getTitle()).find()) {
                video.setVideoId("skip-title");
                log.info("----- Skipping title...");
                break;
            }
        }

        if(video.getVideoId() == null) {
            if(isSourceExists(video)) {
                video.setVideoId("dup_source");
                log.info("----- Duplicate source...");
            }
        }

        // not saving videos wo source - they will be rechecked
        if((video.getStreamUrl() != null) || (video.getVideoId() != null) ) {
            String stat = null;
            try {
                PreparedStatement stmt = connection.prepareStatement(
                        "INSERT INTO `tbl_video` (`url`, `stream_url`, `title`, `descr`, `tags`, `thumb`, `breadcrumbs`, `quality`, `video_id`, `date`) VALUES (?,?,?,?,?,?,?,?,?,?)");

                StringBuilder tags = new StringBuilder();
                String delim = "";
                for (String i : video.getTags()) {
                    tags.append(delim).append(i);
                    delim = ",";
                }

                stmt.setString(1, video.getUrl());
                stmt.setString(2, video.getStreamUrl());
                stmt.setString(3, video.getTitle());
                stmt.setString(4, video.getDescr());
                stmt.setString(5, tags.toString());
                stmt.setString(6, video.getThumb());
                stmt.setString(7, video.getBreadcrumbs());
                stmt.setInt(8, video.getQuality());
                stmt.setString(9, video.getVideoId());
                stmt.setTimestamp(10, new java.sql.Timestamp(video.getDate().getTime()));

                stat = stmt.toString();
                stmt.execute();
                //log.info(stat);
            } catch (SQLException e) {
                log.warning(stat);
                e.printStackTrace();
            }
        }
        else
            log.info("----- No source...");

        return video;
    }

    private VideoData getVideo(String url) {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().setUserAgent(Main.USER_AGENT).build()) {
            HttpGet request = new HttpGet(url);
            request.addHeader("content-type", "application/json");
            HttpResponse result = httpClient.execute(request);
            String json = EntityUtils.toString(result.getEntity(), "UTF-8");

            //log.info(json);
            JSONObject obj = new JSONObject(json);

            if(obj.getInt("status") == 404) {
                //System.out.println(404);
                return null;
            }

            return createVideoData(url, obj);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private VideoData createVideoData(String url, JSONObject obj) {
        JSONObject medialist = obj.getJSONObject("data").getJSONObject("playlist").getJSONArray("medialist").getJSONObject(0);

        String title = truncateText(medialist.getString("title"), 100 - TITLE_APPEND_TEXT.length()) + TITLE_APPEND_TEXT;
        String descr = medialist.getString("anons").replace("<br>", "");

        VideoData video = new VideoData(title, descr, null);
        log.info(video.getTitle());
        log.info(video.getDescr());
        video.setUrl(url);
        log.info(video.getUrl());

        Object picture = medialist.get("picture");
        if(picture instanceof String) {
            video.setThumb((String)picture);
        }
        log.info(video.getThumb());

        if(medialist.has("sources") && medialist.getJSONObject("sources").has("http")) {
            JSONObject sources = medialist.getJSONObject("sources").getJSONObject("http");
            String sourceUrl = "";
            int bestQuality = 0;
            Iterator<String> keys = sources.keys();
            while( keys.hasNext() ){
                String key = keys.next();
                int quality = Integer.parseInt(key);
                if(quality > bestQuality) {
                    sourceUrl = sources.getString(key);
                    bestQuality = quality;
                }
            }

            video.setStreamUrl(sourceUrl);
            video.setQuality(bestQuality);
            log.info(video.getStreamUrl());
        }
        else {
            log.info("NO SOURCE.");
        }

        return video;
    }

    /**
     * Truncate text to the nearest word, up to a maximum length specified.
     *
     * @param text
     * @param maxLength
     * @return
     */
    private String truncateText(String text, int maxLength) {
        if (text != null && text.length() > maxLength) {
            BreakIterator bi = BreakIterator.getWordInstance();
            bi.setText(text);

            if (bi.isBoundary(maxLength - 1)) {
                return text.substring(0, maxLength - 2);
            } else {
                int preceding = bi.preceding(maxLength - 1);
                return text.substring(0, preceding - 1);
            }
        } else {
            return text;
        }
    }

    private boolean isSourceExists(VideoData video) {
        boolean result = false;

        if(video.getStreamUrl() != null)
            try {
                PreparedStatement stmt = connection.prepareStatement(
                        "SELECT `stream_url` FROM `tbl_video` WHERE `stream_url` = ?;");
                stmt.setString(1, video.getStreamUrl());

                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    result = true;
                }
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }

        return result;
    }

    public static String html2text(String html) {
        return Jsoup.parse(html).text();
    }
}
