package jetbrains.buildServer.termsOfService;

import jetbrains.buildServer.users.SUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;

public interface TermsOfServiceManager {

    @NotNull
    List<Agreement> getMustAcceptAgreements(@NotNull SUser user);

    @NotNull
    List<Agreement> getAgreements();

    @NotNull
    Optional<Agreement> findAgreement(@NotNull String id);

    @NotNull
    List<ExternalAgreementLink> getExternalAgreements();

    @NotNull
    Optional<GuestNotice> getGuestNotice();

    interface Agreement {

        @NotNull
        String getId();

        @NotNull
        String getShortName();

        @NotNull
        String getFullName();

        @Nullable
        String getText();

        @NotNull
        String getLink();

        @NotNull
        List<Consent> getConsents();

        boolean isAccepted(@NotNull SUser user);

        void accept(@NotNull SUser user, @NotNull HttpServletRequest request);

        void changeConsentState(@NotNull SUser user, @NotNull String consentId, boolean agreed, @NotNull HttpServletRequest request);
    }

    interface GuestNotice {
        @NotNull
        String getText();

        @NotNull
        String getLink();
    }

    interface Consent {
        @NotNull
        String getId();

        boolean isCheckedByDefault();

        @NotNull
        String getText();
    }

    interface ExternalAgreementLink {
        @NotNull
        String getName();

        @NotNull
        String getUrl();
    }
}
