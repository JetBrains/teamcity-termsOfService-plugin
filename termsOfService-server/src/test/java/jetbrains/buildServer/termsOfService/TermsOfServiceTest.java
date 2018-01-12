package jetbrains.buildServer.termsOfService;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.controllers.ActionMessages;
import jetbrains.buildServer.controllers.XmlResponseUtil;
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
import jetbrains.buildServer.util.Dates;
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
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

@SuppressWarnings("unchecked")
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
    private TermsOfServiceUserProfileExtension userConsentsExtension;
    private long currentTime;
    private ServerPaths serverPaths;

    @Override
    @BeforeMethod
    public void setUp() throws Exception {
        super.setUp();
        session = new MockHttpSession();
        setInternalProperty("teamcity.http.auth.treat.all.clients.as.browsers", "true");
        securityContext = new SecurityContextImpl();
        EventDispatcher<BuildServerListener> events = ServerSideEventDispatcher.create(securityContext, BuildServerListener.class);

        timeService = Mockito.mock(TimeService.class);
        currentTime = System.currentTimeMillis();
        when(timeService.now()).thenAnswer(invocation -> currentTime);

        users = new CopyOnWriteArrayList<>();
        userModel = Mockito.mock(UserModelEx.class);
        UserEx guest = createUser("guest", -1000);
        when(userModel.getGuestUser()).thenReturn(guest);
        when(userModel.isGuestUser(ArgumentMatchers.eq(guest))).thenReturn(true);
        when(userModel.isGuestUser(AdditionalMatchers.not(ArgumentMatchers.eq(guest)))).thenReturn(false);

        PluginDescriptor pluginDescriptor = Mockito.mock(PluginDescriptor.class);
        PagePlaces pagePlaces = Mockito.mock(PagePlaces.class);
        when(pagePlaces.getPlaceById(any())).thenReturn(Mockito.mock(PagePlace.class));

        serverPaths = new ServerPaths(createTempDir());
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
        userConsentsExtension = new TermsOfServiceUserProfileExtension(termsOfServiceManager, pagePlaces, pluginDescriptor, webControllerManager);
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
    public void attempt_to_reference_content_file_out_of_termsOfService_configs_directory() throws Exception {
        File mainConfig = new File(serverPaths.getConfigDir(), "main-config.xml");
        FileUtil.writeFileAndReportErrors(mainConfig, "some config");

        writeConfig("<terms-of-service>\n" +
                "    <agreement id=\"hosted_teamcity\">\n" +
                "        <parameters>\n" +
                "          \t<param name=\"content-file\" value=\"../main-config.xml\"/>\n" +
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

        SUser user = login(createUser("user1"));
        assertOverviewPageRedirectsToAgreement("hosted_teamcity");

        Map<String, Object> model = GET_Accept_Agreement_Page("hosted_teamcity");
        then(((TermsOfServiceManager.Agreement) model.get("agreement")).getHtml()).isEqualTo(FileUtil.readText(myAgreementFile, "UTF-8"));
        then(((TermsOfServiceManager.Agreement) model.get("agreement")).getFullName()).isEqualTo("Terms of Service for Hosted TeamCity (teamcity.jetbrains.com)");

        POST_Accept_Agreement_Page("hosted_teamcity");
        then(termsOfServiceManager.getMustAcceptAgreements(user)).hasSize(0);
        then(termsOfServiceManager.getAgreements()).extracting(a -> a.isAccepted(user)).containsOnly(true);
        assertAgreementUserProperties("hosted_teamcity", "2017.1", request.getRemoteAddr(), timeService.now());

        assertOverviewPageAccessible();

        model = GET_View_Agreement_Page("hosted_teamcity");
        then(model.get("agreementText")).isEqualTo(FileUtil.readText(myAgreementFile, "UTF-8"));
        then(model.get("termsOfServiceName")).isEqualTo("Terms of Service for Hosted TeamCity (teamcity.jetbrains.com)");
    }

    /**
     * Is a user is using TeamCity while the agreement is configured then the agreement should not be shown to him,
     * but must be shown for any new sessions.
     */
    @Test
    public void should_not_show_just_configured_agreement_to_currently_active_users() throws Exception {
        login(createUser("user1"));
        assertOverviewPageAccessible();

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

        assertOverviewPageAccessible();

        relogin();

        assertOverviewPageRedirectsToAgreement("hosted_teamcity");
        assertOverviewPageRedirectsToAgreement("hosted_teamcity");//second request must also redirect to the agreement
    }

    /**
     * Is a user is using TeamCity while the agreement is configured then the agreement should not be shown to him,
     * but must be shown for any new sessions.
     */
    @Test
    public void should_support_enforcement_date() throws Exception {
        login(createUser("user1"));
        assertOverviewPageAccessible();

        String enforcementDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ").format(Dates.daysAfter(new Date(currentTime), 1));

        writeConfig("<terms-of-service>\n" +
                "    <agreement id=\"hosted_teamcity\">\n" +
                "        <parameters>\n" +
                "            <param name=\"content-file\" value=\"agreement.html\"/>\n" +
                "            <param name=\"version\" value=\"2017.1\"/>\n" +
                "            <param name=\"enforcement-date\" value=\"" + enforcementDate + "\"/>\n" +
                "            <param name=\"short-name\" value=\"Terms of Service\"/>\n" +
                "            <param name=\"full-name\" value=\"Terms of Service for Hosted TeamCity (teamcity.jetbrains.com)\"/>\n" +
                "        </parameters>\n" +
                "    </agreement>\n" +
                "</terms-of-service>");

        assertOverviewPageAccessible();

        currentTime = Dates.daysAfter(new Date(currentTime), 1).getTime() + 1;

        assertOverviewPageRedirectsToAgreement("hosted_teamcity");
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

        login(createUser("user1"));

        Map<String, Object> model = GET_Accept_Agreement_Page("hosted_teamcity");
        then(model.get("displayReason")).isEqualTo(AcceptTermsOfServiceController.DisplayReason.NOT_ACCEPTED);

        POST_Accept_Agreement_Page("hosted_teamcity");
        assertOverviewPageAccessible();

        writeConfig("<terms-of-service>\n" +
                "    <agreement id=\"hosted_teamcity\">\n" +
                "        <parameters>\n" +
                "           <param name=\"content-file\" value=\"agreement.html\"/>\n" +
                "           <param name=\"version\" value=\"2017.2\"/>\n" +
                "           <param name=\"last-updated\" value=\"08 January 2018\"/>\n" +
                "           <param name=\"short-name\" value=\"Terms of Service\"/>\n" +
                "           <param name=\"full-name\" value=\"Terms of Service for Hosted TeamCity (teamcity.jetbrains.com)\"/>\n" +
                "        </parameters>\n" +
                "    </agreement>\n" +
                "</terms-of-service>");
        relogin();
        assertOverviewPageRedirectsToAgreement("hosted_teamcity");

        model = GET_Accept_Agreement_Page("hosted_teamcity");
        then(model.get("displayReason")).isEqualTo(AcceptTermsOfServiceController.DisplayReason.NEW_VERSION);
        then(((TermsOfServiceManager.Agreement) model.get("agreement")).getLastUpdated()).isEqualTo("08 January 2018");

        POST_Accept_Agreement_Page("hosted_teamcity");
        assertOverviewPageAccessible();
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

        SUser user = login(createUser("user1"));

        assertOverviewPageRedirectsToAgreement("hosted_teamcity");

        Map<String, Object> model = GET_Accept_Agreement_Page("hosted_teamcity");
        then(((TermsOfServiceManager.Agreement) model.get("agreement")).getConsents()).extracting(TermsOfServiceManager.Consent::getId).contains("analytics", "marketing");

        POST_Accept_Agreement_Page("hosted_teamcity", "analytics");
        then(termsOfServiceManager.getMustAcceptAgreements(user)).hasSize(0);
        assertAgreementUserProperties("hosted_teamcity", "2017.1", request.getRemoteAddr(), timeService.now());
        assertConsentAccepted("hosted_teamcity", "analytics", request.getRemoteAddr(), timeService.now());
        assertConsentNotAccepted("hosted_teamcity", "marketing");
    }

    @Test
    public void should_display_consents_on_user_profile_page() throws Exception {
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
                "          \t<consent id=\"newsletter\" text=\"Allow newsletter\" default=\"false\"/>\n" +
                "        </consents>\n" +
                "    </agreement>\n" +
                "</terms-of-service>");

        SUser user = login(createUser("user1"));

        POST_Accept_Agreement_Page("hosted_teamcity", "analytics", "newsletter");

        assertConsentAccepted("hosted_teamcity", "analytics", request.getRemoteAddr(), timeService.now());
        assertConsentAccepted("hosted_teamcity", "newsletter", request.getRemoteAddr(), timeService.now());
        assertConsentNotAccepted("hosted_teamcity", "marketing");

        //ensure that the profile page shows accepted consents
        TermsOfServiceUserProfileExtension.Form form = GET_User_Consents_Page();
        then(form.getConsentStates()).containsOnly(entry("analytics", true), entry("marketing", false), entry("newsletter", true));

        //revoke one consent
        long previousAcceptedTime = currentTime;
        currentTime += 10 * 60 * 1000;
        POST_User_Consents_Page("analytics");
        assertAgreementUserProperties("hosted_teamcity", "2017.1", request.getRemoteAddr(), previousAcceptedTime);//the acceptance date should not change
        assertConsentNotAccepted("hosted_teamcity", "marketing");
        assertConsentNotAccepted("hosted_teamcity", "newsletter");

        //open profile page again
        form = GET_User_Consents_Page();
        then(form.getConsentStates()).containsOnly(entry("analytics", true), entry("marketing", false), entry("newsletter", false));
    }

    @Test
    public void should_support_external_agreement_link() throws Exception {
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

        assertOverviewPageRedirectsToAgreement("hosted_teamcity");

        Map<String, Object> model = GET_Accept_Agreement_Page("hosted_teamcity");
        then(((TermsOfServiceManager.Agreement) model.get("agreement")).getHtml()).isEqualTo(FileUtil.readText(myAgreementFile, "UTF-8"));
        then(((TermsOfServiceManager.Agreement) model.get("agreement")).getFullName()).isEqualTo("Terms of Service for Hosted TeamCity (teamcity.jetbrains.com)");

        POST_Accept_Agreement_Page("hosted_teamcity");

        assertOverviewPageAccessible();

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
                "            <param name=\"accepted-cookie-name\" value=\"privacy_policy_accepted\"/>\n" +
                "            <param name=\"accepted-cookie-max-age-days\" value=\"10\"/>\n" +
                "        </parameters>\n" +
                "    </guest-notice>\n" +
                "</terms-of-service>");

        login(userModel.getGuestUser());

        assertOverviewPageAccessible();

        then((List<TermsOfServiceManager.Agreement>) linksExtension().get("agreements"))
                .extracting("link")
                .containsOnly("/viewTermsOfServices.html?agreement=privacy_policy");

        TermsOfServiceManager.GuestNotice guestNotice = (TermsOfServiceManager.GuestNotice) guestNoteExtension().get("guestNotice");
        then(guestNotice.getText()).isEqualTo("A privacy reminder from JetBrains");
        then(guestNotice.getHtml()).isEqualTo("Guest Notice");
        then(guestNotice.getCookieName()).isEqualTo("privacy_policy_accepted");
        then(guestNotice.getCookieDurationDays()).isEqualTo(10);
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
        when(user.getBooleanProperty(any(PropertyKey.class))).thenAnswer((Answer<Boolean>) invocation -> Boolean.parseBoolean(properties.get(invocation.<PropertyKey>getArgument(0).getKey())));
        Mockito.doAnswer((Answer<Void>) invocationOnMock -> {
            properties.put(invocationOnMock.<PropertyKey>getArgument(0).getKey(), invocationOnMock.getArgument(1));
            return null;
        }).when(user).setUserProperty(any(PropertyKey.class), any(String.class));
        Mockito.doAnswer((Answer<Void>) invocationOnMock -> {
            properties.remove(invocationOnMock.<PropertyKey>getArgument(0).getKey());
            return null;
        }).when(user).deleteUserProperty(any(PropertyKey.class));
        users.add(user);
        return user;
    }

    private void assertOverviewPageAccessible() throws Exception {
        newRequest(GET, "/overview.html");
        then(interceptor.preHandle(request, response)).isTrue();
    }

    private void assertOverviewPageRedirectsToAgreement(@NotNull String agreementId) throws Exception {
        newRequest(GET, "/overview.html");
        then(interceptor.preHandle(request, response)).isFalse();
        then(response.getRedirectedUrl()).isEqualTo("/acceptTermsOfServices.html?agreement=" + agreementId + "&proceedUrl=%2Foverview.html");
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

    private SUser login(SUser user) {
        securityContext.setAuthorityHolder(user);
        return user;
    }

    private void relogin() {
        session.invalidate();
        session = new MockHttpSession();
    }

    private Map<String, Object> GET_View_Agreement_Page(@NotNull String agreementId) throws IOException {
        newRequest(GET, "/viewTermsOfServices.html?agreement=" + agreementId);
        request.addParameter("agreement", agreementId);
        return viewAgreementController.doHandle(request, response).getModel();

    }

    private Map<String, Object> GET_Accept_Agreement_Page(@NotNull String agreementId) throws IOException {
        newRequest(GET, "/acceptTermsOfServices.html?agreement=" + agreementId);
        request.addParameter("agreement", agreementId);
        return acceptAgreementController.doHandle(request, response).getModel();

    }

    private void POST_Accept_Agreement_Page(@NotNull String agreementId, String... acceptedConsents) throws IOException {
        newRequest(POST, "/acceptTermsOfServices.html");
        request.addParameter("agreement", agreementId);
        for (String acceptedConsent : acceptedConsents) {
            request.addParameter(acceptedConsent, "true");
        }
        acceptAgreementController.doHandle(request, response);
    }


    private TermsOfServiceUserProfileExtension.Form GET_User_Consents_Page() throws IOException {
        newRequest(GET, TermsOfServiceUserProfileExtension.PATH);
        return (TermsOfServiceUserProfileExtension.Form) userConsentsExtension.doGet(request, response).getModel().get("form");
    }

    private void POST_User_Consents_Page(String... acceptedConsents) {
        newRequest(POST, TermsOfServiceUserProfileExtension.PATH);
        for (String acceptedConsent : acceptedConsents) {
            request.addParameter(acceptedConsent, "true");
        }
        userConsentsExtension.doPost(request, response, XmlResponseUtil.newXmlResponse());
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

    private void assertAgreementUserProperties(@NotNull String agreementId, @NotNull String version, @NotNull String acceptedIp, long acceptedDate) {
        SUser user = SessionUser.getUser(request);
        then(user.getPropertyValue(new SimplePropertyKey("teamcity.termsOfService." + agreementId + ".acceptedVersion"))).isEqualTo(version);
        then(user.getPropertyValue(new SimplePropertyKey("teamcity.termsOfService." + agreementId + ".acceptedFromIP"))).isEqualTo(acceptedIp);
        then(user.getPropertyValue(new SimplePropertyKey("teamcity.termsOfService." + agreementId + ".acceptedDate"))).isEqualTo(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(acceptedDate));
    }

    private void assertConsentAccepted(@NotNull String agreementId, @NotNull String consent, @NotNull String acceptedIp, long acceptedDate) {
        SUser user = SessionUser.getUser(request);
        then(user.getPropertyValue(new SimplePropertyKey("teamcity.termsOfService." + agreementId + ".consent." + consent + ".accepted"))).isEqualTo("true");
        then(user.getPropertyValue(new SimplePropertyKey("teamcity.termsOfService." + agreementId + ".consent." + consent + ".acceptedFromIP"))).isEqualTo(acceptedIp);
        then(user.getPropertyValue(new SimplePropertyKey("teamcity.termsOfService." + agreementId + ".consent." + consent + ".acceptedDate"))).isEqualTo(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(acceptedDate));
    }

    private void assertConsentNotAccepted(@NotNull String agreementId, @NotNull String consent) {
        SUser user = SessionUser.getUser(request);
        then(user.getPropertyValue(new SimplePropertyKey("teamcity.termsOfService." + agreementId + ".consent." + consent + ".accepted"))).isNull();
        then(user.getPropertyValue(new SimplePropertyKey("teamcity.termsOfService." + agreementId + ".consent." + consent + ".acceptedFromIP"))).isNull();
        then(user.getPropertyValue(new SimplePropertyKey("teamcity.termsOfService." + agreementId + ".consent." + consent + ".acceptedDate"))).isNull();
    }
}
