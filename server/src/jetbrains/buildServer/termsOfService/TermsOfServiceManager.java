package jetbrains.buildServer.termsOfService;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.users.PropertyKey;
import jetbrains.buildServer.users.SUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public interface TermsOfServiceManager {
    Logger LOGGER = Logger.getInstance("jetbrains.buildServer.TermsOfService");

    boolean mustAccept(@NotNull SUser user);

    @NotNull
    List<Agreement> getAgreementsFor(@NotNull SUser user);

    @NotNull
    Optional<Agreement> getAgreement(@NotNull SUser user, @NotNull String id);

    interface Agreement {
        @NotNull
        String getId();

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

        boolean shouldAccept(@NotNull SUser user);

        boolean isAccepted(@NotNull SUser user);

        void accept(@NotNull SUser user);
    }
}
