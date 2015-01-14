package jetbrains.buildServer.termsOfService;

import jetbrains.buildServer.users.PropertyKey;
import jetbrains.buildServer.users.SimplePropertyKey;
import org.jetbrains.annotations.NotNull;

public class PropertyBasedConfig implements TermsOfServiceConfig {
  @NotNull
  private final PropertyKey myKey;
  private final String myPath;

  public PropertyBasedConfig(final @NotNull String key, final String path) {
    myKey = new SimplePropertyKey(key);
    myPath = path;
  }

  @NotNull
  public PropertyKey getKey() {
    return myKey;
  }

  public String getPath() {
    return myPath;
  }

}

