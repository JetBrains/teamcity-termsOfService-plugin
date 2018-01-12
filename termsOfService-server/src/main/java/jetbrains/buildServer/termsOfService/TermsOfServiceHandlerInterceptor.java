package jetbrains.buildServer.termsOfService;


import jetbrains.buildServer.controllers.interceptors.TeamCityHandlerInterceptor;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.web.util.SessionUser;
import jetbrains.buildServer.web.util.WebUtil;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.List;

public class TermsOfServiceHandlerInterceptor implements TeamCityHandlerInterceptor {

    public static final String PROCEED_URL_PARAM = "proceedUrl";
    private static final String NO_AGREEMENTS_EXIST_WHEN_SESSION_WAS_CREATED_ATTR = "TeamCity_TermsOfService_NoAgreementsExistWhenSessionWasCreated";

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
        HttpSession session = request.getSession();

        if (user == null || session == null) {
            return true;
        }

        List<TermsOfServiceManager.Agreement> mustAcceptAgreements = myManager.getMustAcceptAgreements(user);

        if (mustAcceptAgreements.isEmpty()) {
            session.setAttribute(NO_AGREEMENTS_EXIST_WHEN_SESSION_WAS_CREATED_ATTR, true);
            return true;
        }

        TermsOfServiceManager.Agreement agreement = mustAcceptAgreements.get(0);

        if (session.getAttribute(NO_AGREEMENTS_EXIST_WHEN_SESSION_WAS_CREATED_ATTR) != null && !agreement.isEnforcedForActiveSessions()) {
            return true;
        }

        if (!path.startsWith(AcceptTermsOfServiceController.PATH)) {
            String entryPoint = AcceptTermsOfServiceController.PATH + "?agreement=" + agreement.getId() + "&" + PROCEED_URL_PARAM + "=" + WebUtil.encode(WebUtil.getRequestUrl(request));
            TermsOfServiceLogger.LOGGER.debug(String.format("Will redirect to %s. ", entryPoint));
            response.sendRedirect(request.getContextPath() + entryPoint);
            return false;
        }

        return true;
    }

}