package jetbrains.buildServer.termsOfService;

import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.controllers.BaseControllerTestCase;
import jetbrains.buildServer.serverSide.MockServerPluginDescriptior;
import jetbrains.buildServer.serverSide.auth.*;
import jetbrains.buildServer.users.SUser;
import org.springframework.web.servlet.ModelAndView;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ProjectManagerTermsOfServiceControllerTest extends BaseControllerTestCase {

    @Override
    protected BaseController createController() {
        PropertyBasedConfig myConfig = new PropertyBasedConfig("property1", "PM Terms of Service", "Project Manager Terms of Service", "/pmTermsOfService.html", "_pm_text.jspf");
        PropertyBasedManager manager = new PropertyBasedManager(myConfig, TermsOfServiceUtil.PROJECT_MANAGER_NO_GUEST);
        myController = new TermsOfServiceController(myServer, myWebManager,
                new MockServerPluginDescriptior(),
                manager);
        return myController;
    }

    @Test
    public void test_no_user() throws Exception {
        ModelAndView modelAndView = doGet();
        Assert.assertNull(modelAndView);
    }

    @Test
    public void test_terms_of_service_for_user() throws Exception {
        SUser user1 = createUser("user1");
        makeLoggedIn(user1);
        Assert.assertFalse(TermsOfServiceUtil.hasProjectEditPermission(user1));
        ModelAndView modelAndView = doGet();
        Assert.assertNull(modelAndView);
    }

    @Test
    public void test_terms_of_service_for_pm() throws Exception {
        SUser user1 = createUser("user1");
        RolesManager rolesManager = myServer.getSingletonService(RolesManager.class);
        Role projectAdmin = rolesManager.findRoleById("PROJECT_ADMIN");
        Assert.assertNotNull(projectAdmin);
        user1.addRole(RoleScope.projectScope(myProject.getProjectId()), projectAdmin);
        makeLoggedIn(user1);
        Assert.assertTrue(TermsOfServiceUtil.hasProjectEditPermission(user1));
        ModelAndView modelAndView = doGet();
        Assert.assertNotNull(modelAndView);
        Assert.assertEquals(modelAndView.getViewName(), TermsOfServiceController.ACCEPT_TERMS_OF_SERVICE_JSP);
        Assert.assertEquals(modelAndView.getModel().get("contentFile"), "_pm_text.jspf");
        Assert.assertEquals(modelAndView.getModel().get("termsOfServiceName"), "Project Manager Terms of Service");
    }

    @Test
    public void test_terms_of_service_for_guest() throws Exception {
        SUser guestUser = myServer.getUserModel().getGuestUser();
        makeLoggedIn(guestUser);
        Assert.assertFalse(TermsOfServiceUtil.hasPermissionChangeOwnProfile(guestUser));
        ModelAndView modelAndView = doGet();
        Assert.assertNull(modelAndView);
    }



}