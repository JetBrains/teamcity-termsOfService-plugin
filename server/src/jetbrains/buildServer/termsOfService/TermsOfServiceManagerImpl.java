package jetbrains.buildServer.termsOfService;

import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.users.PropertyKey;
import jetbrains.buildServer.users.SUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

public class TermsOfServiceManagerImpl implements TermsOfServiceManager {
    static final String TEAMCITY_TERMS_OF_SERVICE_ENABLED_PROPERTY = "teamcity.termsOfService.enabled";

    @NotNull
    private final TermsOfServiceConfig myConfig;

    public TermsOfServiceManagerImpl(final @NotNull TermsOfServiceConfig config) {
        myConfig = config;
    }

    @Override
    public boolean mustAccept(@NotNull SUser user) {
        return TeamCityProperties.getBoolean(TEAMCITY_TERMS_OF_SERVICE_ENABLED_PROPERTY)
                && getAgreementsFor(user).stream().anyMatch(a -> a.shouldAccept(user) && !a.isAccepted(user));
    }

    @NotNull
    @Override
    public List<Agreement> getAgreementsFor(@NotNull SUser user) {
        return myConfig.getAgreements(user).stream().map(AgreementImpl::new).collect(toList());
    }

    @NotNull
    @Override
    public Optional<Agreement> getAgreement(@NotNull SUser user, @NotNull String id) {
        return getAgreementsFor(user).stream().filter(a -> a.getId().equals(id)).findFirst();
    }


    private static final class AgreementImpl implements Agreement {

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
                    "/termsOfServices.html?agreement=" + agreementSettings.getId();
        }

        @NotNull
        @Override
        public PropertyKey getUserPropertyKey() {
            return agreementSettings.getUserPropertyKey();
        }

        @Override
        public boolean shouldAccept(@NotNull SUser user) {
            return TeamCityProperties.getBoolean(TEAMCITY_TERMS_OF_SERVICE_ENABLED_PROPERTY);
        }

        @Override
        public boolean isAccepted(@NotNull SUser user) {
            return user.getBooleanProperty(getUserPropertyKey());
        }

        @Override
        public void accept(@NotNull SUser user) {
            user.setUserProperty(getUserPropertyKey(), "true");
        }

        @Override
        public String toString() {
            return "Agreement " + agreementSettings.getShortName() + " (id = " + agreementSettings.getId() + ")";
        }
    }

}
