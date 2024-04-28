package com.company.file.service;

import com.company.file.constant.SupportedProtocolConstant;
import com.company.file.downloader.FileDownloader;
import com.company.file.downloader.impl.FtpFileDownloader;
import com.company.file.downloader.impl.HttpFileDownloader;
import com.company.file.downloader.impl.SftpFileDownloader;
import com.company.file.model.DownloadBulkResult;
import com.company.file.model.config.DownloadConfiguration;
import com.company.file.model.URIPreValidationResult;
import com.company.file.validator.URIPreValidator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Slf4j
public class FileDownloaderService {
  private static final int INITIAL_ATTEMPT = 1;

  private final Clock clock;
  private final Map<String, FileDownloader> fileDownloaderProtocolRegistry;
  private final ScheduledExecutorService scheduledExecutorService;

  public FileDownloaderService(Clock clock,
                               Map<String, FileDownloader> fileDownloaderProtocolRegistry,
                               ScheduledExecutorService scheduledExecutorService) {
    this.clock = clock;
    this.fileDownloaderProtocolRegistry = fileDownloaderProtocolRegistry;
    this.scheduledExecutorService = scheduledExecutorService;
  }

  /**
   * download file in bulk.
   * the flow will be :
   * 1. do pre-validation first for all the uri given.
   * 2. if there is one or more fail to pass pre-validation, then it won't continue to download process
   * 3. if all given uri are valid, then it will continue to
   *    - create the directory if not exists yet
   *    - download all given uri parallel (based on given corePoolSize) and will be run in the background
   * 4. as mentioned in #3 download process will be run in the background, so user can check the pre-validation result first.
   * 5. to check whether the download process is success or not, need to check further to log
   *
   * @param uriStringSet     set of uri given by user
   * @param numberOfRetry    how many attempt to download
   * @param downloadLocation where download location will be
   * @return DownloadBulkResult, current use to let user know whether the pre-validation is success or not
   */
  public DownloadBulkResult downloadBulk(Set<String> uriStringSet, int numberOfRetry, String downloadLocation) {
    final String identifier = clock.millis() + "downloadLocation";
    log.info("method downloadList start with identifier : {}, total uri : {}, uriStringSet : {}, numberOfRetry : {}, downloadPath : {}",
        identifier, uriStringSet.size(), uriStringSet, numberOfRetry, downloadLocation);

    URIPreValidationResult uriPreValidationResult = URIPreValidator.initialise(uriStringSet)
        .validateAll()
        .getUriPreValidationResult();
    Set<URI> uriValidSet = uriPreValidationResult.getUriValidSet();
    log.info("method downloadList check validation result with identifier : {}, total validUri : {}, uriPreValidationResult : {}",
        identifier, uriValidSet.size(), uriPreValidationResult);

    if (!uriPreValidationResult.isAllUriValid()) {
      return DownloadBulkResult.preValidationFailed(uriPreValidationResult);
    }

    makeDirectoryIfNotExist(downloadLocation);

    uriValidSet.forEach(uri -> scheduledExecutorService.execute(() -> {
      String outputPath = generateOutputPath(downloadLocation, uri);
      downloadWithRetry(uri, outputPath, numberOfRetry, INITIAL_ATTEMPT, identifier);
    }));

    return DownloadBulkResult.preValidationSuccess();
  }

  private void makeDirectoryIfNotExist(String downloadLocation) {
    File directory = new File(downloadLocation);

    // Check if the directory doesn't exist
    if (!directory.exists()) {
      // Attempt to create the directory
      directory.mkdirs();
    }
  }

