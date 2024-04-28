package com.company.file.downloader.impl;

import com.company.file.downloader.FileDownloader;
import com.company.file.util.FileDownloaderUtil;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.net.URI;

public class SftpFileDownloader implements FileDownloader {
  @Override
  public void download(URI uri, String outputPath) throws Exception {
    Session session = null;
    ChannelSftp channelSftp = null;
    try {
      String host = uri.getHost();
      int port = uri.getPort() != -1 ? uri.getPort() : 21; // Default FTP port is 21
      String username = uri.getUserInfo() != null ? uri.getUserInfo().split(":")[0] : "anonymous"; // Default username is "anonymous"
      String password = uri.getUserInfo() != null ? uri.getUserInfo().split(":")[1] : "anonymous"; // Default password is "anonymous"
      String filePath = uri.getPath();

      JSch jsch = new JSch();
      session = jsch.getSession(username, host, port);

      session.setConfig("StrictHostKeyChecking", "no");
      if (password != null) {
        session.setPassword(password);
      }
      session.connect();

      channelSftp = (ChannelSftp) session.openChannel("sftp");
      channelSftp.connect();

      FileDownloaderUtil.saveFile(channelSftp.get(filePath), outputPath);
    } finally {
      if (session != null) {
        session.disconnect();
      }

      if (channelSftp != null) {
        try {
          channelSftp.disconnect();
        } catch (Exception ignored) {
        }
      }
    }
  }
}
