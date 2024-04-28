package com.company.file.model;

import com.company.file.enums.DownloadStatusEnum;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
public class DownloadBulkResult {
  DownloadStatusEnum downloadStatus;
  URIPreValidationResult uriPreValidationResult;

  public static DownloadBulkResult preValidationFailed(URIPreValidationResult uriPreValidationResult) {
    return new DownloadBulkResult(
        DownloadStatusEnum.PRE_VALIDATION_FAILED,
        uriPreValidationResult
    );
  }

  public static DownloadBulkResult preValidationSuccess() {
    return new DownloadBulkResult(
        DownloadStatusEnum.PRE_VALIDATION_SUCCESS,
        null
    );
  }
}
