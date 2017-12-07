package jetbrains.buildServer.termsOfService;

import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.controllers.BaseControllerTestCase;
import jetbrains.buildServer.serverSide.MockServerPluginDescriptior;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.util.FileUtil;
import org.springframework.web.servlet.ModelAndView;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;

public class TermsOfServiceControllerTest extends BaseControllerTestCase {

    private PropertyBasedConfig myConfig;

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
        myConfig = new PropertyBasedConfig("property1", "Terms of Service","Terms of Service",
                "_for_users.html","agreement.html",
                myFixture.getServerPaths());
        PropertyBasedManager manager = new PropertyBasedManager(myConfig, TermsOfServiceUtil.ANY_USER_NO_GUEST);
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
    public void test_terms_of_service_accepted() throws Exception {
        SUser user = createUser("user1");
        makeLoggedIn(user);
        user.setUserProperty(myConfig.getKey(), "true");
        ModelAndView modelAndView = doGet();
        Assert.assertNotNull(modelAndView);
        Assert.assertEquals(modelAndView.getViewName(), TermsOfServiceController.TERMS_OF_SERVICE_JSP);
    }

    @Test
    public void test_terms_of_service_not_accepted_yet() throws Exception {
        makeLoggedIn(createUser("user1"));
        ModelAndView modelAndView = doGet();
        Assert.assertNotNull(modelAndView);
        Assert.assertEquals(modelAndView.getViewName(), TermsOfServiceController.ACCEPT_TERMS_OF_SERVICE_JSP);
        Assert.assertEquals(modelAndView.getModel().get("agreementText"), FileUtil.readText(myAgreementFile, "UTF-8"));
        Assert.assertEquals(modelAndView.getModel().get("termsOfServiceName"), "Terms of Service");
    }

    @Test
    public void test_terms_flow() throws Exception {
        SUser user1 = createUser("user1");
        SUser user2 = createUser("user2");
        makeLoggedIn(user1);
        ModelAndView modelAndView = doGet();
        Assert.assertNotNull(modelAndView);
        Assert.assertEquals(modelAndView.getViewName(), TermsOfServiceController.ACCEPT_TERMS_OF_SERVICE_JSP);
        modelAndView = doPost();
        Assert.assertNull(modelAndView);
        Assert.assertTrue(user1.getBooleanProperty(myConfig.getKey()));
        modelAndView = doGet();
        Assert.assertNotNull(modelAndView);
        Assert.assertEquals(modelAndView.getViewName(), TermsOfServiceController.TERMS_OF_SERVICE_JSP);
        makeLoggedIn(user2);
        modelAndView = doGet();
        Assert.assertNotNull(modelAndView);
        Assert.assertEquals(modelAndView.getViewName(), TermsOfServiceController.ACCEPT_TERMS_OF_SERVICE_JSP);
    }
}
