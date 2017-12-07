package jetbrains.buildServer.termsOfService;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.users.SUser;
import org.jetbrains.annotations.NotNull;

public interface TermsOfServiceManager {
    Logger LOGGER = Logger.getInstance("jetbrains.buildServer.TermsOfService");

    boolean isEnabled();

    @NotNull
    TermsOfServiceConfig getConfig();

    boolean isAccepted(@NotNull SUser user);

    boolean shouldAccept(@NotNull SUser user);

    void accept(@NotNull SUser user);
}
