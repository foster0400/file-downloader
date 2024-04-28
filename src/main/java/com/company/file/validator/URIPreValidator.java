package com.company.file.validator;

import com.company.file.constant.SupportedProtocolConstant;
import com.company.file.model.URIPreValidationResult;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

public class URIPreValidator {
  private final Set<String> uriStringSet;

  private final Set<URI> uriValidSet;

  private final Set<String> invalidSyntaxURISet;
  private final Set<String> invalidProtocolURISet;

  private final URIPreValidationResult uriPreValidationResult;

  private URIPreValidator(Set<String> uriStringSet) {
    this.uriStringSet = uriStringSet;
    this.uriValidSet = new HashSet<>();
    this.invalidSyntaxURISet = new HashSet<>();
    this.invalidProtocolURISet = new HashSet<>();
    this.uriPreValidationResult = new URIPreValidationResult(uriValidSet, invalidSyntaxURISet, invalidProtocolURISet);
  }

  public static URIPreValidator initialise(Set<String> uriStringSet) {
    return new URIPreValidator(uriStringSet);
  }

  public URIPreValidator validateAll() {
    for (String uriString : uriStringSet) {
      URI uri = generateURI(uriString);
      if (uri == null) {
        invalidSyntaxURISet.add(uriString);
        continue;
      }

      if (!isValidProtocol(uri)) {
        invalidProtocolURISet.add(uriString);
        continue;
      }

      uriValidSet.add(uri);
    }
    return this;
  }

  private URI generateURI(String uriString) {
    try {
      return new URI(uriString);
    } catch (URISyntaxException e) {
      return null;
    }
  }

  private boolean isValidProtocol(URI uri) {
    if (uri.getScheme() == null) {
      return false;
    }

    final String protocol = uri.getScheme().toLowerCase();
    return SupportedProtocolConstant.ALL_SUPPORTED_PROTOCOLS.contains(protocol);
  }

  public URIPreValidationResult getUriPreValidationResult() {
    return this.uriPreValidationResult;
  }
}
