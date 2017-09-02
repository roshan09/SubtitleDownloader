package com.opensubtitle;

/**
 * Created by roshan09 on 30/7/17.
 */
public class SubtitleDownloader {

	public void download(String movieName)
	{
		OpenSubtitle openSubtitle = new OpenSubtitle();
		openSubtitle.login();
		openSubtitle.searchSubtitle(movieName);
		openSubtitle.downloadSubtitle();
	}
	
    public static void main(String args[])
	{
        SubtitleDownloader downloader = new SubtitleDownloader();
		downloader.download("Interstellar");
    }
}
