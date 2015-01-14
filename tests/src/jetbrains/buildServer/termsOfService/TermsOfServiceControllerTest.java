package jetbrains.buildServer.termsOfService;


import jetbrains.buildServer.controllers.AuthorizationInterceptor;
import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.controllers.BaseControllerTestCase;
import jetbrains.buildServer.controllers.overview.OverviewController;
import jetbrains.buildServer.serverSide.MockServerPluginDescriptior;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.springframework.web.servlet.ModelAndView;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.servlet.ServletContext;

public class TermsOfServiceControllerTest extends BaseControllerTestCase {

    private AuthorizationInterceptor auth;
    private WebControllerManager web;
    private ServletContext context;
    private long myRequestDate;
    private PropertyBasedConfig myConfig;


    @Override
    protected BaseController createController() {
        myConfig = new PropertyBasedConfig("property1", "/termsOfServices.html");
        myController = new TermsOfServiceController(myServer, myWebManager, new MockServerPluginDescriptior(), new PropertyBasedManager(myConfig));
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