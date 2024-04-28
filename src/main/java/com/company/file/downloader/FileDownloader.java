package com.company.file.downloader;

import java.net.URI;

public interface FileDownloader {
  void download(URI uri, String outputPath) throws Exception;
}
