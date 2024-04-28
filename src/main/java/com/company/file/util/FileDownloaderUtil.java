package com.company.file.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileDownloaderUtil {
  /**
   * save file.
   * notes :
   * 1. when it fails to fully download the file, it will remove the file
   * @param inputStream input stream
   * @param outputPath where the file is going to be saved
   */
  public static void saveFile(InputStream inputStream, String outputPath) {
    ReadableByteChannel readableByteChannel = null;
    FileOutputStream fileOutputStream = null;
    FileChannel fileChannel = null;
    try {
      readableByteChannel = Channels.newChannel(inputStream);
      fileOutputStream = new FileOutputStream(outputPath);
      fileChannel = fileOutputStream.getChannel();
      fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
    } catch (Exception e) {
      if (fileOutputStream != null) {
        // close output stream resource and delete created file
        try {
          fileOutputStream.close();
        } catch (Exception ignored) {}

        try {
          Path path = Paths.get(outputPath);
          Files.deleteIfExists(path);
        } catch (Exception ignored) {}
      }
    } finally {
      //close all resources
      if (fileOutputStream != null) {
        try {
          fileOutputStream.close();
        } catch (Exception ignored) {
        }
      }

      try {
        if (fileChannel != null) {
          fileChannel.close();
        }
      } catch (IOException ignored) {
      }

      try {
        if (readableByteChannel != null) {
          readableByteChannel.close();
        }
      } catch (IOException ignored) {
      }
    }
  }
}
