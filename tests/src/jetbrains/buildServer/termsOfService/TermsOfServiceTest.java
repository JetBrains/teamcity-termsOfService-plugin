package jetbrains.buildServer.termsOfService;

import jetbrains.buildServer.BaseWebTestCase;
import jetbrains.buildServer.serverSide.MockServerPluginDescriptior;
import jetbrains.buildServer.serverSide.impl.BuildServerImpl;
import jetbrains.buildServer.serverSide.impl.FileWatcherFactory;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.util.ConsoleLogger;
import jetbrains.buildServer.util.FileUtil;
import org.apache.log4j.Level;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static jetbrains.buildServer.asserts.WebAsserts.then;
import static jetbrains.buildServer.termsOfService.TermsOfServiceManagerImpl.TEAMCITY_TERMS_OF_SERVICE_ENABLED_PROPERTY;

public class TermsOfServiceTest extends BaseWebTestCase {

    private File myAgreementFile;
    private TermsOfServiceManagerImpl termsOfServiceManager;
    private File configFile;
    private TermsOfServiceConfig config;

    private TermsOfServicesLink link;
    private TermsOfServiceGuestNote guestNote;
    private TermsOfServiceHandlerInterceptor interceptor;
    private ViewTermsOfServiceController viewAgreementController;
    private AcceptTermsOfServiceController acceptAgreementController;

    @Override
    @BeforeMethod
    public void setUp() throws Exception {
        super.setUp();

        link = new TermsOfServicesLink(termsOfServiceManager, getWebFixture().getPagePlaces(), new MockServerPluginDescriptior());
        guestNote = new TermsOfServiceGuestNote(termsOfServiceManager, getWebFixture().getPagePlaces(), new MockServerPluginDescriptior(), myFixture.getUserModel());
        interceptor = new TermsOfServiceHandlerInterceptor(termsOfServiceManager);
        acceptAgreementController = new AcceptTermsOfServiceController(myWebManager, new MockServerPluginDescriptior(), termsOfServiceManager);
        viewAgreementController = new ViewTermsOfServiceController(myWebManager, new MockServerPluginDescriptior(), termsOfServiceManager);
    }

    @Override
    protected void configurePlugins(BuildServerImpl server) {
        setInternalProperty("teamcity.http.auth.treat.all.clients.as.browsers","true" );
        TermsOfServiceLogger.LOGGER = new ConsoleLogger("TOS ", Level.DEBUG);
        setInternalProperty(TEAMCITY_TERMS_OF_SERVICE_ENABLED_PROPERTY, "true");
        File termsOfServiceDir = new File(myFixture.getServerPaths().getConfigDir(), "termsOfService");
        configFile = new File(termsOfServiceDir, "terms-of-service-config.xml");
        myAgreementFile = new File(termsOfServiceDir, "agreement.html");
        FileUtil.createIfDoesntExist(myAgreementFile);
        FileUtil.writeFile(myAgreementFile, "Agreement");
        config = new TermsOfServiceConfig(myFixture.getEventDispatcher(), myFixture.getServerPaths(), myFixture.getSingletonService(FileWatcherFactory.class));
        termsOfServiceManager = new TermsOfServiceManagerImpl(config, myFixture.getUserModel());
    }

    @Test
    public void test_terms_of_service_flow_for_single_agreement() throws Exception {
        writeConfig("<terms-of-service-config>\n" +
                "    <agreement id=\"hosted_teamcity\">\n" +
                "        <parameters>\n" +
                "          \t<param name=\"agreement-file\" value=\"agreement.html\"/>\n" +
                "            <param name=\"short-name\" value=\"Terms of Service\"/>\n" +
                "            <param name=\"full-name\" value=\"Terms of Service for Hosted TeamCity (teamcity.jetbrains.com)\"/>\n" +
                "        </parameters>\n" +
                "    </agreement>\n" +
                "</terms-of-service-config>");

        SUser user = createUser("user1");
        makeLoggedIn(user);

        then(interceptor.preHandle(myRequest, myResponse)).isFalse();
        then(myResponse).isRedirectTo(myRequest.getContextPath() + "/acceptTermsOfServices.html?agreement=hosted_teamcity");

        myRequest.addParameters("agreement", "hosted_teamcity");
        myRequest.setMethod("GET");
        ModelAndView modelAndView = acceptAgreementController.doHandle(myRequest, myResponse);
        then(modelAndView.getModel().get("agreementText")).isEqualTo(FileUtil.readText(myAgreementFile, "UTF-8"));
        then(modelAndView.getModel().get("termsOfServiceName")).isEqualTo("Terms of Service for Hosted TeamCity (teamcity.jetbrains.com)");

        myRequest.addParameters("agreement", "hosted_teamcity");
        myRequest.setMethod("POST");
        acceptAgreementController.doHandle(myRequest, myResponse);
        then(termsOfServiceManager.getMustAcceptAgreements(user)).hasSize(0);
        then(termsOfServiceManager.getAgreements()).allMatch(a -> a.isAccepted(user));

        then(interceptor.preHandle(myRequest, myResponse)).isTrue();
        myRequest.addParameters("agreement", "hosted_teamcity");
        myRequest.setMethod("GET");
        modelAndView = viewAgreementController.doHandle(myRequest, myResponse);
        then(modelAndView.getModel().get("agreementText")).isEqualTo(FileUtil.readText(myAgreementFile, "UTF-8"));
        then(modelAndView.getModel().get("termsOfServiceName")).isEqualTo("Terms of Service for Hosted TeamCity (teamcity.jetbrains.com)");
    }

