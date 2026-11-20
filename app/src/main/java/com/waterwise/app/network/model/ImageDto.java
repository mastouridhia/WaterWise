package com.waterwise.app.network.model;

import com.google.gson.annotations.SerializedName;

/** "default_image" object. The urls are signed and expire after 24 h. */
public class ImageDto {

    public String thumbnail;

    @SerializedName("small_url")
    public String smallUrl;

    @SerializedName("regular_url")
    public String regularUrl;

    /** Best url for a list thumbnail, or null for the "upgrade plan" image. */
    public String usableSmallUrl() {
        String url = smallUrl != null ? smallUrl : thumbnail;
        return isPlaceholder(url) ? null : url;
    }

    /** Best url for a full photo, or null for the "upgrade plan" image. */
    public String usableRegularUrl() {
        String url = regularUrl != null ? regularUrl : smallUrl;
        return isPlaceholder(url) ? null : url;
    }

    private static boolean isPlaceholder(String url) {
        return url == null || url.contains("upgrade_access");
    }
}
