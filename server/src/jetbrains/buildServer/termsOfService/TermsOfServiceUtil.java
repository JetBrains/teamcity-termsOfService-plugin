package jetbrains.buildServer.termsOfService;

import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.serverSide.auth.Permissions;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.users.impl.SecuredUser;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class TermsOfServiceUtil {

    public static UserCondition ANY_USER = new UserCondition() {
        @Override
        public boolean shouldAccept(@NotNull SUser user) {
            return true;
        }
    };

    public static UserCondition PROJECT_MANAGER = new UserCondition() {
        @Override
        public boolean shouldAccept(@NotNull SUser user) {
            return hasProjectAdminPermission(user);
        }
    };

    public static boolean hasProjectAdminPermission(SUser user) {
        return user.isPermissionGrantedForAnyProject(Permission.EDIT_PROJECT);
    }

}
