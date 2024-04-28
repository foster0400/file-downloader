package com.company.file.downloader.impl;

import com.company.file.downloader.FileDownloader;
import com.company.file.util.FileDownloaderUtil;
import org.apache.commons.net.ftp.FTPClient;

import java.net.URI;

public class FtpFileDownloader implements FileDownloader {
  @Override
  public void download(URI uri, String outputPath) throws Exception {
    FTPClient ftpClient = new FTPClient();
    try {
      String host = uri.getHost();
      int port = uri.getPort() != -1 ? uri.getPort() : 21; // Default FTP port is 21
      String username = uri.getUserInfo() != null ? uri.getUserInfo().split(":")[0] : "anonymous"; // Default username is "anonymous"
      String password = uri.getUserInfo() != null ? uri.getUserInfo().split(":")[1] : "anonymous"; // Default password is "anonymous"
      String filePath = uri.getPath();

      ftpClient.connect(host, port);
      ftpClient.login(username, password);
      ftpClient.enterLocalPassiveMode();

      FileDownloaderUtil.saveFile(ftpClient.retrieveFileStream(filePath), outputPath);
    } finally {
      try {
        ftpClient.disconnect();
      } catch (Exception ignored) {
      }
    }
  }
}
