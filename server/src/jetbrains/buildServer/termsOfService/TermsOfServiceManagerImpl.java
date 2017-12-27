package jetbrains.buildServer.termsOfService;

import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.users.SimplePropertyKey;
import jetbrains.buildServer.users.UserModel;
import jetbrains.buildServer.util.*;
import jetbrains.buildServer.web.util.WebUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.concurrent.ThreadSafe;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.intellij.openapi.util.text.StringUtil.isNotEmpty;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;

@ThreadSafe
public class TermsOfServiceManagerImpl implements TermsOfServiceManager {
    static final String TEAMCITY_TERMS_OF_SERVICE_ENABLED_PROPERTY = "teamcity.termsOfService.enabled";

    @NotNull
    private final TermsOfServiceConfig myConfig;
    @NotNull
    private final UserModel userModel;
    @NotNull
    private final TimeService timeService;

    private final List<Agreement> myAgreements = new ArrayList<>();
    private final List<ExternalAgreementLink> externalAgreements = new ArrayList<>();
    private volatile GuestNotice myGuestNotice = null;

    public TermsOfServiceManagerImpl(@NotNull TermsOfServiceConfig config,
                                     @NotNull UserModel userModel,
                                     @NotNull TimeService timeService) {
        myConfig = config;
        this.userModel = userModel;
        this.timeService = timeService;
        config.setOnChange(this::reloadConfig);
    }

    private void reloadConfig(@NotNull Element config) {
        myAgreements.clear();
        externalAgreements.clear();

        for (Object agreementEl : config.getChildren("agreement")) {
            Element paramsElement = ((Element) agreementEl).getChild("parameters");
            Map<String, String> params = paramsElement == null ? emptyMap() : XmlUtil.readParameters(paramsElement);
            if (params.get("content-file") != null) {
                List<TermsOfServiceManager.Consent> consents = new ArrayList<>();
                Element consentsEl = ((Element) agreementEl).getChild("consents");
                if (consentsEl != null) {
                    for (Object consent : consentsEl.getChildren("consent")) {
                        Element consentEl = ((Element) consent);
                        String id = consentEl.getAttributeValue("id");
                        String text = consentEl.getAttributeValue("text");
                        boolean checked = Boolean.parseBoolean(consentEl.getAttributeValue("checked"));
                        if (isNotEmpty(id) && isNotEmpty(text)) {
                            consents.add(new ConsentSettings(id, text, checked));
                        }
                    }
                }

                String agreementFileParam = params.get("content-file");
                File agreementFile = myConfig.getConfigFile(agreementFileParam);
                try {
                    myAgreements.add(new AgreementImpl(((Element) agreementEl).getAttributeValue("id"), FileUtil.readText(agreementFile, "UTF-8"), params, consents));
                } catch (IOException e) {
                    TermsOfServiceLogger.LOGGER.warnAndDebugDetails("Error while reading Terms Of Service agreement file from " + agreementFile, e);
                }
            }
        }

        for (Object agreementEl : config.getChildren("externalAgreementLink")) {
            externalAgreements.add(new ExternalAgreementLinkSettings(((Element) agreementEl).getAttributeValue("text"), ((Element) agreementEl).getAttributeValue("url")));
        }

        Element guestNoticeEl = config.getChild("guest-notice");
        if (guestNoticeEl != null) {
            Element paramsElement = guestNoticeEl.getChild("parameters");
            Map<String, String> params = paramsElement == null ? emptyMap() : XmlUtil.readParameters(paramsElement);
            if (params.get("content-file") != null && params.get("text") != null) {
                File guestNoticeFile = myConfig.getConfigFile(params.get("content-file"));
                try {
                    myGuestNotice = new GuestNoticeSettings(params.get("text"), FileUtil.readText(guestNoticeFile, "UTF-8"));
                } catch (IOException e) {
                    TermsOfServiceLogger.LOGGER.warnAndDebugDetails("Error while reading Guest Notice file from " + guestNoticeFile, e);
                }

            }
        }
    }

    @NotNull
    @Override
    public List<Agreement> getMustAcceptAgreements(@NotNull SUser user) {
        if (!TeamCityProperties.getBoolean(TEAMCITY_TERMS_OF_SERVICE_ENABLED_PROPERTY)) {
            return Collections.emptyList();
        }
        if (userModel.isGuestUser(user)) {
            return Collections.emptyList();
        }

        return getAgreements().stream().filter(a -> !a.isAccepted(user)).collect(toList());
    }

    @NotNull
    public synchronized List<Agreement> getAgreements() {
        return new ArrayList<>(myAgreements);
    }

    @NotNull
    @Override
    public synchronized Optional<Agreement> findAgreement(@NotNull String id) {
        return myAgreements.stream().filter(a -> a.getId().equals(id)).findFirst();
    }

