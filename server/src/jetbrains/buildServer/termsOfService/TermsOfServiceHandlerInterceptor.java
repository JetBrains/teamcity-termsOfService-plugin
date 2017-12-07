package jetbrains.buildServer.termsOfService;


import jetbrains.buildServer.controllers.interceptors.TeamCityHandlerInterceptor;
import jetbrains.buildServer.controllers.login.RememberUrl;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.web.util.SessionUser;
import jetbrains.buildServer.web.util.WebUtil;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TermsOfServiceHandlerInterceptor implements TeamCityHandlerInterceptor {

    protected static final String ENTRY_POINT_PREFIX = "/termsOfServices";

    @NotNull
    private final TermsOfServiceManager myManager;

    public TermsOfServiceHandlerInterceptor(@NotNull TermsOfServiceManager manager) {
        TermsOfServiceManager.LOGGER.debug("ServiceTermsExtension initialized. Manager: " + manager);
        myManager = manager;
    }

    public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
        if (!WebUtil.isProbablyBrowser(request)) {
            return true;
        }

        String path = WebUtil.getPathWithoutContext(request);
        TermsOfServiceManager.LOGGER.debug("path=" + path);

        if(WebUtil.isAjaxRequest(request)){
            return true;
        }

        SUser user = SessionUser.getUser(request);
        if (user == null) {
            return true;
        }

        if (!myManager.isEnabled() || !myManager.shouldAccept(user) || myManager.isAccepted(user)) {
            return true;
        }

        if (!path.startsWith(ENTRY_POINT_PREFIX)) {
            String requestUrl = WebUtil.getOriginalPathWithoutContext(request);
            String entryPoint = getEntryPoint(myManager.getConfig().getPath());
            TermsOfServiceManager.LOGGER.debug(String.format("Will redirect to %s. Remembered original request url %s", entryPoint, requestUrl));
            RememberUrl.remember(request, requestUrl);
            response.sendRedirect(request.getContextPath() + entryPoint);
            return false;
        }
        return true;
    }

    public static String getEntryPoint(@NotNull String path){
        return ENTRY_POINT_PREFIX + path;
    }
}