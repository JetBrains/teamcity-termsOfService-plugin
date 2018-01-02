package jetbrains.buildServer.termsOfService;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.controllers.ActionMessages;
import jetbrains.buildServer.serverSide.BuildServerListener;
import jetbrains.buildServer.serverSide.ServerPaths;
import jetbrains.buildServer.serverSide.ServerSideEventDispatcher;
import jetbrains.buildServer.serverSide.auth.AuthorityHolder;
import jetbrains.buildServer.serverSide.impl.CriticalErrorsImpl;
import jetbrains.buildServer.serverSide.impl.FileWatcherFactory;
import jetbrains.buildServer.serverSide.impl.auth.SecurityContextImpl;
import jetbrains.buildServer.users.PropertyKey;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.users.SimplePropertyKey;
import jetbrains.buildServer.users.UserModelEx;
import jetbrains.buildServer.users.impl.UserEx;
import jetbrains.buildServer.util.EventDispatcher;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.TimeService;
import jetbrains.buildServer.web.openapi.PagePlace;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import jetbrains.buildServer.web.util.SessionUser;
import org.jetbrains.annotations.NotNull;
import org.mockito.AdditionalMatchers;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.servlet.ModelAndView;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

public class TermsOfServiceTest extends BaseTestCase {

    private File myAgreementFile;
    private TermsOfServiceManagerImpl termsOfServiceManager;
    private File configFile;
    private TermsOfServiceConfig config;

    private TermsOfServicesLink link;
    private TermsOfServiceGuestNote guestNote;
    private TermsOfServiceHandlerInterceptor interceptor;
    private ViewTermsOfServiceController viewAgreementController;
    private AcceptTermsOfServiceController acceptAgreementController;
    private TimeService timeService;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockHttpSession session;
    private SecurityContextImpl securityContext;
    private UserModelEx userModel;
    private List<SUser> users;

    @Override
    @BeforeMethod
    public void setUp() throws Exception {
        super.setUp();
        session = new MockHttpSession();
        setInternalProperty("teamcity.http.auth.treat.all.clients.as.browsers","true" );
        securityContext = new SecurityContextImpl();
        EventDispatcher<BuildServerListener> events = ServerSideEventDispatcher.create(securityContext, BuildServerListener.class);

        timeService = Mockito.mock(TimeService.class);
        when(timeService.now()).thenReturn(System.currentTimeMillis());

        users = new CopyOnWriteArrayList<>();
        userModel = Mockito.mock(UserModelEx.class);
        UserEx guest = createUser("guest", -1000);
        when(userModel.getGuestUser()).thenReturn(guest);
        when(userModel.isGuestUser(ArgumentMatchers.eq(guest))).thenReturn(true);
        when(userModel.isGuestUser(AdditionalMatchers.not(ArgumentMatchers.eq(guest)))).thenReturn(false);

        PluginDescriptor pluginDescriptor = Mockito.mock(PluginDescriptor.class);
        PagePlaces pagePlaces = Mockito.mock(PagePlaces.class);
        when(pagePlaces.getPlaceById(any())).thenReturn(Mockito.mock(PagePlace.class));

        ServerPaths serverPaths = new ServerPaths(createTempDir());
        FileWatcherFactory fileWatcherFactory = new FileWatcherFactory(serverPaths, new CriticalErrorsImpl(serverPaths), events);
        File termsOfServiceDir = new File(serverPaths.getConfigDir(), "termsOfService");
        configFile = new File(termsOfServiceDir, "settings.xml");
        myAgreementFile = new File(termsOfServiceDir, "agreement.html");
        FileUtil.createIfDoesntExist(myAgreementFile);
        FileUtil.writeFileAndReportErrors(myAgreementFile, "Agreement");
        config = new TermsOfServiceConfig(events, serverPaths, fileWatcherFactory);
        WebControllerManager webControllerManager = Mockito.mock(WebControllerManager.class);
        termsOfServiceManager = new TermsOfServiceManagerImpl(config, userModel, timeService);
        link = new TermsOfServicesLink(termsOfServiceManager, pagePlaces, pluginDescriptor);
        guestNote = new TermsOfServiceGuestNote(termsOfServiceManager, pagePlaces, pluginDescriptor, userModel);
        interceptor = new TermsOfServiceHandlerInterceptor(termsOfServiceManager);
        acceptAgreementController = new AcceptTermsOfServiceController(webControllerManager, pluginDescriptor, termsOfServiceManager);
        viewAgreementController = new ViewTermsOfServiceController(webControllerManager, pluginDescriptor, termsOfServiceManager);
        events.getMulticaster().serverStartup();
    }

    @Test
    public void missing_settings_file() throws Exception {
        assertFalse(configFile.exists());

        login(createUser("user1"));

        assertAgreementNotShown("hosted_teamcity");
    }

