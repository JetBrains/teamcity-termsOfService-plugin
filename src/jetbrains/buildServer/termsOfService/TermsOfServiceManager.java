package jetbrains.buildServer.termsOfService;

import jetbrains.buildServer.users.SUser;
import org.jetbrains.annotations.NotNull;

public interface TermsOfServiceManager {

  @NotNull
  TermsOfServiceConfig getConfig();

  boolean isAccepted(@NotNull SUser user);

  void accept(@NotNull SUser user);
}
