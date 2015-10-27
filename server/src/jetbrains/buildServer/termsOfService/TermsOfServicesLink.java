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

        model.put("termsOfServices", filter(myManagers, request));

        model.put("entryPointPrefix", TermsOfServiceHandlerInterceptor.ENTRY_POINT_PREFIX);
    }

    @NotNull
    private List<TermsOfServiceManager> filter(@NotNull Collection<TermsOfServiceManager> managers,
                                               @NotNull HttpServletRequest request) {
        List<TermsOfServiceManager> result = new ArrayList<TermsOfServiceManager>();
        SUser user = SessionUser.getUser(request);
        if (user == null) {
            return Collections.emptyList();
        }
        for (TermsOfServiceManager manager : managers) {
            if (!manager.shouldAccept(user)) {
                continue;
            }
            result.add(manager);
        }
        return result;
    }
}
