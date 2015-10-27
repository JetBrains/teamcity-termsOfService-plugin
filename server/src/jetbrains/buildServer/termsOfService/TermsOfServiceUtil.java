package jetbrains.buildServer.termsOfService;

import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.users.SUser;
import org.jetbrains.annotations.NotNull;

public class TermsOfServiceUtil {

    public static UserCondition ANY_USER_NO_GUEST = new UserCondition() {
        @Override
        public boolean shouldAccept(@NotNull SUser user) {
            return hasPermissionChangeOwnProfile(user);
        }
    };

    public static UserCondition PROJECT_MANAGER_NO_GUEST = new UserCondition() {
        @Override
        public boolean shouldAccept(@NotNull SUser user) {
            return hasPermissionChangeOwnProfile(user) && hasProjectEditPermission(user);
        }
    };

    public static boolean hasProjectEditPermission(@NotNull  SUser user) {
        return user.isPermissionGrantedForAnyProject(Permission.EDIT_PROJECT);
    }

    public static boolean hasPermissionChangeOwnProfile(@NotNull  SUser user){
        return user.isPermissionGrantedForAnyProject(Permission.CHANGE_OWN_PROFILE);
    }
}
