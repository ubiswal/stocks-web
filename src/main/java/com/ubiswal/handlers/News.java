package com.ubiswal.handlers;

import lombok.Getter;

public class News {
    @Getter
    private String title;

    @Getter
    private String urlToImage;

    @Getter
    private String url;

    public News(String title, String url, String urlToImage) {
        this.title = title;
        this.urlToImage = urlToImage;
        this.url = url;
    }
}
