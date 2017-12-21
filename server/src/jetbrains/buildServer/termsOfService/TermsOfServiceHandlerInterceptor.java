package jetbrains.buildServer.termsOfService;


import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.controllers.interceptors.PathSet;
import jetbrains.buildServer.controllers.interceptors.TeamCityHandlerInterceptor;
import jetbrains.buildServer.controllers.login.RememberUrl;
import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.web.util.SessionUser;
import jetbrains.buildServer.web.util.WebUtil;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

public class TermsOfServiceHandlerInterceptor implements TeamCityHandlerInterceptor {

    private static final Logger LOG = Logger.getInstance(TermsOfServiceHandlerInterceptor.class.getName());
    protected static final String ENTRY_POINT_PREFIX = "/termsOfServices";
    private static final String LOGOUT_PARAMETER = "logout";
    private static final String TEAMCITY_TERMS_OF_SERVICE_ENABLED_PROPERTY = "teamcity.termsOfService.enabled";

    @NotNull
    private final TermsOfServiceManager myManager;
    @NotNull
    private final PathSet myNonMemorizablePaths = new PathSet();


    public TermsOfServiceHandlerInterceptor(@NotNull TermsOfServiceManager manager) {
        LOG.debug("ServiceTermsExtension initialized. Manager: " + manager);
        myManager = manager;
    }

    public void setNonMemorizablePaths(final List<String> nonMemorizablePaths) {
        myNonMemorizablePaths.clear();
        myNonMemorizablePaths.addAll(nonMemorizablePaths);
    }

    public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
        if (!WebUtil.isProbablyBrowser(request)) {
            return true;
        }

        String path = WebUtil.getPathWithoutContext(request);
        LOG.debug("path=" + path);

        if(WebUtil.isAjaxRequest(request)){
            return true;
        }

        SUser user = SessionUser.getUser(request);
        if (user == null) {
            return true;
        }

        if (!TeamCityProperties.getBoolean(TEAMCITY_TERMS_OF_SERVICE_ENABLED_PROPERTY) || !myManager.shouldAccept(user) || myManager.isAccepted(user)) {
            return true;
        }

        if (!path.startsWith(ENTRY_POINT_PREFIX)) {
            String requestUrl = WebUtil.getOriginalPathWithoutContext(request);
            String entryPoint = getEntryPoint(myManager.getConfig().getPath());
            if (!myNonMemorizablePaths.matches(path)) {
                RememberUrl.remember(request, requestUrl);
            }
            LOG.debug(String.format("Will redirect to %s. Remembered original request url %s", entryPoint, requestUrl));
            response.sendRedirect(request.getContextPath() + entryPoint);
            return false;
        }
        return true;
    }

    public static String getEntryPoint(@NotNull String path){
        return ENTRY_POINT_PREFIX + path;
    }
}