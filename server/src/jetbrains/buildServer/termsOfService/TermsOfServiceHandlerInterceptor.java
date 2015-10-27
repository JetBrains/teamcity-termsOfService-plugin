package jetbrains.buildServer.termsOfService;


import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import jetbrains.buildServer.controllers.interceptors.TeamCityHandlerInterceptor;
import jetbrains.buildServer.controllers.login.RememberUrl;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.web.util.SessionUser;
import jetbrains.buildServer.web.util.WebUtil;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TermsOfServiceHandlerInterceptor implements TeamCityHandlerInterceptor {

    private static final Logger LOG = Logger.getInstance(TermsOfServiceHandlerInterceptor.class.getName());
    protected static final String ENTRY_POINT_PREFIX = "/termsOfServices";
    private static final String AJAX_PREFIX = "/ajax.html";
    private static final String LOGOUT_PARAMETER = "logout";

    @NotNull
    private final TermsOfServiceManager myManager;


    public TermsOfServiceHandlerInterceptor(@NotNull TermsOfServiceManager manager) {
        LOG.debug("ServiceTermsExtension initialized. Manager: " + manager);
        myManager = manager;
    }

    public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
        if (!WebUtil.isProbablyBrowser(request)) {
            return true;
        }

        String path = WebUtil.getPathWithoutContext(request);
        LOG.debug("path=" + path);

        if (path.startsWith(AJAX_PREFIX)){
            return true;
        }

        SUser user = SessionUser.getUser(request);
        if (user == null) {
            return true;
        }

        if (!myManager.shouldAccept(user) || myManager.isAccepted(user)) {
            return true;
        }

        String entryPoint = getEntryPoint(myManager.getConfig().getPath());

        if (!path.startsWith(ENTRY_POINT_PREFIX)) {
            LOG.debug("Will redirect to " + entryPoint);
            RememberUrl.remember(request, path);
            response.sendRedirect(request.getContextPath() + entryPoint);
            return false;
        }
        return true;
    }

    public static String getEntryPoint(@NotNull String path){
        return ENTRY_POINT_PREFIX + path;
    }
}