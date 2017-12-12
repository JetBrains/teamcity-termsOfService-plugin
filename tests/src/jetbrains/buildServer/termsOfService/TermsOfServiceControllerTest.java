package jetbrains.buildServer.termsOfService;

import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.controllers.BaseControllerTestCase;
import jetbrains.buildServer.serverSide.MockServerPluginDescriptior;
import jetbrains.buildServer.serverSide.impl.BuildServerImpl;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.util.FileUtil;
import org.springframework.web.servlet.ModelAndView;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;

import static jetbrains.buildServer.termsOfService.TermsOfServiceManagerImpl.TEAMCITY_TERMS_OF_SERVICE_ENABLED_PROPERTY;

public class TermsOfServiceControllerTest extends BaseControllerTestCase {

    private File myAgreementFile;
    private TermsOfServiceManagerImpl termsOfServiceManager;

    @Override
    protected void configurePlugins(BuildServerImpl server) {
        setInternalProperty(TEAMCITY_TERMS_OF_SERVICE_ENABLED_PROPERTY, "true");
        File termsOfServiceDir = new File(myFixture.getServerPaths().getConfigDir(), "termsOfService");
        File configFile = new File(termsOfServiceDir, "terms-of-service-config.xml");
        FileUtil.createIfDoesntExist(configFile);
        FileUtil.writeFile(configFile,
                "<terms-of-service-config>\n" +
                        "<rule type=\"ALL_USERS\">\n" +
                        "<parameters>\n" +
                        "<param name=\"agreement-file\" value=\"agreement.html\"/>\n" +
                        "</parameters>\n"  +
                        "</rule>\n" +
                        "</terms-of-service-config>"
        );
        myAgreementFile = new File(termsOfServiceDir, "agreement.html");
        FileUtil.createIfDoesntExist(myAgreementFile);
        FileUtil.writeFile(myAgreementFile, "Agreement");

        termsOfServiceManager = new TermsOfServiceManagerImpl(new FileBasedConfig(myFixture.getEventDispatcher(), myFixture.getServerPaths()));
    }

    @Override
    protected BaseController createController() {
        return new TermsOfServiceController(myWebManager, new MockServerPluginDescriptior(), termsOfServiceManager);
    }

    @Test
    public void test_no_user() throws Exception {
        ModelAndView modelAndView = doGet();
        Assert.assertNull(modelAndView);
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
        Assert.assertEquals(modelAndView.getViewName(), TermsOfServiceController.ACCEPT_TERMS_OF_SERVICE_JSP);

        modelAndView = doPost();
        Assert.assertNull(modelAndView);
        Assert.assertTrue(termsOfServiceManager.isAccepted(user1));

        modelAndView = doGet();
        Assert.assertNotNull(modelAndView);
        Assert.assertEquals(modelAndView.getViewName(), TermsOfServiceController.TERMS_OF_SERVICE_JSP);

        makeLoggedIn(user2);
        modelAndView = doGet();
        Assert.assertNotNull(modelAndView);
        Assert.assertEquals(modelAndView.getViewName(), TermsOfServiceController.ACCEPT_TERMS_OF_SERVICE_JSP);
    }
}
