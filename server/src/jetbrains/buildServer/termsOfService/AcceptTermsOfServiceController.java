package jetbrains.buildServer.termsOfService;

import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.controllers.login.RememberUrl;
import jetbrains.buildServer.controllers.overview.OverviewController;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import jetbrains.buildServer.web.util.SessionUser;
import jetbrains.buildServer.web.util.WebUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

import static jetbrains.buildServer.termsOfService.ViewTermsOfServiceController.TERMS_OF_SERVICE_JSP;
import static jetbrains.buildServer.termsOfService.TermsOfServiceLogger.LOGGER;

public class AcceptTermsOfServiceController extends BaseController {

    public static final String PATH = "/acceptTermsOfServices.html";
    private static final String ACCEPT_TERMS_OF_SERVICE_JSP = "acceptTermsOfService.jsp";

    @NotNull
    private final String myResourcesPath;
    @NotNull
    private final TermsOfServiceManager myManager;

    public AcceptTermsOfServiceController(@NotNull WebControllerManager webControllerManager,
                                          @NotNull PluginDescriptor descriptor,
                                          @NotNull TermsOfServiceManager manager) {
        myManager = manager;
        webControllerManager.registerController(PATH, this);
        myResourcesPath = descriptor.getPluginResourcesPath();
    }

    @Nullable
    @Override
    protected ModelAndView doHandle(@NotNull final HttpServletRequest request, @NotNull final HttpServletResponse response) throws IOException {
        SUser user = SessionUser.getUser(request);
        String agreementId = request.getParameter("agreement");

        if (agreementId == null) {
            LOGGER.warn("Request without agreement id detected " + WebUtil.getRequestDump(request));
            response.setStatus(404);
            return null;
        }

        Optional<TermsOfServiceManager.Agreement> agreement = myManager.findAgreement(agreementId);

        if (!agreement.isPresent()) {
            LOGGER.warn("Request for unknown agreement '" + agreementId + "'  detected: " + WebUtil.getRequestDump(request));
            response.setStatus(404);
            return redirectTo("/", response);
        }

        if (isPost(request)) {
            return accept(user, agreement.get(), request, response);
        } else {
            return show(user, agreement.get());
        }
    }


    private ModelAndView show(@NotNull SUser user, @NotNull TermsOfServiceManager.Agreement agreement) {
        ModelAndView view = new ModelAndView(agreement.isAccepted(user) ? myResourcesPath + TERMS_OF_SERVICE_JSP : myResourcesPath + ACCEPT_TERMS_OF_SERVICE_JSP);
        String agreementText = agreement.getText();
        if (agreementText == null) {
            return new ModelAndView(new RedirectView(agreement.getLink()));
        }
        view.addObject("agreementText", agreement.getText());
        view.addObject("termsOfServiceName", agreement.getFullName());
        view.addObject("consents", agreement.getConsents());
        return view;
    }


    private ModelAndView accept(@NotNull SUser user, @NotNull TermsOfServiceManager.Agreement agreement,
                                @NotNull HttpServletRequest request, @NotNull HttpServletResponse response) throws IOException {
        agreement.accept(user, request);

        for (TermsOfServiceManager.Consent consent : agreement.getConsents()) {
            if (request.getParameter(consent.getId()) != null) {
                agreement.changeConsentState(user, consent.getId(), true, request);
            }
        }

        String next = RememberUrl.readAndForget(request);
        if (next == null) {
            next = OverviewController.getOverviewPageUrl(request);
        }
        response.sendRedirect(next);
        return null;
    }

}

