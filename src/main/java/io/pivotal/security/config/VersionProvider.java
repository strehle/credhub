package io.pivotal.security.config;


import io.pivotal.security.util.ResourceReader;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class VersionProvider {
  private String version;

  VersionProvider(ResourceReader resources) {
    try {
      version = resources.readFileToString("version").trim();
    } catch (IOException | IllegalArgumentException e) {
      version = "0.0.0";
    }
  }

  public String currentVersion() {
    return version;
  }
}