    @NotNull
    @Override
    public synchronized List<ExternalAgreementLink> getExternalAgreements() {
        return new ArrayList<>(externalAgreements);
    }

    @NotNull
    @Override
    public Optional<GuestNotice> getGuestNotice() {
        if (myGuestNotice == null) {
            return Optional.empty();
        }

        return Optional.of(myGuestNotice);
    }

    private final class AgreementImpl implements Agreement {

        @NotNull private final String id;
        @NotNull private final String html;
        private final Map<String, String> params;
        private final List<Consent> consents;

        AgreementImpl(@NotNull String id, @NotNull String html, @NotNull Map<String, String> params, @NotNull List<Consent> consents) {
            this.id = id;
            this.html = html;
            this.params = params;
            this.consents = consents;
        }

        @NotNull
        @Override
        public String getId() {
            return id;
        }

        @NotNull
        @Override
        public String getShortName() {
            return StringUtil.notNullize(params.get("short-name"), "Terms of Service");
        }

        @NotNull
        @Override
        public String getFullName() {
            return StringUtil.notNullize(params.get("full-name"), "Terms of Service");
        }

        @NotNull
        @Override
        public String getHtml() {
            return html;

        }

        @NotNull
        @Override
        public String getLink() {
            return ViewTermsOfServiceController.PATH + "?agreement=" + id;
        }

        @NotNull
        @Override
        public List<Consent> getConsents() {
            return consents;
        }

        @Override
        public boolean isAccepted(@NotNull SUser user) {
            String acceptedVersion = user.getPropertyValue(new SimplePropertyKey("teamcity.policy." + id + ".acceptedVersion"));
            return acceptedVersion != null && VersionComparatorUtil.compare(acceptedVersion, params.getOrDefault("version", "1")) >= 0;
        }

        @Override
        public void accept(@NotNull SUser user, @NotNull HttpServletRequest request) {
            user.setUserProperty(new SimplePropertyKey("teamcity.policy." + id + ".acceptedVersion"), params.getOrDefault("version", "1"));
            user.setUserProperty(new SimplePropertyKey("teamcity.policy." + id + ".acceptedDate"), new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(timeService.now()));
            user.setUserProperty(new SimplePropertyKey("teamcity.policy." + id + ".acceptedFromIP"), WebUtil.getRemoteAddress(request));
        }

        @Override
        public void changeConsentState(@NotNull SUser user, @NotNull String consentId, boolean agreed, @NotNull HttpServletRequest request) {
            if (agreed) {
                user.setUserProperty(new SimplePropertyKey("teamcity.policy." + id + ".consent." + consentId + ".accepted"), "true");
                user.setUserProperty(new SimplePropertyKey("teamcity.policy." + id + ".consent." + consentId + ".acceptedDate"), new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(timeService.now()));
                user.setUserProperty(new SimplePropertyKey("teamcity.policy." + id + ".consent." + consentId + ".acceptedFromIP"), WebUtil.getRemoteAddress(request));
            } else {
                user.deleteUserProperty(new SimplePropertyKey("teamcity.policy." + id + ".consent." + consentId + ".accepted"));
                user.deleteUserProperty(new SimplePropertyKey("teamcity.policy." + id + ".consent." + consentId + ".acceptedDate"));
                user.deleteUserProperty(new SimplePropertyKey("teamcity.policy." + id + ".consent." + consentId + ".acceptedFromIP"));
            }
        }


        @Override
        public String toString() {
            return "Agreement " + StringUtil.notNullize(params.get("short-name"), "Terms of Service") + " (id = " + id + ")";
        }

    }

    class ExternalAgreementLinkSettings implements TermsOfServiceManager.ExternalAgreementLink {
        private final String text;
        private final String url;

        ExternalAgreementLinkSettings(String text, String url) {
            this.text = text;
            this.url = url;
        }

        @NotNull
        @Override
        public String getName() {
            return text;
        }

        @NotNull
        @Override
        public String getUrl() {
            return url;
        }
    }

    class GuestNoticeSettings implements GuestNotice {
        private final String text;
        private final String htmlContent;

        GuestNoticeSettings(@NotNull String text, @NotNull String htmlContent) {
            this.text = text;
            this.htmlContent = htmlContent;
        }

        @NotNull
        public String getText() {
            return text;
        }

        @NotNull
        @Override
        public String getHtml() {
            return htmlContent;
        }
    }

    class ConsentSettings implements TermsOfServiceManager.Consent {
        @NotNull
        private final String id;
        @NotNull
        private final String text;
        private final boolean checked;

        ConsentSettings(@NotNull String id, @NotNull String text, boolean checked) {
            this.id = id;
            this.text = text;
            this.checked = checked;
        }

        @NotNull
        @Override
        public String getId() {
            return id;
        }

        @Override
        public boolean isCheckedByDefault() {
            return checked;
        }

        @NotNull
        @Override
        public String getText() {
            return text;
        }
    }
}
