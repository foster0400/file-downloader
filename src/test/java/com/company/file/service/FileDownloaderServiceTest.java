package com.company.file.service;


import com.company.file.constant.SupportedProtocolConstant;
import com.company.file.downloader.FileDownloader;
import com.company.file.downloader.impl.FtpFileDownloader;
import com.company.file.downloader.impl.HttpFileDownloader;
import com.company.file.downloader.impl.SftpFileDownloader;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.net.URI;
import java.time.Clock;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;

public class FileDownloaderServiceTest {

  private static final String downloadLocation = "downloads";
  private static final Set<String> invalidUriSet = Set.of(
      "htt://speedtest.tele2.net/1MB.zip",
      "https://programming/ 24hrs. txt?param=1???",
      "ftp.gnu.org/README",
      "sft://demo:@test.rebex.net/pub/example/Example.csv",
      "sftp://demo:test.rebex.net/pu{}b/example/sample.pdf/invalid"
  );

  private static final Set<String> validUriSet = Set.of(
      "https://github.com/foster0400/rekognition-for-attendance-demo/blob/master/doc/attendance.png",
      "http://github.com/foster0400/rekognition-for-attendance-demo/blob/master/doc/attendance.xml",
      "ftp://ftp.gnu.org/README",
      "sftp://test.rebex.net:22/pub/example/readme.txt"
  );

  private static final Set<String> noFileFoundUriSet = Set.of(
      "https://github.com/foster0400/rekognition-for-attendance-demo/blob/master/doc/RANDOM.png"
  );

  private static final Set<String> sameResourceUriSet = Set.of(
      "sftp://demo:test123@test.rebex.net:22/pub/example/readme.txt" //same with "sftp://test.rebex.net:22/pub/example/readme.txt"
  );

  private ScheduledExecutorService scheduledExecutorService;
  private FileDownloaderService fileDownloaderService;

  @BeforeAll
  static void beforeAll() {
    File directory = new File(downloadLocation);

    if (directory.exists()) {
      deleteDirectory(directory);
    }
  }

  @BeforeEach
  void setUp() {
    FileDownloader httpFileDownloader = new HttpFileDownloader();
    Map<String, FileDownloader> fileDownloaderProtocolRegistry = Map.of(
        SupportedProtocolConstant.HTTPS_PROTOCOL, httpFileDownloader,
        SupportedProtocolConstant.HTTP_PROTOCOL, httpFileDownloader,
        SupportedProtocolConstant.FTP_PROTOCOL, new FtpFileDownloader(),
        SupportedProtocolConstant.SFTP_PROTOCOL, new SftpFileDownloader()
    );

    scheduledExecutorService = Executors.newScheduledThreadPool(3);
    fileDownloaderService = Mockito.spy(new FileDownloaderService(
        Clock.systemUTC(),
        fileDownloaderProtocolRegistry,
        scheduledExecutorService
    ));
  }

  private static void deleteDirectory(File directory) {
    if (!directory.isDirectory()) {
      return;
    }
    File[] files = directory.listFiles();

    if (files != null) {
      for (File file : files) {
        if (file.isDirectory()) {
          deleteDirectory(file);
        } else {
          file.delete();
        }
      }
    }
    directory.delete();
  }

  @AfterEach
  void tearDown() {
    File directory = new File(downloadLocation);

    if (directory.exists()) {
      deleteDirectory(directory);
    }
  }

  @AfterAll
  static void afterAll() {
  }

  @Test
  void testDownloadBulk_AllUriInvalid() {
    fileDownloaderService.downloadBulk(invalidUriSet, 1, downloadLocation);

    File directory = new File(downloadLocation);

    assertFalse(directory.exists());
  }

  @Test
  void testDownloadBulk_PartialUriInvalid() {
    Set<String> uriSet = new HashSet<>();
    uriSet.addAll(validUriSet);
    uriSet.addAll(invalidUriSet);
    fileDownloaderService.downloadBulk(invalidUriSet, 1, downloadLocation);

    File directory = new File(downloadLocation);

    assertFalse(directory.exists());
  }

  @Test
  void testDownloadBulk_AllSuccess() throws Exception {
    fileDownloaderService.downloadBulk(validUriSet, 1, downloadLocation);
    scheduledExecutorService.awaitTermination(10, TimeUnit.SECONDS);

    File directory = new File(downloadLocation);

    assertTrue(directory.exists());
    File[] files = directory.listFiles();
    assertNotNull(files);
    assertEquals(validUriSet.size(), files.length);
  }

  @Test
  void testDownloadBulk_AllSuccess_HaveSameResources() throws Exception {
    Set<String> uriSet = new HashSet<>();
    uriSet.addAll(validUriSet);
    uriSet.addAll(sameResourceUriSet);
    fileDownloaderService.downloadBulk(uriSet, 1, downloadLocation);
    scheduledExecutorService.awaitTermination(10, TimeUnit.SECONDS);

    File directory = new File(downloadLocation);

    assertTrue(directory.exists());
    File[] files = directory.listFiles();
    assertNotNull(files);
    assertEquals(validUriSet.size(), files.length); //size is equal to validUriSet even though there is same resource uri set has been added
  }

  @Test
  void testDownloadBulk_HaveFailedDownload() throws Exception {
    int numberOfRetry = 2;
    Set<String> uriSet = new HashSet<>();
    uriSet.addAll(validUriSet);
    uriSet.addAll(noFileFoundUriSet);
    fileDownloaderService.downloadBulk(uriSet, numberOfRetry, downloadLocation);
    scheduledExecutorService.awaitTermination(20, TimeUnit.SECONDS);

    File directory = new File(downloadLocation);

    assertTrue(directory.exists());
    File[] files = directory.listFiles();
    assertNotNull(files);
    assertEquals(validUriSet.size(), files.length);
    Mockito.verify(fileDownloaderService, times(validUriSet.size() + (noFileFoundUriSet.size() * numberOfRetry)))
        .downloadWithRetry(any(), any(), anyInt(), anyInt(), any());
  }

  @Test
  void testDownloadWithRetry_CheckRetryMechanismWorks() throws Exception {
    int numberOfRetry = 2;
    String uriString = "https://example/example.txt";
    URI uri = new URI(uriString);
    fileDownloaderService.downloadWithRetry(uri, downloadLocation + uri.getPath(), numberOfRetry, 1, "id");

    Mockito.verify(fileDownloaderService, times(2)).downloadWithRetry(any(), any(), anyInt(), anyInt(), any());
  }

  @Test
  void testGenerateOutputPath_SameResource_SameOutputPath() throws Exception {
    URI uri1 = new URI("sftp://test.rebex.net:22/pub/example/readme.txt");
    URI uri2 = new URI("sftp://demo:test123@test.rebex.net:22/pub/example/readme.txt");
    assertEquals(
        fileDownloaderService.generateOutputPath(downloadLocation, uri1),
        fileDownloaderService.generateOutputPath(downloadLocation, uri2)
    );
  }

  @Test
  void testGenerateOutputPath_DifferentResource_DifferentOutputPath() throws Exception {
    URI uri1 = new URI("https://github.com/foster0400/rekognition-for-attendance-demo/blob/master/doc/attendance.png");
    URI uri2 = new URI("http://github.com/foster0400/rekognition-for-attendance-demo/blob/master/doc/attendance.xml");
    assertNotEquals(
        fileDownloaderService.generateOutputPath(downloadLocation, uri1),
        fileDownloaderService.generateOutputPath(downloadLocation, uri2)
    );
  }
}