  /**
   * download the file with retry.
   * the flow will be :
   * 1. get suitable service (file downloader protocol)
   * 2. make sure the service is not null -> means the protocol is already registered
   * 3. call download to suitable service
   * 5. if there is error when call download
   * it will trigger retry mechanism until the download process is success or number of attempt has reached max
   *
   * @param uri           uri object
   * @param outputPath    where the downloaded file will be put
   * @param numberOfRetry how many attempt to download
   * @param attempt       current attempt
   * @param identifier    this will be used to indicate the download is triggered by which process.
   */
  void downloadWithRetry(URI uri, String outputPath, int numberOfRetry, int attempt, String identifier) {
    log.info("method downloadWithRetry start with identifier : {}, outputPath : {}, uri : {}, attempt : {}/{}", identifier, outputPath, uri, attempt,
        numberOfRetry);
    FileDownloader fileDownloaderProtocol = fileDownloaderProtocolRegistry.get(uri.getScheme().toLowerCase());
    if (fileDownloaderProtocol == null) {
      log.warn("method downloadWithRetry fileDownloaderProtocol is null, indicate protocol not supported yet " +
          "for identifier : {}, outputPath : {}, uri : {}, attempt : {}/{}", identifier, outputPath, uri, attempt, numberOfRetry);
      return;
    }

    try {
      fileDownloaderProtocol.download(uri, outputPath);
      log.info("method downloadWithRetry download success for identifier : {}, outputhPath : {}, uri : {}, attempt : {}/{}", identifier, outputPath,
          uri, attempt, numberOfRetry);
    } catch (Exception e) {
      log.error("method downloadWithRetry download error for identifier : {}, outputPath : {}, uri : {}, attempt : {}/{}", identifier, outputPath,
          uri, attempt, numberOfRetry, e);
      if (attempt < numberOfRetry) {
        downloadWithRetry(uri, outputPath, numberOfRetry, attempt + 1, identifier);
      }
    }
  }

  /**
   * generate output path for the file.
   * the output will be : {downloadLocation}/{uniquePrefix}-{filename}
   *
   * @param downloadLocation given download location
   * @param uri              uri object
   * @return String of expected output path
   */
  String generateOutputPath(String downloadLocation, URI uri) {
    final String path = uri.getPath();
    String filename = path.substring(path.lastIndexOf('/') + 1);
    return downloadLocation + "/" + generateFilenameUniquePrefix(uri) + "-" + filename;
  }

  /**
   * will be generated from protocol used, uri hostname, and path without its filename.
   *
   * @param uri uri
   * @return String the unique prefix
   */
  private static String generateFilenameUniquePrefix(URI uri) {
    String path = uri.getPath();
    String pathWithoutFilename = path.substring(0, path.lastIndexOf('/'));
    String input = uri.getScheme() + uri.getHost() + pathWithoutFilename;
    // Create MessageDigest instance for SHA-256
    MessageDigest md;
    try {
      md = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      //should not happen
      return null;
    }
    // Add input data bytes to digest
    md.update(input.getBytes());
    // Get the hash's bytes
    byte[] bytes = md.digest();
    // Convert byte array to a string representation
    StringBuilder sb = new StringBuilder();
    for (byte b : bytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }

  public static void main(String[] args) {
    ObjectMapper objectMapper = new ObjectMapper();
    if (args.length == 0) {
      log.info("need to input 2 file path as arguments : uri-list.json and configuration.json\n" +
          "example : /Users/chandra/uri-list.json /Users/chandra/configuration.json");
      return;
    }
    if (args.length == 1) {
      log.info("need to input 2 file path as arguments : uri-list.json and configuration.json\n" +
          "example : /Users/chandra/uri-list.json /Users/chandra/configuration.json");
      return;
    }

    Set<String> uriSet;
    DownloadConfiguration downloadConfiguration;
    try {
      uriSet = new HashSet<>(objectMapper.readValue(new File(args[0]), new TypeReference<List<String>>() {
      }));
      downloadConfiguration = objectMapper.readValue(new File(args[1]), new TypeReference<>() {
      });
    } catch (Exception e) {
      log.error("error when trying to get uri list and configuration", e);
      return;
    }

    FileDownloader httpFileDownloader = new HttpFileDownloader();

    Map<String, FileDownloader> fileDownloaderProtocolRegistry = Map.of(
        SupportedProtocolConstant.HTTPS_PROTOCOL, httpFileDownloader,
        SupportedProtocolConstant.HTTP_PROTOCOL, httpFileDownloader,
        SupportedProtocolConstant.FTP_PROTOCOL, new FtpFileDownloader(),
        SupportedProtocolConstant.SFTP_PROTOCOL, new SftpFileDownloader()
    );

    ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(downloadConfiguration.getCorePoolSize());
    FileDownloaderService fileDownloaderService = new FileDownloaderService(
        Clock.systemUTC(),
        fileDownloaderProtocolRegistry,
        scheduledExecutorService);

    fileDownloaderService.downloadBulk(uriSet, downloadConfiguration.getNumberOfRetry(), downloadConfiguration.getDownloadLocation());
    scheduledExecutorService.shutdown();
  }

}
