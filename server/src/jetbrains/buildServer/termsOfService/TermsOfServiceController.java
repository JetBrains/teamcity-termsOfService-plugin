package jetbrains.buildServer.termsOfService;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.intellij.openapi.diagnostic.Logger;
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

  private static final Logger LOG = Logger.getInstance(TermsOfServiceController.class.getName());
  protected static final String ACCEPT_TERMS_OF_SERVICE_JSP = "acceptTermsOfService.jsp";
  protected static final String TERMS_OF_SERVICE_JSP = "termsOfService.jsp";

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
    UserEx user = (UserEx)SessionUser.getUser(request);
    if (user==null){
      LOG.warn("User is set to null. TermsOfServiceController should not be executed.");
      return null;
    }
    if (isPost(request)) {
      return doPost(user, request, response);
    } else {
      return doGet(user, request, response);
    }
  }


  private ModelAndView doGet(@NotNull UserEx user, @NotNull final HttpServletRequest request, @NotNull final HttpServletResponse response) {
    return new ModelAndView(!myManager.isAccepted(user) ?
                            myResourcesPath + ACCEPT_TERMS_OF_SERVICE_JSP :
                            myResourcesPath + TERMS_OF_SERVICE_JSP);
  }


  private ModelAndView doPost(@NotNull UserEx user, @NotNull final HttpServletRequest request, @NotNull final HttpServletResponse response) {
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

