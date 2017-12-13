package jetbrains.buildServer.termsOfService;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.users.PropertyKey;
import jetbrains.buildServer.users.SUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public interface TermsOfServiceManager {
    Logger LOGGER = Logger.getInstance("jetbrains.buildServer.TermsOfService");

    boolean isAccepted(@NotNull SUser user);

    boolean shouldAccept(@NotNull SUser user);

    void accept(@NotNull SUser user);

    @NotNull
    Optional<Agreement> getAgreementFor(@NotNull SUser user);

    interface Agreement {

        @NotNull
        String getShortName();

        @NotNull
        String getFullName();

        @Nullable
        String getText();

        @Nullable
        String getLink();

        @NotNull
        PropertyKey getUserPropertyKey();
    }
}
