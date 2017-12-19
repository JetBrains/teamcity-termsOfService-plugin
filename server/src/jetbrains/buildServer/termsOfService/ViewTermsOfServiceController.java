package jetbrains.buildServer.termsOfService;

import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import jetbrains.buildServer.web.util.WebUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

import static jetbrains.buildServer.termsOfService.TermsOfServiceLogger.LOGGER;

public class ViewTermsOfServiceController extends BaseController {

    protected static final String TERMS_OF_SERVICE_JSP = "termsOfService.jsp";
    public static final String PATH = "/viewTermsOfServices.html";

    @NotNull
    private final String myResourcesPath;
    @NotNull
    private final TermsOfServiceManager myManager;

    public ViewTermsOfServiceController(@NotNull WebControllerManager webControllerManager,
                                        @NotNull PluginDescriptor descriptor,
                                        @NotNull TermsOfServiceManager manager) {
        myManager = manager;
        webControllerManager.registerController(PATH, this);
        myResourcesPath = descriptor.getPluginResourcesPath();
    }

    @Nullable
    @Override
    protected ModelAndView doHandle(@NotNull final HttpServletRequest request, @NotNull final HttpServletResponse response) {
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

        ModelAndView view = new ModelAndView(myResourcesPath + TERMS_OF_SERVICE_JSP);
        String agreementText = agreement.get().getText();
        if (agreementText == null) {
            return new ModelAndView(new RedirectView(agreement.get().getLink()));
        }

        view.addObject("agreementText", agreement.get().getText());
        view.addObject("termsOfServiceName", agreement.get().getFullName());
        return view;
    }
}

