package com.company.file.model;

import com.company.file.util.CollectionUtil;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.net.URI;
import java.util.Set;

@Value
@Getter
@AllArgsConstructor
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
public class URIPreValidationResult {
  Set<URI> uriValidSet;
  Set<String> invalidSyntaxURISet;
  Set<String> invalidProtocolURISet;

  public boolean isAllUriValid() {
    return CollectionUtil.isEmpty(invalidSyntaxURISet) && CollectionUtil.isEmpty(invalidProtocolURISet);
  }
}