    @Test
    public void missing_agreement_file() throws Exception {
        writeConfig("<terms-of-service>\n" +
                "    <agreement id=\"hosted_teamcity\">\n" +
                "        <parameters>\n" +
                "          \t<param name=\"content-file\" value=\"not_extisting.html\"/>\n" +
                "          \t<param name=\"version\" value=\"2017.1\"/>\n" +
                "            <param name=\"short-name\" value=\"Terms of Service\"/>\n" +
                "            <param name=\"full-name\" value=\"Terms of Service for Hosted TeamCity (teamcity.jetbrains.com)\"/>\n" +
                "        </parameters>\n" +
                "    </agreement>\n" +
                "</terms-of-service>");

        login(createUser("user1"));

        assertAgreementNotShown("hosted_teamcity");
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
        login(user);

        newRequest(GET, "/overview.html");
        then(interceptor.preHandle(request, response)).isFalse();
        then(response.getRedirectedUrl()).isEqualTo("/acceptTermsOfServices.html?agreement=hosted_teamcity");

        newRequest(GET, "/acceptTermsOfServices.html?agreement=hosted_teamcity");
        ModelAndView modelAndView = acceptAgreementController.doHandle(request, response);
        then(modelAndView.getModel().get("agreementText")).isEqualTo(FileUtil.readText(myAgreementFile, "UTF-8"));
        then(modelAndView.getModel().get("termsOfServiceName")).isEqualTo("Terms of Service for Hosted TeamCity (teamcity.jetbrains.com)");

        newRequest(POST, "/acceptTermsOfServices.html");
        request.addParameter("agreement", "hosted_teamcity");
        acceptAgreementController.doHandle(request, response);
        then(termsOfServiceManager.getMustAcceptAgreements(user)).hasSize(0);
        then(termsOfServiceManager.getAgreements()).extracting(a -> a.isAccepted(user)).containsOnly(true);
        then(user.getPropertyValue(new SimplePropertyKey("teamcity.policy.hosted_teamcity.acceptedVersion"))).isEqualTo("2017.1");
        then(user.getPropertyValue(new SimplePropertyKey("teamcity.policy.hosted_teamcity.acceptedFromIP"))).isEqualTo(request.getRemoteAddr());
        then(user.getPropertyValue(new SimplePropertyKey("teamcity.policy.hosted_teamcity.acceptedDate"))).isEqualTo(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(timeService.now()));

        then(interceptor.preHandle(request, response)).isTrue();
        newRequest(GET, "/acceptTermsOfServices.html?agreement=hosted_teamcity");
        modelAndView = viewAgreementController.doHandle(request, response);
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
        login(user);

        newRequest(POST, "/acceptTermsOfServices.html");
        request.addParameter("agreement", "hosted_teamcity");
        acceptAgreementController.doHandle(request, response);
        then(interceptor.preHandle(request, response)).isTrue();

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

        newRequest(GET, "/overview.html");
        then(interceptor.preHandle(request, response)).isFalse(); //new version was not accepted

        newRequest(POST, "/acceptTermsOfServices.html");
        request.addParameter("agreement", "hosted_teamcity");
        acceptAgreementController.doHandle(request, response);

        then(interceptor.preHandle(request, response)).isTrue();
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
        login(user);

        newRequest(GET, "/overview.html");
        then(interceptor.preHandle(request, response)).isFalse();
        then(response.getRedirectedUrl()).isEqualTo("/acceptTermsOfServices.html?agreement=hosted_teamcity");

        newRequest(GET, "/acceptTermsOfServices.html?agreement=hosted_teamcity");
        request.addParameter("agreement", "hosted_teamcity");
        ModelAndView modelAndView = acceptAgreementController.doHandle(request, response);
        then(((List<TermsOfServiceManager.Consent>) modelAndView.getModel().get("consents"))).extracting(TermsOfServiceManager.Consent::getId).contains("analytics", "marketing");

        newRequest(POST, "/acceptTermsOfServices.html");
        request.addParameter("agreement", "hosted_teamcity");
        request.addParameter("analytics", "true");
        acceptAgreementController.doHandle(request, response);
        then(termsOfServiceManager.getMustAcceptAgreements(user)).hasSize(0);
        then(user.getPropertyValue(new SimplePropertyKey("teamcity.policy.hosted_teamcity.acceptedVersion"))).isEqualTo("2017.1");
        then(user.getPropertyValue(new SimplePropertyKey("teamcity.policy.hosted_teamcity.acceptedFromIP"))).isEqualTo( request.getRemoteAddr());
        then(user.getPropertyValue(new SimplePropertyKey("teamcity.policy.hosted_teamcity.acceptedDate"))).isEqualTo( new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(timeService.now()));
        then(user.getPropertyValue(new SimplePropertyKey("teamcity.policy.hosted_teamcity.consent.analytics.accepted"))).isEqualTo( "true");
        then(user.getPropertyValue(new SimplePropertyKey("teamcity.policy.hosted_teamcity.consent.analytics.acceptedDate"))).isEqualTo( new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(timeService.now()));
        then(user.getPropertyValue(new SimplePropertyKey("teamcity.policy.hosted_teamcity.consent.analytics.acceptedFromIP"))).isEqualTo( request.getRemoteAddr());
        then(user.getPropertyValue(new SimplePropertyKey("teamcity.policy.hosted_teamcity.consent.marketing.accepted"))).isNull();
        then(user.getPropertyValue(new SimplePropertyKey("teamcity.policy.hosted_teamcity.consent.marketing.acceptedDate"))).isNull();
        then(user.getPropertyValue(new SimplePropertyKey("teamcity.policy.hosted_teamcity.consent.marketing.acceptedFromIP"))).isNull();
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
                        "    <external-agreement-link text=\"Terms of Service\" url=\"https://www.jetbrains.com/company/privacy.html\"/>\n" +
                    "</terms-of-service>");

