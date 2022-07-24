
package com.discordnotifcationsaio.wiki;


import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Page {

    @SerializedName("pageid")
    @Expose
    private Long pageid;
    @SerializedName("ns")
    @Expose
    private Long ns;
    @SerializedName("title")
    @Expose
    private String title;
    @SerializedName("thumbnail")
    @Expose
    private Thumbnail thumbnail;
    @SerializedName("pageimage")
    @Expose
    private String pageimage;

    public Long getPageid() {
        return pageid;
    }

    public void setPageid(Long pageid) {
        this.pageid = pageid;
    }

    public Long getNs() {
        return ns;
    }

    public void setNs(Long ns) {
        this.ns = ns;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Thumbnail getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(Thumbnail thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getPageimage() {
        return pageimage;
    }

    public void setPageimage(String pageimage) {
        this.pageimage = pageimage;
    }

}
