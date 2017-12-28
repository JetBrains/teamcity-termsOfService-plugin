package jetbrains.buildServer.termsOfService;

import jetbrains.buildServer.BaseWebTestCase;
import jetbrains.buildServer.MockTimeService;
import jetbrains.buildServer.serverSide.MockServerPluginDescriptior;
import jetbrains.buildServer.serverSide.impl.BuildServerImpl;
import jetbrains.buildServer.serverSide.impl.FileWatcherFactory;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.util.ConsoleLogger;
import jetbrains.buildServer.util.FileUtil;
import org.apache.log4j.Level;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.servlet.ModelAndView;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.text.SimpleDateFormat;
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
    private MockTimeService timeService;

    @Override
    @BeforeMethod
    public void setUp() throws Exception {
        super.setUp();

        link = new TermsOfServicesLink(termsOfServiceManager, getWebFixture().getPagePlaces(), new MockServerPluginDescriptior());
        guestNote = new TermsOfServiceGuestNote(termsOfServiceManager, getWebFixture().getPagePlaces(), new MockServerPluginDescriptior(), myFixture.getUserModel());
        interceptor = new TermsOfServiceHandlerInterceptor(termsOfServiceManager);
        acceptAgreementController = new AcceptTermsOfServiceController(myWebManager, new MockServerPluginDescriptior(), termsOfServiceManager);
        viewAgreementController = new ViewTermsOfServiceController(myWebManager, new MockServerPluginDescriptior(), termsOfServiceManager);
        myRequest.setRemoteAddr("182.22.12.12");
    }

    @Override
    protected void configurePlugins(BuildServerImpl server) {
        setInternalProperty("teamcity.http.auth.treat.all.clients.as.browsers","true" );
        TermsOfServiceLogger.LOGGER = new ConsoleLogger("TOS ", Level.DEBUG);
        setInternalProperty(TEAMCITY_TERMS_OF_SERVICE_ENABLED_PROPERTY, "true");
        File termsOfServiceDir = new File(myFixture.getServerPaths().getConfigDir(), "termsOfService");
        configFile = new File(termsOfServiceDir, "settings.xml");
        myAgreementFile = new File(termsOfServiceDir, "agreement.html");
        FileUtil.createIfDoesntExist(myAgreementFile);
        FileUtil.writeFile(myAgreementFile, "Agreement");
        config = new TermsOfServiceConfig(myFixture.getEventDispatcher(), myFixture.getServerPaths(), myFixture.getSingletonService(FileWatcherFactory.class));
        timeService = new MockTimeService();
        termsOfServiceManager = new TermsOfServiceManagerImpl(config, myFixture.getUserModel(), timeService);
    }

    @Test
    public void test_terms_of_service_flow_for_single_agreement() throws Exception {
        writeConfig("<terms-of-service>\n" +
                "    <agreement id=\"hosted_teamcity\">\n" +
                "        <parameters>\n" +
                "          \t<param name=\"content-file\" value=\"agreement.html\"/>\n" +
                "          \t<param name=\"version\" value=\"2017.1\"/>\n" +
                "            <param name=\"short-name\" value=\"Terms of Service\"/>\n" +
                "            <param name=\"full-name\" value=\"Terms of Service for Hosted TeamCity (teamcity.jetbrains.com)\"/>\n" +
                "        </parameters>\n" +
                "    </agreement>\n" +
                "</terms-of-service>");

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
        then(user).hasProperty("teamcity.policy.hosted_teamcity.acceptedVersion", "2017.1");
        then(user).hasProperty("teamcity.policy.hosted_teamcity.acceptedFromIP", myRequest.getRemoteAddr());
        then(user).hasProperty("teamcity.policy.hosted_teamcity.acceptedDate", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(timeService.getNow()));

        then(interceptor.preHandle(myRequest, myResponse)).isTrue();
        myRequest.addParameters("agreement", "hosted_teamcity");
        myRequest.setMethod("GET");
        modelAndView = viewAgreementController.doHandle(myRequest, myResponse);
        then(modelAndView.getModel().get("agreementText")).isEqualTo(FileUtil.readText(myAgreementFile, "UTF-8"));
        then(modelAndView.getModel().get("termsOfServiceName")).isEqualTo("Terms of Service for Hosted TeamCity (teamcity.jetbrains.com)");
    }

    @Test
    public void should_support_version_changes() throws Exception {
        writeConfig("<terms-of-service>\n" +
                "    <agreement id=\"hosted_teamcity\">\n" +
                "        <parameters>\n" +
                "          \t<param name=\"content-file\" value=\"agreement.html\"/>\n" +
                "          \t<param name=\"version\" value=\"2017.1\"/>\n" +
                "            <param name=\"short-name\" value=\"Terms of Service\"/>\n" +
                "            <param name=\"full-name\" value=\"Terms of Service for Hosted TeamCity (teamcity.jetbrains.com)\"/>\n" +
                "        </parameters>\n" +
                "    </agreement>\n" +
                "</terms-of-service>");

        SUser user = createUser("user1");
        makeLoggedIn(user);

        myRequest.addParameters("agreement", "hosted_teamcity");
        myRequest.setMethod("POST");
        acceptAgreementController.doHandle(myRequest, myResponse);
        then(interceptor.preHandle(myRequest, myResponse)).isTrue();


        writeConfig("<terms-of-service>\n" +
                "    <agreement id=\"hosted_teamcity\">\n" +
                "        <parameters>\n" +
                "          \t<param name=\"content-file\" value=\"agreement.html\"/>\n" +
                "          \t<param name=\"version\" value=\"2017.2\"/>\n" +
                "            <param name=\"short-name\" value=\"Terms of Service\"/>\n" +
                "            <param name=\"full-name\" value=\"Terms of Service for Hosted TeamCity (teamcity.jetbrains.com)\"/>\n" +
                "        </parameters>\n" +
                "    </agreement>\n" +
                "</terms-of-service>");
        myRequest.addParameters("agreement", "hosted_teamcity");
        myRequest.setMethod("GET");

        then(interceptor.preHandle(myRequest, myResponse)).isFalse(); //new version was not accepted

        myRequest.addParameters("agreement", "hosted_teamcity");
        myRequest.setMethod("POST");
        acceptAgreementController.doHandle(myRequest, myResponse);

        then(interceptor.preHandle(myRequest, myResponse)).isTrue();
    }

    @Test
    public void should_support_configurable_list_of_consents() throws Exception {
        writeConfig("<terms-of-service>\n" +
                "    <agreement id=\"hosted_teamcity\">\n" +
                "        <parameters>\n" +
                "          \t<param name=\"content-file\" value=\"agreement.html\"/>\n" +
                "          \t<param name=\"version\" value=\"2017.1\"/>\n" +
                "            <param name=\"short-name\" value=\"Terms of Service\"/>\n" +
                "            <param name=\"full-name\" value=\"Terms of Service for Hosted TeamCity (teamcity.jetbrains.com)\"/>\n" +
                "        </parameters>\n" +
                "        <consents>\n" +
                "          \t<consent id=\"analytics\" text=\"Allow analytics\" default=\"true\"/>\n" +
                "          \t<consent id=\"marketing\" text=\"Allow marketing\" default=\"false\"/>\n" +
                "        </consents>\n" +
                "    </agreement>\n" +
                "</terms-of-service>");

        SUser user = createUser("user1");
        makeLoggedIn(user);

        then(interceptor.preHandle(myRequest, myResponse)).isFalse();
        then(myResponse).isRedirectTo(myRequest.getContextPath() + "/acceptTermsOfServices.html?agreement=hosted_teamcity");

        myRequest.addParameters("agreement", "hosted_teamcity");
        myRequest.setMethod("GET");
        ModelAndView modelAndView = acceptAgreementController.doHandle(myRequest, myResponse);
        then(((List<TermsOfServiceManager.Consent>) modelAndView.getModel().get("consents"))).extracting(TermsOfServiceManager.Consent::getId).contains("analytics", "marketing");


        myRequest.addParameters("agreement", "hosted_teamcity", "analytics", "true");
        myRequest.setMethod("POST");
        acceptAgreementController.doHandle(myRequest, myResponse);
        then(termsOfServiceManager.getMustAcceptAgreements(user)).hasSize(0);
        then(user).hasProperty("teamcity.policy.hosted_teamcity.acceptedVersion", "2017.1");
        then(user).hasProperty("teamcity.policy.hosted_teamcity.acceptedFromIP", myRequest.getRemoteAddr());
        then(user).hasProperty("teamcity.policy.hosted_teamcity.acceptedDate", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(timeService.getNow()));
        then(user).hasProperty("teamcity.policy.hosted_teamcity.consent.analytics.accepted", "true");
        then(user).hasProperty("teamcity.policy.hosted_teamcity.consent.analytics.acceptedDate", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(timeService.getNow()));
        then(user).hasProperty("teamcity.policy.hosted_teamcity.consent.analytics.acceptedFromIP", myRequest.getRemoteAddr());
        then(user).doesNotHaveProperty("teamcity.policy.hosted_teamcity.consent.marketing.accepted");
        then(user).doesNotHaveProperty("teamcity.policy.hosted_teamcity.consent.marketing.acceptedDate");
        then(user).doesNotHaveProperty("teamcity.policy.hosted_teamcity.consent.marketing.acceptedFromIP");
    }

    @Test
    public void should_support_several_agreements_one_of_which_user_must_accept() throws Exception {
        writeConfig("<terms-of-service>\n" +
                        "    <agreement id=\"hosted_teamcity\">\n" +
                        "        <parameters>\n" +
                        "          \t<param name=\"content-file\" value=\"agreement.html\"/>\n" +
                        "            <param name=\"short-name\" value=\"Terms of Service\"/>\n" +
                        "            <param name=\"full-name\" value=\"Terms of Service for Hosted TeamCity (teamcity.jetbrains.com)\"/>\n" +
                        "        </parameters>\n" +
                        "    </agreement>\n" +
                        "    <external-agreement-link name=\"Terms of Service\" url=\"https://www.jetbrains.com/company/privacy.html\"/>\n" +
                    "</terms-of-service>");

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
                .hasSize(1)
                .extracting("link")
                .contains("/viewTermsOfServices.html?agreement=hosted_teamcity");
        then((List<TermsOfServiceManager.Agreement>) linksExtension().get("externalAgreements"))
                .hasSize(1)
                .extracting("url")
                .contains("https://www.jetbrains.com/company/privacy.html");
    }


    @Test
    public void should_support_guest_user_notice() throws Exception {
        File guestNoticeFile = new File(myAgreementFile.getParent(), "guestNotice.html");
        FileUtil.createIfDoesntExist(guestNoticeFile);
        FileUtil.writeFile(guestNoticeFile, "Guest Notice");

        writeConfig("<terms-of-service>\n" +
                "    <agreement id=\"privacy_policy\">\n" +
                "        <parameters>\n" +
                "          \t<param name=\"content-file\" value=\"agreement.html\"/>\n" +
                "            <param name=\"short-name\" value=\"Terms of Service\"/>\n" +
                "            <param name=\"full-name\" value=\"Terms of Service for Hosted TeamCity (teamcity.jetbrains.com)\"/>\n" +
                "        </parameters>\n" +
                "    </agreement>\n" +
                "    <guest-notice>\n" +
                "        <parameters>\n" +
                "           \t<param name=\"content-file\" value=\"guestNotice.html\"/>\n" +
                "            <param name=\"text\" value=\"A privacy reminder from JetBrains\"/>\n" +
                "        </parameters>\n" +
                "    </guest-notice>\n" +
                "</terms-of-service>");


        makeLoggedIn(getUserModelEx().getGuestUser());

        then(interceptor.preHandle(myRequest, myResponse)).isTrue();

        then((List<TermsOfServiceManager.Agreement>) linksExtension().get("agreements"))
                .extracting("link")
                .containsOnly("/viewTermsOfServices.html?agreement=privacy_policy");

        TermsOfServiceManager.GuestNotice guestNotice = (TermsOfServiceManager.GuestNotice) guestNoteExtension().get("guestNotice");
        then(guestNotice.getText()).isEqualTo("A privacy reminder from JetBrains");
        then(guestNotice.getHtml()).isEqualTo("Guest Notice");
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
