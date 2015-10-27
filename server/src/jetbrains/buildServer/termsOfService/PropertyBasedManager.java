package jetbrains.buildServer.termsOfService;

import jetbrains.buildServer.users.SUser;
import org.jetbrains.annotations.NotNull;

public class PropertyBasedManager implements TermsOfServiceManager {

    @NotNull
    private final PropertyBasedConfig myConfig;
    @NotNull
    private UserCondition myCondition;


    public PropertyBasedManager(final @NotNull PropertyBasedConfig config, final @NotNull UserCondition condition) {
        myConfig = config;
        myCondition = condition;
    }

    @NotNull
    public TermsOfServiceConfig getConfig() {
        return myConfig;
    }

    public boolean isAccepted(@NotNull final SUser user) {
        return user.getBooleanProperty(myConfig.getKey());
    }

    @Override
    public boolean shouldAccept(@NotNull SUser user) {
        return myCondition.shouldAccept(user);
    }

    public void accept(@NotNull final SUser user) {
        user.setUserProperty(myConfig.getKey(), "true");
    }

}
