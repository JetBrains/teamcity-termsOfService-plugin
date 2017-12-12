package jetbrains.buildServer.termsOfService;

import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.users.SUser;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class TermsOfServiceManagerImpl implements TermsOfServiceManager {
    static final String TEAMCITY_TERMS_OF_SERVICE_ENABLED_PROPERTY = "teamcity.termsOfService.enabled";

    @NotNull
    private final TermsOfServiceConfig myConfig;

    public TermsOfServiceManagerImpl(final @NotNull TermsOfServiceConfig config) {
        myConfig = config;
    }

    public boolean isAccepted(@NotNull final SUser user) {
        return getAgreementFor(user).map(rule -> user.getBooleanProperty(rule.getUserPropertyKey())).orElse(false);
    }

    @Override
    public boolean shouldAccept(@NotNull SUser user) {
        return TeamCityProperties.getBoolean(TEAMCITY_TERMS_OF_SERVICE_ENABLED_PROPERTY)
                && getAgreementFor(user).isPresent();
    }

    public void accept(@NotNull final SUser user) {
        getAgreementFor(user).ifPresent(r -> user.setUserProperty(r.getUserPropertyKey(), "true"));
    }

    @NotNull
    @Override
    public Optional<Agreement> getAgreementFor(@NotNull SUser user) {
        return myConfig.getRule(user);
    }

}
