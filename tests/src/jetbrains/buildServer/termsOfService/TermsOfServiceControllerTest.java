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
import java.util.List;
import java.util.Map;

import static jetbrains.buildServer.asserts.WebAsserts.then;
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
        FileUtil.writeFile(configFile,
                "<terms-of-service-config>\n" +
                        "    <agreement id=\"hosted_teamcity\">\n" +
                        "      <rule type=\"ALL_USERS\">\n" +
                        "        <parameters>\n" +
                        "          \t<param name=\"agreement-file\" value=\"agreement.html\"/>\n" +
                        "            <param name=\"short-name\" value=\"Terms of Service\"/>\n" +
                        "            <param name=\"full-name\" value=\"Terms of Service for Hosted TeamCity (teamcity.jetbrains.com)\"/>\n" +
                        "        </parameters>\n" +
                        "      </rule>\n" +
                        "    </agreement>\n" +
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

        ModelAndView modelAndView = doGet("agreement", "hosted_teamcity");

        Assert.assertNotNull(modelAndView);

        then(modelAndView).hasViewName(TermsOfServiceController.ACCEPT_TERMS_OF_SERVICE_JSP);
        then(modelAndView.getModel().get("agreementText")).isEqualTo(FileUtil.readText(myAgreementFile, "UTF-8"));
        then(modelAndView.getModel().get("termsOfServiceName")).isEqualTo("Terms of Service for Hosted TeamCity (teamcity.jetbrains.com)");
    }

    @Test
    public void test_terms_flow() throws Exception {
        SUser user1 = createUser("user1");
        SUser user2 = createUser("user2");

        makeLoggedIn(user1);
        then(termsOfServiceManager.mustAccept(user1)).isTrue();

        ModelAndView modelAndView = doGet("agreement", "hosted_teamcity");
        then(modelAndView).hasViewName(TermsOfServiceController.ACCEPT_TERMS_OF_SERVICE_JSP);

        doPost("agreement", "hosted_teamcity");
        then(termsOfServiceManager.mustAccept(user1)).isFalse();
        then(termsOfServiceManager.getAgreementsFor(user1)).allMatch(a -> a.isAccepted(user1));

        modelAndView = doGet("agreement", "hosted_teamcity");
        then(modelAndView).hasViewName(TermsOfServiceController.TERMS_OF_SERVICE_JSP);

        makeLoggedIn(user2);
        modelAndView = doGet("agreement", "hosted_teamcity");
        then(modelAndView).hasViewName(TermsOfServiceController.ACCEPT_TERMS_OF_SERVICE_JSP);
    }

    @Test
    public void should_support_links_to_external_agreements() throws Exception {
        FileUtil.writeFile(configFile,
                "<terms-of-service-config>\n" +
                        "    <agreement id=\"hosted_teamcity\">\n" +
                        "      <rule type=\"ALL_USERS\">\n" +
                        "        <parameters>\n" +
                        "          \t<param name=\"agreement-link\" value=\"http://jetbrains.com/agreement.html\"/>\n" +
                        "            <param name=\"short-name\" value=\"Terms of Service\"/>\n" +
                        "            <param name=\"full-name\" value=\"Terms of Service for Hosted TeamCity (teamcity.jetbrains.com)\"/>\n" +
                        "        </parameters>\n" +
                        "      </rule>\n" +
                        "    </agreement>\n" +
                        "</terms-of-service-config>");

        config.loadSettings();

        makeLoggedIn(createUser("user"));

        ModelAndView modelAndView = doGet("agreement", "hosted_teamcity");
        Assert.assertEquals(modelAndView.getView().getClass(), RedirectView.class);

        Map<String, Object> model = new HashMap<>();
        link.fillModel(model, myRequest);
        then((List<TermsOfServiceManager.Agreement>) model.get("agreements"))
                .extracting("link")
                .containsOnly("http://jetbrains.com/agreement.html");
    }

    @Test
    public void should_support_several_agreements() {
        FileUtil.writeFile(configFile,
                        "<terms-of-service-config>\n" +
                        "    <agreement id=\"hosted_teamcity\">\n" +
                        "      <rule type=\"ALL_USERS\">\n" +
                        "        <parameters>\n" +
                        "          \t<param name=\"agreement-file\" value=\"agreement.html\"/>\n" +
                        "            <param name=\"short-name\" value=\"Terms of Service\"/>\n" +
                        "            <param name=\"full-name\" value=\"Terms of Service for Hosted TeamCity (teamcity.jetbrains.com)\"/>\n" +
                        "        </parameters>\n" +
                        "      </rule>\n" +
                        "    </agreement>\n" +
                        "    <agreement id=\"privacy_policy\">\n" +
                        "      <rule type=\"ALL_USERS\">\n" +
                        "        <parameters>\n" +
                        "           \t<param name=\"agreement-link\" value=\"https://www.jetbrains.com/company/privacy.html\"/>\n" +
                        "            <param name=\"short-name\" value=\"Privacy Policy\"/>\n" +
                        "        </parameters>\n" +
                        "      </rule>\n" +
                        "    </agreement>\n" +
                        "</terms-of-service-config>");
        config.loadSettings();

        makeLoggedIn(createUser("user"));

        Map<String, Object> model = new HashMap<>();
        link.fillModel(model, myRequest);

        then((List<TermsOfServiceManager.Agreement>) model.get("agreements"))
                .hasSize(2)
                .extracting("link")
                .contains("https://www.jetbrains.com/company/privacy.html", "/termsOfServices.html?agreement=hosted_teamcity");
    }
}
