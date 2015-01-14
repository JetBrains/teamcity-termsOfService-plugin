package jetbrains.buildServer.termsOfService;


import com.intellij.openapi.diagnostic.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import jetbrains.buildServer.controllers.interceptors.TeamCityHandlerInterceptor;
import jetbrains.buildServer.controllers.login.RememberUrl;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.web.util.SessionUser;
import jetbrains.buildServer.web.util.WebUtil;
import org.jetbrains.annotations.NotNull;

public class TermsOfServiceHandlerInterceptor implements TeamCityHandlerInterceptor {

  private static final Logger LOG = Logger.getInstance(TermsOfServiceHandlerInterceptor.class.getName());

  @NotNull private final TermsOfServiceManager myManager;


  public TermsOfServiceHandlerInterceptor(@NotNull TermsOfServiceManager manager) {
    LOG.debug("ServiceTermsExtension initialized. Config: " + manager.getConfig());
    myManager = manager;
  }

  public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
    if (!WebUtil.isProbablyBrowser(request)) {
      return true;
    }

    String path = WebUtil.getPathWithoutContext(request);
    LOG.debug("path=" + path);

    SUser user = SessionUser.getUser(request);
    if (user == null) {
      return true;
    }

    if (myManager.isAccepted(user)) {
      return true;
    }

    if (!path.equals(myManager.getConfig().getPath())) {
      LOG.debug("Will redirect to " + myManager.getConfig().getPath());
      RememberUrl.remember(request, path);
      response.sendRedirect(request.getContextPath() + myManager.getConfig().getPath());
      return false;
    }
    return true;
  }

}