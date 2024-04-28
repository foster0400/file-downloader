package com.company.file.downloader.impl;

import com.company.file.downloader.FileDownloader;
import com.company.file.util.FileDownloaderUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

@Slf4j
public class HttpFileDownloader implements FileDownloader {
  @Override
  public void download(URI uri, String outputPath) throws Exception {
    HttpURLConnection conn = null;
    try {
      URL url = uri.toURL();
      conn = (HttpURLConnection) url.openConnection();
      conn.setConnectTimeout(5000); // Set connection timeout to 5 seconds
      conn.setReadTimeout(10000); // Set read timeout to 10 seconds
      FileDownloaderUtil.saveFile(conn.getInputStream(), outputPath);
    } finally {
      if (conn != null) {
        conn.disconnect();
      }
    }
  }
}
