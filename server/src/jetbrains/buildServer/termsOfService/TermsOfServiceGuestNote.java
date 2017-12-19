package jetbrains.buildServer.termsOfService;

import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.users.UserModel;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.PlaceId;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.SimplePageExtension;
import jetbrains.buildServer.web.util.SessionUser;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public class TermsOfServiceGuestNote extends SimplePageExtension {

    private final TermsOfServiceManager termsOfServiceManager;
    private final UserModel userModel;

    public TermsOfServiceGuestNote(@NotNull TermsOfServiceManager manager,
                                   @NotNull PagePlaces pagePlaces,
                                   @NotNull PluginDescriptor descriptor,
                                   @NotNull UserModel userModel) {
        super(pagePlaces, PlaceId.BEFORE_CONTENT, "TermsOfServicesGuestNote", descriptor.getPluginResourcesPath() + "/termsOfServiceGuestNote.jsp");
        termsOfServiceManager = manager;
        this.userModel = userModel;
        register();
    }

    @Override
    public void fillModel(@NotNull Map<String, Object> model, @NotNull HttpServletRequest request) {
        super.fillModel(model, request);
        termsOfServiceManager.getGuestNotice().ifPresent(notice -> model.put("guestNotice", notice));
    }

    @Override
    public boolean isAvailable(@NotNull HttpServletRequest request) {
        SUser user = SessionUser.getUser(request);
        return user != null && userModel.isGuestUser(user);
    }
}

