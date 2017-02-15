package jetbrains.buildServer.termsOfService;

import jetbrains.buildServer.controllers.admin.AdminPermissionsUtil;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.users.impl.UserEx;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.servlet.ModelAndView;

public class ProjectAdminTermsOfServiceController extends TermsOfServiceController {

    @NotNull
    private final AdminPermissionsUtil myAdminPermissionsUtil;

    public ProjectAdminTermsOfServiceController(@NotNull SBuildServer server,
                                                @NotNull WebControllerManager webControllerManager,
                                                @NotNull PluginDescriptor descriptor,
                                                @NotNull TermsOfServiceManager manager,
                                                @NotNull  AdminPermissionsUtil adminPermissionsUtil) {
        super(server, webControllerManager, descriptor, manager);
        myAdminPermissionsUtil = adminPermissionsUtil;
    }

    @Override
    protected ModelAndView doGet(@NotNull UserEx user) {
        ModelAndView mv = super.doGet(user);
        mv.getModel().put("descriptionFile","projectAdminDescription.jsp");
        mv.getModel().put("projects", myAdminPermissionsUtil.getAllEditableProjects());
        return mv;
    }
}

