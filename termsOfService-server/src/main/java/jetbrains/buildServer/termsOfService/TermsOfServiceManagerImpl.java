package jetbrains.buildServer.termsOfService;

import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.users.SimplePropertyKey;
import jetbrains.buildServer.users.UserModel;
import jetbrains.buildServer.util.*;
import jetbrains.buildServer.web.util.WebUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import javax.annotation.concurrent.ThreadSafe;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;

@ThreadSafe
public class TermsOfServiceManagerImpl implements TermsOfServiceManager {
    private static final String TEAMCITY_TERMS_OF_SERVICE_ENABLED_PROPERTY = "teamcity.termsOfService.enabled";

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

    private synchronized void reloadConfig(@NotNull Element config) {
        TermsOfServiceLogger.LOGGER.info("Loading Terms Of Service configuration from " + myConfig.getMainConfig());
        readAgreements(config);
        readExternalAgreement(config);
        readGuestNotice(config);
    }

    private void readAgreements(@NotNull Element config) {
        myAgreements.clear();
        List agreementElements = config.getChildren("agreement");
        if (agreementElements.isEmpty()) {
            TermsOfServiceLogger.LOGGER.warn("Broken Terms Of Service configuration: no 'agreement' elements found in " + myConfig.getMainConfig());
        }

        for (Object agreementEl : agreementElements) {
            Element paramsElement = ((Element) agreementEl).getChild("parameters");
            Map<String, String> params = paramsElement == null ? emptyMap() : XmlUtil.readParameters(paramsElement);
            String agreementId = ((Element) agreementEl).getAttributeValue("id");
            String agreementFileParam = params.get("content-file");

            if (StringUtil.isEmptyOrSpaces(agreementId)) {
                TermsOfServiceLogger.LOGGER.warn("Broken Terms Of Service configuration: missing agreement id, the agreement is ignored.");
                continue;
            }

            if (StringUtil.isEmptyOrSpaces(agreementFileParam)) {
                TermsOfServiceLogger.LOGGER.warn("Broken Terms Of Service configuration: missing 'content-file' parameter for agreement id = " + agreementId + ", the agreement is ignored.");
                continue;
            }

            File agreementFile = myConfig.getConfigFile(agreementFileParam);
            String agreementContent;

            if (agreementFile.exists() && agreementFile.isFile()) {
                try {
                    agreementContent = FileUtil.readText(agreementFile, "UTF-8");
                } catch (IOException e) {
                    TermsOfServiceLogger.LOGGER.warnAndDebugDetails("Error while reading Terms Of Service agreement file from " + agreementFile + " for agreement id = " + agreementId, e);
                    continue;
                }
            } else {
                TermsOfServiceLogger.LOGGER.warn("Broken Terms Of Service configuration: agreement file '" + agreementFile + "' doesn't exist for agreement id = " + agreementId);
                continue;
            }


            List<Consent> consents = new ArrayList<>();
            Element consentsEl = ((Element) agreementEl).getChild("consents");
            if (consentsEl != null) {
                for (Object consent : consentsEl.getChildren("consent")) {
                    Element consentEl = ((Element) consent);
                    String id = consentEl.getAttributeValue("id");
                    String text = consentEl.getAttributeValue("text");

                    if (StringUtil.isEmptyOrSpaces(id)) {
                        TermsOfServiceLogger.LOGGER.warn("Broken Terms Of Service configuration: missing consent id, the consent is ignored.");
                        continue;
                    }

                    if (StringUtil.isEmptyOrSpaces(text)) {
                        TermsOfServiceLogger.LOGGER.warn("Broken Terms Of Service configuration: missing consent text, the consent is ignored.");
                        continue;
                    }

                    boolean checked = Boolean.parseBoolean(consentEl.getAttributeValue("default"));
                    consents.add(new ConsentImpl(id, text, checked, agreementId));
                }
            }

            myAgreements.add(new AgreementImpl(agreementId, agreementContent, params, consents));
        }
    }

    private void readExternalAgreement(@NotNull Element config) {
        externalAgreements.clear();
        for (Object agreementEl : config.getChildren("external-agreement-link")) {
            String text = ((Element) agreementEl).getAttributeValue("text");
            String url = ((Element) agreementEl).getAttributeValue("url");

            if (StringUtil.isEmptyOrSpaces(text)) {
                TermsOfServiceLogger.LOGGER.warn("Broken Terms Of Service configuration: missing external agreement text, the agreement is ignored.");
                continue;
            }

            if (StringUtil.isEmptyOrSpaces(url)) {
                TermsOfServiceLogger.LOGGER.warn("Broken Terms Of Service configuration: missing external agreement url, the agreement is ignored.");
                continue;
            }

            externalAgreements.add(new ExternalAgreementLinkSettings(text, url));
        }
    }

