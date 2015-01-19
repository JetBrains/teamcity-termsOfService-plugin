package jetbrains.buildServer.termsOfService;

import jetbrains.buildServer.users.SUser;
import org.jetbrains.annotations.NotNull;

public interface UserCondition {

    boolean shouldAccept(@NotNull SUser user);

}
