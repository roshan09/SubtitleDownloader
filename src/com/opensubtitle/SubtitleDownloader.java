package com.opensubtitle;

/**
 * Created by roshan09 on 30/7/17.
 */
public class SubtitleDownloader {

    public static void main(String args[]) {
        OpenSubtitle openSubtitle = new OpenSubtitle();
        openSubtitle.login();
        openSubtitle.searchSubtitle("Interstellar");
        openSubtitle.downloadSubtitle();
    }
}