    private void readGuestNotice(@NotNull Element config) {
        Element guestNoticeEl = config.getChild("guest-notice");
        if (guestNoticeEl != null) {
            Element paramsElement = guestNoticeEl.getChild("parameters");
            Map<String, String> params = paramsElement == null ? emptyMap() : XmlUtil.readParameters(paramsElement);
            String text = params.get("text");
            String contentFile = params.get("content-file");

            if (StringUtil.isEmptyOrSpaces(text)) {
                TermsOfServiceLogger.LOGGER.warn("Broken Terms Of Service configuration: missing guest notice text, the guest notice is ignored.");
                return;
            }

            if (StringUtil.isEmptyOrSpaces(contentFile)) {
                TermsOfServiceLogger.LOGGER.warn("Broken Terms Of Service configuration: missing 'content-file' parameter for a guest notice, the guest notice is ignored.");
                return;
            }

            File guestNoticeFile = myConfig.getConfigFile(params.get("content-file"));

            if (guestNoticeFile.exists() && guestNoticeFile.isFile()) {
                try {
                    String guestNoticeContent = FileUtil.readText(guestNoticeFile, "UTF-8");
                    String cookieName = params.getOrDefault("accepted-cookie-name", "guest-notice-accepted");
                    int cookieDurationMinutes = StringUtil.parseInt(params.getOrDefault("accepted-cookie-max-age-days", "30"), 30);
                    myGuestNotice = new GuestNoticeSettings(text, guestNoticeContent, cookieName, cookieDurationMinutes);
                } catch (IOException e) {
                    TermsOfServiceLogger.LOGGER.warnAndDebugDetails("Error while reading guest notice content from " + guestNoticeFile, e);
                }
            } else {
                TermsOfServiceLogger.LOGGER.warn("Broken Terms Of Service configuration: guest notice content file '" + guestNoticeFile + "' doesn't exist");
            }
        }
    }

    @NotNull
    @Override
    public synchronized List<Agreement> getMustAcceptAgreements(@NotNull SUser user) {
        if (!TeamCityProperties.getBooleanOrTrue(TEAMCITY_TERMS_OF_SERVICE_ENABLED_PROPERTY)) {
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
    public synchronized Optional<GuestNotice> getGuestNotice() {
        if (myGuestNotice == null) {
            return Optional.empty();
        }

        return Optional.of(myGuestNotice);
    }

    private final class AgreementImpl implements Agreement {

        @NotNull
        private final String id;
        @NotNull
        private final String html;
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
        public String getVersion() {
            return params.getOrDefault("version", "1");
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
            return acceptedVersion != null && VersionComparatorUtil.compare(acceptedVersion, getVersion()) >= 0;
        }

        @Override
        public void accept(@NotNull SUser user, @NotNull HttpServletRequest request) {
            user.setUserProperty(new SimplePropertyKey("teamcity.policy." + id + ".acceptedVersion"), getVersion());
            user.setUserProperty(new SimplePropertyKey("teamcity.policy." + id + ".acceptedDate"), new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(timeService.now()));
            user.setUserProperty(new SimplePropertyKey("teamcity.policy." + id + ".acceptedFromIP"), WebUtil.getRemoteAddress(request));
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
        private final String cookieName;
        private final int cookieDurationDays;

        GuestNoticeSettings(@NotNull String text, @NotNull String htmlContent, @NotNull String cookieName, int cookieDurationDays) {
            this.text = text;
            this.htmlContent = htmlContent;
            this.cookieName = cookieName;
            this.cookieDurationDays = cookieDurationDays;
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

        @NotNull
        @Override
        public String getCookieName() {
            return cookieName;
        }

        @Override
        public int getCookieDurationDays() {
            return cookieDurationDays;
        }
    }

    class ConsentImpl implements TermsOfServiceManager.Consent {
        @NotNull
        private final String id;
        @NotNull
        private final String text;
        private final boolean checked;

        @NotNull
        private final String agreementId;

        ConsentImpl(@NotNull String id, @NotNull String text, boolean checked, @NotNull String agreementId) {
            this.id = id;
            this.text = text;
            this.checked = checked;
            this.agreementId = agreementId;
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

        @Override
        public boolean isAccepted(@NotNull SUser user) {
            return user.getBooleanProperty(getAcceptedPropertyKey());
        }

        @Override
        public void changeAcceptedState(@NotNull SUser user, boolean accepted, @NotNull String acceptedFromIp) {
            SimplePropertyKey acceptedProp = getAcceptedPropertyKey();
            SimplePropertyKey acceptedDateProp = new SimplePropertyKey("teamcity.policy." + agreementId + ".consent." + id + ".acceptedDate");
            SimplePropertyKey acceptedIpProp = new SimplePropertyKey("teamcity.policy." + agreementId + ".consent." + id + ".acceptedFromIP");
            if (accepted) {
                if (!user.getBooleanProperty(acceptedProp)){ //don't overwrite if already accepted
                    user.setUserProperty(acceptedProp, "true");
                    user.setUserProperty(acceptedDateProp, new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(timeService.now()));
                    user.setUserProperty(acceptedIpProp, acceptedFromIp);
                }
            } else {
                user.deleteUserProperty(acceptedProp);
                user.deleteUserProperty(acceptedDateProp);
                user.deleteUserProperty(acceptedIpProp);
            }
        }

        @NotNull
        private SimplePropertyKey getAcceptedPropertyKey() {
            return new SimplePropertyKey("teamcity.policy." + agreementId + ".consent." + id + ".accepted");
        }
    }
}
