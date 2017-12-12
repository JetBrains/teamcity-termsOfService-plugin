package jetbrains.buildServer.termsOfService;

import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.users.SUser;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class TermsOfServiceManagerImpl implements TermsOfServiceManager {
    public static final String TEAMCITY_TERMS_OF_SERVICE_ENABLED_PROPERTY = "teamcity.termsOfService.enabled";

    @NotNull
    private final TermsOfServiceConfig myConfig;

    public TermsOfServiceManagerImpl(final @NotNull FileBasedConfig config) {
        myConfig = config;
    }

    @NotNull
    public TermsOfServiceConfig getConfig() {
        return myConfig;
    }

    public boolean isAccepted(@NotNull final SUser user) {
        return myConfig.getRule(user).map(rule -> user.getBooleanProperty(rule.getUserPropertyKey())).orElse(false);
    }

    @Override
    public boolean shouldAccept(@NotNull SUser user) {
        return TeamCityProperties.getBoolean(TEAMCITY_TERMS_OF_SERVICE_ENABLED_PROPERTY)
                && myConfig.getAgreementText(user) != null;
    }

    public void accept(@NotNull final SUser user) {
        myConfig.getRule(user).ifPresent(r -> user.setUserProperty(r.getUserPropertyKey(), "true"));
    }

}
