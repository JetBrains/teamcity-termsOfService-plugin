package jetbrains.buildServer.termsOfService;

import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.PlaceId;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.SimplePageExtension;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Map;

public class TermsOfServicesLink extends SimplePageExtension {

    private final Collection<TermsOfServiceManager> myManagers;

    public TermsOfServicesLink(@NotNull Collection<TermsOfServiceManager> managers,
                               @NotNull PagePlaces pagePlaces,
                               @NotNull PluginDescriptor descriptor) {
        super(pagePlaces, PlaceId.ALL_PAGES_FOOTER, "TermsOfServicesLink", descriptor.getPluginResourcesPath() + "/termsOfServiceLink.jsp");
        myManagers = managers;
        register();
    }

    @Override
    public void fillModel(@NotNull Map<String, Object> model, @NotNull HttpServletRequest request) {
        super.fillModel(model, request);
        model.put("termsOfServices", myManagers);
        model.put("entryPointPrefix", TermsOfServiceHandlerInterceptor.ENTRY_POINT_PREFIX);
    }
}
