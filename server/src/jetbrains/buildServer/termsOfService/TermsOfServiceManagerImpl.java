package jetbrains.buildServer.termsOfService;

import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.users.SimplePropertyKey;
import jetbrains.buildServer.users.UserModel;
import jetbrains.buildServer.util.TimeService;
import jetbrains.buildServer.web.util.WebUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

public class TermsOfServiceManagerImpl implements TermsOfServiceManager {
    static final String TEAMCITY_TERMS_OF_SERVICE_ENABLED_PROPERTY = "teamcity.termsOfService.enabled";

    @NotNull
    private final TermsOfServiceConfig myConfig;
    @NotNull
    private final UserModel userModel;
    @NotNull
    private final TimeService timeService;

    public TermsOfServiceManagerImpl(final @NotNull TermsOfServiceConfig config, @NotNull UserModel userModel,
                                     @NotNull TimeService timeService) {
        myConfig = config;
        this.userModel = userModel;
        this.timeService = timeService;
    }

    @NotNull
    @Override
    public List<Agreement> getMustAcceptAgreements(@NotNull SUser user) {
        if (!TeamCityProperties.getBoolean(TEAMCITY_TERMS_OF_SERVICE_ENABLED_PROPERTY)) {
            return Collections.emptyList();
        }
        if (userModel.isGuestUser(user)) {
            return Collections.emptyList();
        }

        return getAgreements().stream().filter(a -> !a.isAccepted(user) && a.shouldAccept(user)).collect(toList());
    }

    @NotNull
    public List<Agreement> getAgreements() {
        return myConfig.getAgreements().stream().map(AgreementImpl::new).collect(toList());
    }

    @NotNull
    @Override
    public Optional<Agreement> findAgreement(@NotNull String id) {
        return getAgreements().stream().filter(a -> a.getId().equals(id)).findFirst();
    }

    @NotNull
    @Override
    public Optional<GuestNotice> getGuestNotice() {
        Optional<TermsOfServiceConfig.GuestNoticeSettings> guestNotice = myConfig.getGuestNotice();
        if (!guestNotice.isPresent()) {
            return Optional.empty();
        }

        Optional<TermsOfServiceConfig.AgreementSettings> agreement = myConfig.getAgreement(guestNotice.get().getAgreementId());

        if (agreement.isPresent()) {
            return guestNotice.map(s -> new GuestNotice() {
                @NotNull
                @Override
                public String getText() {
                    return s.getText();
                }

                @NotNull
                @Override
                public String getLink() {
                    return new AgreementImpl(agreement.get()).getLink();
                }
            });
        } else {
            return Optional.empty();
        }
    }

    private final class AgreementImpl implements Agreement {

        private final TermsOfServiceConfig.AgreementSettings agreementSettings;

        private AgreementImpl(TermsOfServiceConfig.AgreementSettings agreementSettings) {
            this.agreementSettings = agreementSettings;
        }

        @NotNull
        @Override
        public String getId() {
            return agreementSettings.getId();
        }

        @NotNull
        @Override
        public String getShortName() {
            return agreementSettings.getShortName();
        }

        @NotNull
        @Override
        public String getFullName() {
            return agreementSettings.getFullName();
        }

        @Nullable
        @Override
        public String getText() {
            return agreementSettings.getText();
        }

        @NotNull
        @Override
        public String getLink() {
            return agreementSettings.getLink() != null ?
                    agreementSettings.getLink() :
                    ViewTermsOfServiceController.PATH + "?agreement=" + agreementSettings.getId();
        }

        @Override
        public boolean isAccepted(@NotNull SUser user) {
            return user.getPropertyValue(new SimplePropertyKey("teamcity.policy." + agreementSettings.getId() + ".acceptedVersion")) != null;
        }

        public boolean shouldAccept(@NotNull SUser user) {
            return agreementSettings.getForceAccept();
        }

        @Override
        public void accept(@NotNull SUser user, @NotNull HttpServletRequest request) {
            user.setUserProperty(new SimplePropertyKey("teamcity.policy." + agreementSettings.getId() + ".acceptedVersion"), agreementSettings.getVersion());
            user.setUserProperty(new SimplePropertyKey("teamcity.policy." + agreementSettings.getId() + ".acceptedDate"), new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(timeService.now()));
            user.setUserProperty(new SimplePropertyKey("teamcity.policy." + agreementSettings.getId() + ".acceptedFromIP"), WebUtil.getRemoteAddress(request));
        }

        @Override
        public String toString() {
            return "Agreement " + agreementSettings.getShortName() + " (id = " + agreementSettings.getId() + ")";
        }
    }

}
