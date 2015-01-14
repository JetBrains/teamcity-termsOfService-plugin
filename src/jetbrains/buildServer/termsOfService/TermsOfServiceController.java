package jetbrains.buildServer.termsOfService;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.controllers.login.RememberUrl;
import jetbrains.buildServer.controllers.overview.OverviewController;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.users.impl.UserEx;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import jetbrains.buildServer.web.util.SessionUser;
import jetbrains.buildServer.web.util.WebUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

public class TermsOfServiceController extends BaseController {

  @NotNull private final SBuildServer myServer;
  @NotNull private final String myResourcesPath;
  @NotNull private final TermsOfServiceManager myManager;

  public TermsOfServiceController(@NotNull SBuildServer server,
                                  @NotNull WebControllerManager webControllerManager,
                                  @NotNull final PluginDescriptor descriptor,
                                  @NotNull TermsOfServiceManager manager) {
    myServer = server;
    webControllerManager.registerController("/termsOfService.html", this);
    myResourcesPath = descriptor.getPluginResourcesPath();
    myManager = manager;
  }

  @Nullable
  @Override
  protected ModelAndView doHandle(@NotNull final HttpServletRequest request, @NotNull final HttpServletResponse response) throws Exception {
    if (isPost(request)) {
      return doPost(request, response);
    } else {
      return doGet(request, response);
    }
  }


  protected ModelAndView doGet(@NotNull final HttpServletRequest request, @NotNull final HttpServletResponse response) {
    UserEx user = (UserEx)SessionUser.getUser(request);
    return new ModelAndView(!myManager.isAccepted(user) ?
                            myResourcesPath + "acceptTermsOfService.jsp" :
                            myResourcesPath + "termsOfService.jsp");
  }


  protected ModelAndView doPost(@NotNull final HttpServletRequest request, @NotNull final HttpServletResponse response) {
    UserEx user = (UserEx)SessionUser.getUser(request);
    myManager.accept(user);
    String next = RememberUrl.readAndForget(request);
    if (next == null) {
      next = OverviewController.getOverviewPageUrl(request);
    } else {
      next = WebUtil.getServletContext(request).getContextPath().concat(next);
    }
    try {
      response.sendRedirect(next);
    } catch (IOException e) {
      //
    }
    return null;
  }

}

