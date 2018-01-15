package jetbrains.buildServer.termsOfService;

import jetbrains.buildServer.controllers.*;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.web.openapi.*;
import jetbrains.buildServer.web.util.SessionUser;
import jetbrains.buildServer.web.util.WebUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TermsOfServiceUserProfileExtension extends BaseFormXmlController {

    public static final String PATH = "/userProfileTermsOfService.html";

    private final TermsOfServiceManager termsOfServiceManager;
    private final PluginDescriptor descriptor;

    public TermsOfServiceUserProfileExtension(@NotNull TermsOfServiceManager manager,
                                              @NotNull PagePlaces pagePlaces,
                                              @NotNull PluginDescriptor descriptor,
                                              @NotNull WebControllerManager webControllerManager) {
        webControllerManager.registerController(PATH, this);
        this.descriptor = descriptor;
        this.termsOfServiceManager = manager;

        SimpleCustomTab userProfileTab = new SimpleCustomTab(pagePlaces) {

            private final ThreadLocal<String> tabTitle = new ThreadLocal<>();

            @NotNull
            @Override
            public String getTabTitle() {
                String title = tabTitle.get();
                return title != null ? title : "Privacy";
            }

            @Override
            public boolean isAvailable(@NotNull HttpServletRequest request) {
                TermsOfServiceManager.Agreement agreement = getAgreementWithConsents(SessionUser.getUser(request));
                if (agreement != null) {
                    tabTitle.set(agreement.getShortName());
                    return true;
                }
                return false;
            }
        };

        userProfileTab.setPlaceId(PlaceId.MY_TOOLS_TABS);
        userProfileTab.setPluginName("TermsOfServicesUserConsents");
        userProfileTab.setIncludeUrl(PATH);
        userProfileTab.register();
    }

    @Override
    protected ModelAndView doGet(@NotNull HttpServletRequest request, @NotNull HttpServletResponse httpServletResponse) {
        ModelAndView modelAndView = new ModelAndView(descriptor.getPluginResourcesPath() + "/termsOfServiceUserProfile.jsp");
        Form form = getOrCreateBean(request);
        if (form == null) {
            return redirectTo("/profile.html", httpServletResponse);
        }
        modelAndView.getModel().put("form", form);
        return modelAndView;
    }

    @Override
    protected void doPost(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Element element) {
        Form form = getOrCreateBean(request);

        if (form == null) {
            writeRedirect(element, "/profile.html");
            return;
        }

        form.getAgreement().getConsents().forEach(consent -> {
            if (request.getParameter(consent.getId()) != null) {
                form.setConsentState(consent.getId(), true);
            } else {
                form.setConsentState(consent.getId(), false);
            }
        });

        if ("storeInSession".equals(request.getParameter("submitUserConsents"))) {
            XmlResponseUtil.writeFormModifiedIfNeeded(element, form);
            return;
        }

        form.agreement.getConsents().forEach(consent -> {
            consent.changeAcceptedState(SessionUser.getUser(request), form.consentsState.getOrDefault(consent.getId(), false), WebUtil.getRemoteAddress(request));
        });

        forgetFormBean(request, Form.class);

        getOrCreateMessages(request).addMessage("consentsSaved", "Your changes have been saved.");
    }


    @Nullable
    private Form getOrCreateBean(@NotNull HttpServletRequest request) {
        TermsOfServiceManager.Agreement agreement = getAgreementWithConsents(SessionUser.getUser(request));
        if (agreement == null) {
            return null;
        }
        return FormUtil.getOrCreateForm(request, Form.class, r -> {
            Form form = new Form(agreement, SessionUser.getUser(request));
            form.rememberState();
            return form;
        });
    }

    @Nullable
    private TermsOfServiceManager.Agreement getAgreementWithConsents(@NotNull SUser user) {
        List<TermsOfServiceManager.Agreement> agreements = termsOfServiceManager.getAgreements(user);
        if (agreements.isEmpty()) {
            return null;
        }

        TermsOfServiceManager.Agreement first = agreements.get(0);
        if (!first.getConsents().isEmpty()) {
            return first;
        }

        return null;
    }

    public static final class Form extends RememberState {
        @StateField
        private final Map<String, Boolean> consentsState;

        private final TermsOfServiceManager.Agreement agreement;

        private Form(@NotNull TermsOfServiceManager.Agreement agreement, @NotNull SUser user) {
            this.agreement = agreement;
            consentsState = agreement.getConsents().stream().collect(Collectors.toMap(TermsOfServiceManager.Consent::getId, c -> c.isAccepted(user)));
        }

        public Map<String, Boolean> getConsentStates() {
            return consentsState;
        }

        public TermsOfServiceManager.Agreement getAgreement() {
            return agreement;
        }

        private void setConsentState(String id, boolean accepted) {
            consentsState.put(id, accepted);
        }
    }
}