package com.company.file.model.config;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
public class DownloadConfiguration {
  String downloadLocation;
  int numberOfRetry;
  int corePoolSize;
}
