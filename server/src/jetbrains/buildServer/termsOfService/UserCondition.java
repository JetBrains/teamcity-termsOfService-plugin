package jetbrains.buildServer.termsOfService;

import jetbrains.buildServer.users.SUser;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface UserCondition {

    boolean shouldAccept(@NotNull SUser user);

}
