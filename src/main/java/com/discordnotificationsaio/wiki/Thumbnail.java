
package com.discordnotificationsaio.wiki;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Thumbnail {

    @SerializedName("source")
    @Expose
    private String source;
    @SerializedName("width")
    @Expose
    private Long width;
    @SerializedName("height")
    @Expose
    private Long height;

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Long getWidth() {
        return width;
    }

    public void setWidth(Long width) {
        this.width = width;
    }

    public Long getHeight() {
        return height;
    }

    public void setHeight(Long height) {
        this.height = height;
    }

}
