package jetbrains.buildServer.termsOfService;

import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.controllers.BaseControllerTestCase;
import jetbrains.buildServer.serverSide.MockServerPluginDescriptior;
import jetbrains.buildServer.serverSide.impl.BuildServerImpl;
import jetbrains.buildServer.serverSide.impl.FileWatcherFactory;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.util.FileUtil;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static jetbrains.buildServer.termsOfService.TermsOfServiceManagerImpl.TEAMCITY_TERMS_OF_SERVICE_ENABLED_PROPERTY;

public class TermsOfServiceControllerTest extends BaseControllerTestCase {

    private File myAgreementFile;
    private TermsOfServiceManagerImpl termsOfServiceManager;
    private File configFile;
    private TermsOfServiceConfig config;
    private TermsOfServicesLink link;

    @Override
    protected void configurePlugins(BuildServerImpl server) {
        setInternalProperty(TEAMCITY_TERMS_OF_SERVICE_ENABLED_PROPERTY, "true");
        File termsOfServiceDir = new File(myFixture.getServerPaths().getConfigDir(), "termsOfService");
        configFile = new File(termsOfServiceDir, "terms-of-service-config.xml");
        FileUtil.createIfDoesntExist(configFile);
        FileUtil.writeFile(configFile, "<terms-of-service-config>\n" +
                "<rule type=\"ALL_USERS\">\n" +
                "<parameters>\n" +
                "<param name=\"agreement-file\" value=\"agreement.html\"/>\n" +
                "</parameters>\n" +
                "</rule>\n" +
                "</terms-of-service-config>");
        myAgreementFile = new File(termsOfServiceDir, "agreement.html");
        FileUtil.createIfDoesntExist(myAgreementFile);
        FileUtil.writeFile(myAgreementFile, "Agreement");
        config = new TermsOfServiceConfig(myFixture.getEventDispatcher(), myFixture.getServerPaths(), myFixture.getSingletonService(FileWatcherFactory.class));
        termsOfServiceManager = new TermsOfServiceManagerImpl(config);
    }

    @Override
    protected BaseController createController() {
        link = new TermsOfServicesLink(termsOfServiceManager, getWebFixture().getPagePlaces(), new MockServerPluginDescriptior());
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

    @Test
    public void should_support_links_to_external_agreements() throws Exception {
        FileUtil.writeFile(configFile, "<terms-of-service-config>\n" +
                "<rule type=\"ALL_USERS\">\n" +
                "<parameters>\n" +
                "<param name=\"agreement-link\" value=\"http://jetbrains.com/agreement.html\"/>\n" +
                "</parameters>\n" +
                "</rule>\n" +
                "</terms-of-service-config>");
        config.loadSettings();

        makeLoggedIn(createUser("user"));

        ModelAndView modelAndView = doGet();
        Assert.assertEquals(modelAndView.getView().getClass(), RedirectView.class);

        Map<String, Object> model = new HashMap<>();
        link.fillModel(model, myRequest);
        Assert.assertEquals(model.get("agreementLink"), "http://jetbrains.com/agreement.html");
    }
}