    @Test
    public void should_support_links_to_external_agreements_which_user_must_not_accept_but_can_review() throws Exception {
        writeConfig("<terms-of-service-config>\n" +
                        "    <agreement id=\"hosted_teamcity\">\n" +
                        "        <parameters>\n" +
                        "          \t<param name=\"agreement-link\" value=\"http://jetbrains.com/agreement.html\"/>\n" +
                        "            <param name=\"force-accept\" value=\"false\"/>\n" +
                        "            <param name=\"short-name\" value=\"Terms of Service\"/>\n" +
                        "        </parameters>\n" +
                        "    </agreement>\n" +
                        "</terms-of-service-config>");
        makeLoggedIn(createUser("user"));

        then(interceptor.preHandle(myRequest, myResponse)).isFalse();

        myRequest.addParameters("agreement", "hosted_teamcity");
        myRequest.setMethod("GET");
        ModelAndView modelAndView = viewAgreementController.doHandle(myRequest, myResponse);
        Assert.assertEquals(modelAndView.getView().getClass(), RedirectView.class);

        then((List<TermsOfServiceManager.Agreement>) linksExtension().get("agreements"))
                .extracting("link")
                .containsOnly("http://jetbrains.com/agreement.html");
    }

    @Test
    public void should_support_several_agreements_one_of_which_user_must_accept() throws Exception {
        writeConfig("<terms-of-service-config>\n" +
                        "    <agreement id=\"hosted_teamcity\">\n" +
                        "        <parameters>\n" +
                        "          \t<param name=\"agreement-file\" value=\"agreement.html\"/>\n" +
                        "            <param name=\"short-name\" value=\"Terms of Service\"/>\n" +
                        "            <param name=\"full-name\" value=\"Terms of Service for Hosted TeamCity (teamcity.jetbrains.com)\"/>\n" +
                        "        </parameters>\n" +
                        "    </agreement>\n" +
                        "    <agreement id=\"privacy_policy\">\n" +
                        "        <parameters>\n" +
                        "           \t<param name=\"agreement-link\" value=\"https://www.jetbrains.com/company/privacy.html\"/>\n" +
                        "            <param name=\"force-accept\" value=\"false\"/>\n" +
                        "            <param name=\"short-name\" value=\"Privacy Policy\"/>\n" +
                        "        </parameters>\n" +
                        "    </agreement>\n" +
                        "</terms-of-service-config>");

        makeLoggedIn(createUser("user"));

        then(interceptor.preHandle(myRequest, myResponse)).isFalse();
        then(myResponse).isRedirectTo(myRequest.getContextPath() + "/acceptTermsOfServices.html?agreement=hosted_teamcity");

        myRequest.addParameters("agreement", "hosted_teamcity");
        myRequest.setMethod("GET");
        ModelAndView modelAndView = acceptAgreementController.doHandle(myRequest, myResponse);
        then(modelAndView.getModel().get("agreementText")).isEqualTo(FileUtil.readText(myAgreementFile, "UTF-8"));
        then(modelAndView.getModel().get("termsOfServiceName")).isEqualTo("Terms of Service for Hosted TeamCity (teamcity.jetbrains.com)");

        myRequest.addParameters("agreement", "hosted_teamcity");
        myRequest.setMethod("POST");
        acceptAgreementController.doHandle(myRequest, myResponse);

        then(interceptor.preHandle(myRequest, myResponse)).isTrue();

        then((List<TermsOfServiceManager.Agreement>) linksExtension().get("agreements"))
                .hasSize(2)
                .extracting("link")
                .contains("https://www.jetbrains.com/company/privacy.html", "/viewTermsOfServices.html?agreement=hosted_teamcity");
    }


    @Test
    public void should_support_guest_user_notice() throws Exception {
        writeConfig("<terms-of-service-config>\n" +
                "    <agreement id=\"privacy_policy\">\n" +
                "        <parameters>\n" +
                "          \t<param name=\"agreement-link\" value=\"https://www.jetbrains.com/company/privacy.html\"/>\n" +
                "            <param name=\"short-name\" value=\"Terms of Service\"/>\n" +
                "        </parameters>\n" +
                "    </agreement>\n" +
                "    <guest-notice>\n" +
                "        <parameters>\n" +
                "           \t<param name=\"agreement\" value=\"privacy_policy\"/>\n" +
                "            <param name=\"text\" value=\"A privacy reminder from JetBrains\"/>\n" +
                "        </parameters>\n" +
                "    </guest-notice>\n" +
                "</terms-of-service-config>");


        makeLoggedIn(getUserModelEx().getGuestUser());

        then(interceptor.preHandle(myRequest, myResponse)).isTrue();

        then((List<TermsOfServiceManager.Agreement>) linksExtension().get("agreements"))
                .extracting("link")
                .containsOnly("https://www.jetbrains.com/company/privacy.html");

        then(((TermsOfServiceManager.GuestNotice) guestNoteExtension().get("guestNotice")).getText())
                .isEqualTo("A privacy reminder from JetBrains");
    }

    private void writeConfig(String s) {
        FileUtil.createIfDoesntExist(configFile);
        FileUtil.writeFile(configFile, s);
        config.loadSettings();
    }

    @NotNull
    private Map<String, Object> linksExtension() {
        Map<String, Object> model = new HashMap<>();
        link.fillModel(model, myRequest);
        return model;
    }

    @NotNull
    private Map<String, Object> guestNoteExtension() {
        Map<String, Object> model = new HashMap<>();
        guestNote.fillModel(model, myRequest);
        return model;
    }
}