        login(createUser("user"));

        newRequest(GET, "/overview.html");
        then(interceptor.preHandle(request, response)).isFalse();
        then(response.getRedirectedUrl()).isEqualTo("/acceptTermsOfServices.html?agreement=hosted_teamcity");

        newRequest(GET, "/acceptTermsOfServices.html?agreement=hosted_teamcity");
        ModelAndView modelAndView = acceptAgreementController.doHandle(request, response);
        then(modelAndView.getModel().get("agreementText")).isEqualTo(FileUtil.readText(myAgreementFile, "UTF-8"));
        then(modelAndView.getModel().get("termsOfServiceName")).isEqualTo("Terms of Service for Hosted TeamCity (teamcity.jetbrains.com)");

        newRequest(POST, "/acceptTermsOfServices.html");
        request.addParameter("agreement", "hosted_teamcity");
        acceptAgreementController.doHandle(request, response);

        then(interceptor.preHandle(request, response)).isTrue();

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
        FileUtil.writeFileAndReportErrors(guestNoticeFile, "Guest Notice");

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


        login(userModel.getGuestUser());

        newRequest(GET, "/overview.html");
        then(interceptor.preHandle(request, response)).isTrue();

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
        link.fillModel(model, request);
        return model;
    }

    @NotNull
    private Map<String, Object> guestNoteExtension() {
        Map<String, Object> model = new HashMap<>();
        guestNote.fillModel(model, request);
        return model;
    }

    @NotNull
    private UserEx createUser(@NotNull String username) {
        return createUser(username, users.size() + 1L);
    }

    @NotNull
    private UserEx createUser(@NotNull String username, long id) {
        Map<String, String> properties = new HashMap<>();
        UserEx user = Mockito.mock(UserEx.class);
        when(user.getId()).thenReturn(id);
        when(user.getUsername()).thenReturn(username);
        when(user.describe(anyBoolean())).thenReturn(username);
        when(user.getPropertyValue(any(PropertyKey.class))).thenAnswer((Answer<String>) invocation -> properties.get(invocation.<PropertyKey>getArgument(0).getKey()));
        Mockito.doAnswer((Answer<Void>) invocationOnMock -> {
            properties.put(invocationOnMock.<PropertyKey>getArgument(0).getKey(), invocationOnMock.getArgument(1));
            return null;
        }).when(user).setUserProperty(any(PropertyKey.class), any(String.class));
        users.add(user);
        return user;
    }

    private void newRequest(HttpMethod method, String url) {
        request = MockMvcRequestBuilders.request(method, url).session(session).buildRequest(new MockServletContext());
        response = new MockHttpServletResponse();
        ActionMessages messages = ActionMessages.getMessages(request);
        if (messages != null) messages.clearMessages();
        AuthorityHolder loggedInUser = securityContext.getAuthorityHolder();
        if (loggedInUser instanceof SUser && SessionUser.getUser(request) != loggedInUser) {
            SessionUser.setUser(request, (SUser) loggedInUser);
        }
    }

    private void login(SUser user) {
        securityContext.setAuthorityHolder(user);
    }

    private void assertAgreementNotShown(@NotNull String agreementId) throws Exception {
        newRequest(GET, "/overview.html");
        then(interceptor.preHandle(request, response)).isTrue();

        newRequest(GET, "/acceptTermsOfServices.html?agreement=" + agreementId);
        acceptAgreementController.doHandle(request, response);
        then(response.getStatus()).isEqualTo(404);

        newRequest(GET, "/viewTermsOfServices.html?agreement=" + agreementId);
        viewAgreementController.doHandle(request, response);
        then(response.getStatus()).isEqualTo(404);

        then((List<TermsOfServiceManager.Agreement>) linksExtension().get("agreements")).isEmpty();
    }

}
