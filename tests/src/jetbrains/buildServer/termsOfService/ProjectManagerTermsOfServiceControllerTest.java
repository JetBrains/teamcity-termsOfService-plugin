package jetbrains.buildServer.termsOfService;

import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.controllers.BaseControllerTestCase;
import jetbrains.buildServer.serverSide.MockServerPluginDescriptior;
import jetbrains.buildServer.serverSide.auth.*;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.util.FileUtil;
import org.springframework.web.servlet.ModelAndView;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;

public class ProjectManagerTermsOfServiceControllerTest extends BaseControllerTestCase {

    private File myAgreementFile;
    @Override
    @BeforeMethod
    public void setUp() throws Exception {
        super.setUp();
        File config = new File(myFixture.getServerPaths().getConfigDir(), "termsOfService");
        myAgreementFile = new File(config, "agreement.html");
        FileUtil.createIfDoesntExist(myAgreementFile);
        FileUtil.writeFile(myAgreementFile, "Agreement", "UTF-8");
    }

    @Override
    protected BaseController createController() {
        PropertyBasedConfig myConfig = new PropertyBasedConfig("property1", "PM Terms of Service",
                "Project Manager Terms of Service", "/pmTermsOfService.html",
                "agreement.html",
                myFixture.getServerPaths());
        PropertyBasedManager manager = new PropertyBasedManager(myConfig, TermsOfServiceUtil.PROJECT_MANAGER_NO_GUEST);
        myController = new TermsOfServiceController(myWebManager,
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
        Assert.assertEquals(modelAndView.getModel().get("agreementText"), FileUtil.readText(myAgreementFile, "UTF-8"));
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