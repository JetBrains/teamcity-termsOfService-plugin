package jetbrains.buildServer.termsOfService;


import jetbrains.buildServer.users.PropertyKey;
import jetbrains.buildServer.users.SUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public interface TermsOfServiceConfig {

    /**
     * @return agreement text or null when text is not defined.
     */
    @Nullable
    String getAgreementText(@NotNull SUser user);

    @NotNull
    Optional<Rule> getRule(@NotNull SUser user);

    interface Rule {

        @NotNull
        String getAgreementShortName();

        @NotNull
        String getAgreementFullName();

        @NotNull
        String getAgreementFileName();

        @NotNull
        PropertyKey getUserPropertyKey();
    }

}
