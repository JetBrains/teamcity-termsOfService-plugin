package jetbrains.buildServer.termsOfService;

import jetbrains.buildServer.users.SUser;
import org.jetbrains.annotations.NotNull;

public class PropertyBasedManager implements TermsOfServiceManager {

  @NotNull
  private final PropertyBasedConfig myConfig;

  public PropertyBasedManager(final @NotNull PropertyBasedConfig config) {
    myConfig = config;
  }

  @NotNull
  public TermsOfServiceConfig getConfig() {
    return myConfig;
  }

  public boolean isAccepted(@NotNull final SUser user) {
    return user.getBooleanProperty(myConfig.getKey());
  }

  public void accept(@NotNull final SUser user) {
    user.setUserProperty(myConfig.getKey(),"true");
  }

}
