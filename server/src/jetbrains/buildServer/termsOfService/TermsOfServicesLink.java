package jetbrains.buildServer.termsOfService;

import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.PlaceId;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.SimplePageExtension;
import jetbrains.buildServer.web.util.SessionUser;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

public class TermsOfServicesLink extends SimplePageExtension {

    private final TermsOfServiceManager termsOfServiceManager;

    public TermsOfServicesLink(@NotNull TermsOfServiceManager manager,
                               @NotNull PagePlaces pagePlaces,
                               @NotNull PluginDescriptor descriptor) {
        super(pagePlaces, PlaceId.ALL_PAGES_FOOTER, "TermsOfServicesLink", descriptor.getPluginResourcesPath() + "/termsOfServiceLink.jsp");
        termsOfServiceManager = manager;
        register();
    }

    @Override
    public void fillModel(@NotNull Map<String, Object> model, @NotNull HttpServletRequest request) {
        super.fillModel(model, request);

        SUser user = SessionUser.getUser(request);
        if (user != null) {
            model.put("agreements", termsOfServiceManager.getAgreements());
            model.put("externalAgreements", termsOfServiceManager.getExternalAgreements());
        }
    }
}
