import java.util.Date;
import java.util.List;

/**
 * Created by Pub on 17.03.2018.
 */
public class VideoData {
    private String url;
    private Integer sourceUrlId;
    private String streamUrl;
    private String title;
    private String descr;
    private List<String> tags;
    private String thumb;
    private String breadcrumbs;
    private Integer quality;
    private String account;
    private String videoId;
    private Date date;

    VideoData(String title, String descr, List<String> tags) {
        this.title = title;
        this.descr = descr;
        this.tags = tags;
        this.date = new Date();
    }

    @Override
    public String toString() {
        return String.format("%s, %s, %s", this.getTitle(), this.getUrl(), this.getStreamUrl());
    }

    String getUrl() {
        return url;
    }

    void setUrl(String url) {
        this.url = url;
    }

    public Integer getSourceUrlId() {
        return sourceUrlId;
    }

    public void setSourceUrlId(Integer sourceUrlId) {
        this.sourceUrlId = sourceUrlId;
    }

    String getStreamUrl() {
        return streamUrl;
    }

    void setStreamUrl(String streamUrl) {
        this.streamUrl = streamUrl;
    }

    String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    String getDescr() {
        return descr;
    }

    public void setDescr(String descr) {
        this.descr = descr;
    }

    List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    String getThumb() {
        return thumb;
    }

    void setThumb(String thumb) {
        this.thumb = thumb;
    }

    String getBreadcrumbs() {
        return breadcrumbs;
    }

    void setBreadcrumbs(String breadcrumbs) {
        this.breadcrumbs = breadcrumbs;
    }

    Integer getQuality() {
        return quality;
    }

    public void setQuality(Integer quality) {
        this.quality = quality;
    }

    String getAccount() {
        return account;
    }

    void setAccount(String account) {
        this.account = account;
    }

    String getVideoId() {
        return videoId;
    }

    void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    Date getDate() {
        return date;
    }

    void setDate(Date date) {
        this.date = date;
    }
}
