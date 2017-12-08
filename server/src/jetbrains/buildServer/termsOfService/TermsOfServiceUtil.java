package jetbrains.buildServer.termsOfService;

import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.users.SUser;
import org.jetbrains.annotations.NotNull;

public class TermsOfServiceUtil {

    public static UserCondition ANY_USER_NO_GUEST = TermsOfServiceUtil::hasPermissionChangeOwnProfile;

    public static boolean hasPermissionChangeOwnProfile(@NotNull  SUser user){
        return user.isPermissionGrantedForAnyProject(Permission.CHANGE_OWN_PROFILE);
    }
}
