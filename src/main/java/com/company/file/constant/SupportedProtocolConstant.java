package com.company.file.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Set;

@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
public class SupportedProtocolConstant {
  public static final String HTTP_PROTOCOL = "http";
  public static final String HTTPS_PROTOCOL = "https";
  public static final String FTP_PROTOCOL = "ftp";
  public static final String SFTP_PROTOCOL = "sftp";

  public static final Set<String> ALL_SUPPORTED_PROTOCOLS = Set.of(
      HTTP_PROTOCOL,
      HTTPS_PROTOCOL,
      FTP_PROTOCOL,
      SFTP_PROTOCOL
  );
}
