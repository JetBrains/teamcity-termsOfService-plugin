package jetbrains.buildServer.termsOfService;


import jetbrains.buildServer.controllers.interceptors.PathSet;
import jetbrains.buildServer.controllers.interceptors.TeamCityHandlerInterceptor;
import jetbrains.buildServer.controllers.login.RememberUrl;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.web.util.SessionUser;
import jetbrains.buildServer.web.util.WebUtil;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

public class TermsOfServiceHandlerInterceptor implements TeamCityHandlerInterceptor {

    @NotNull
    private final TermsOfServiceManager myManager;

    @NotNull
    private final PathSet myNonMemorizablePaths = new PathSet();

    public TermsOfServiceHandlerInterceptor(@NotNull TermsOfServiceManager manager) {
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

        if(WebUtil.isAjaxRequest(request)){
            return true;
        }

        SUser user = SessionUser.getUser(request);
        if (user == null) {
            return true;
        }

        List<TermsOfServiceManager.Agreement> mustAcceptAgreements = myManager.getMustAcceptAgreements(user);
        if (mustAcceptAgreements.isEmpty()) {
            return true;
        }

        TermsOfServiceManager.Agreement agreement = mustAcceptAgreements.get(0);

        if (!path.startsWith(AcceptTermsOfServiceController.PATH)) {
            String requestUrl = WebUtil.getRequestUrl(request);
            String entryPoint = AcceptTermsOfServiceController.PATH + "?agreement=" + agreement.getId();
            TermsOfServiceLogger.LOGGER.debug(String.format("Will redirect to %s. Remembered original request url %s", entryPoint, requestUrl));
            if (!myNonMemorizablePaths.matches(path)) {
                RememberUrl.remember(request, requestUrl);
            }
            response.sendRedirect(request.getContextPath() + entryPoint);
            return false;
        }

        return true;
    }

}