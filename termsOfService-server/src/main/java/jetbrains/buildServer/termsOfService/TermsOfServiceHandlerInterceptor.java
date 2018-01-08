package jetbrains.buildServer.termsOfService;


import jetbrains.buildServer.controllers.interceptors.TeamCityHandlerInterceptor;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.web.util.SessionUser;
import jetbrains.buildServer.web.util.WebUtil;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

public class TermsOfServiceHandlerInterceptor implements TeamCityHandlerInterceptor {

    public static final String PROCEED_URL_PARAM = "proceedUrl";

    @NotNull
    private final TermsOfServiceManager myManager;

    public TermsOfServiceHandlerInterceptor(@NotNull TermsOfServiceManager manager) {
        myManager = manager;
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
            String entryPoint = AcceptTermsOfServiceController.PATH + "?agreement=" + agreement.getId() + "&" + PROCEED_URL_PARAM + "=" + WebUtil.encode(WebUtil.getRequestUrl(request));
            TermsOfServiceLogger.LOGGER.debug(String.format("Will redirect to %s. ", entryPoint));
            response.sendRedirect(request.getContextPath() + entryPoint);
            return false;
        }

        return true;
    }

}