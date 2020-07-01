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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Predicate;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;

@ThreadSafe
public class TermsOfServiceManagerImpl implements TermsOfServiceManager {
    private static final String TEAMCITY_TERMS_OF_SERVICE_ENABLED_PROPERTY = "teamcity.termsOfService.enabled";
    private static final String TEAMCITY_TERMS_OF_SERVICE_ASK_SUPER_USER_PROPERTY = "teamcity.termsOfService.askSuperUser";
    private static final String ACCEPTED_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    @NotNull
    private final TermsOfServiceConfig myConfig;
    @NotNull
    private final UserModel userModel;
    @NotNull
    private final TimeService timeService;

    @NotNull
    private final List<AgreementImpl> myAgreements = new ArrayList<>();
    @NotNull
    private final List<ExternalAgreementLink> externalAgreements = new ArrayList<>();
    @Nullable
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
        TermsOfServiceLogger.LOGGER.debug("Loading configuration from " + myConfig.getMainConfig());
        readAgreements(config);
        readExternalAgreement(config);
        readGuestNotice(config);
        String msg = "Configuration was loaded from " + myConfig.getMainConfig() + ", ";
        if (!myAgreements.isEmpty()) {
            msg += myAgreements.size() + " " + StringUtil.pluralize("agreement", myAgreements.size())+ " loaded";
        } else {
            msg += "no agreements were loaded";
        }
        if (myGuestNotice != null) {
            msg += ", guest notice was loaded";
        }
        TermsOfServiceLogger.LOGGER.info(msg);

    }

    private void readAgreements(@NotNull Element config) {
        myAgreements.clear();
        List agreementElements = config.getChildren("agreement");
        if (agreementElements.isEmpty()) {
            TermsOfServiceLogger.LOGGER.warn("Broken configuration: no 'agreement' elements found in " + myConfig.getMainConfig());
        }

        for (Object agreementElObj : agreementElements) {
            Element agreementEl = (Element) agreementElObj;

            Element paramsElement = agreementEl.getChild("parameters");
            Map<String, String> params = paramsElement == null ? emptyMap() : XmlUtil.readParameters(paramsElement);
            String agreementId = agreementEl.getAttributeValue("id");
            String agreementFileParam = params.get("content-file");

            if (StringUtil.isEmptyOrSpaces(agreementId)) {
                TermsOfServiceLogger.LOGGER.warn("Broken configuration: missing agreement id, the agreement is ignored.");
                continue;
            }

            if (StringUtil.isEmptyOrSpaces(agreementFileParam)) {
                TermsOfServiceLogger.LOGGER.warn("Broken configuration: missing 'content-file' parameter for agreement id = '" + agreementId + "', the agreement is ignored.");
                continue;
            }

            if (isEnabled(agreementEl)) {
                TermsOfServiceLogger.LOGGER.info("Agreement '" + agreementId + "' is disabled, to enable change 'enabled' attribute value to 'true'");
                continue;
            }


            File agreementFile = myConfig.getConfigFile(agreementFileParam);

            if (!FileUtil.isAncestor(myConfig.getConfigDir(), agreementFile, false)) {
                TermsOfServiceLogger.LOGGER.warn("Agreement file '" + agreementFile + "' is outside of the allowed directory '" + myConfig.getConfigDir() + "', the agreement is ignored.");
                continue;
            }

            String agreementContent;

            if (agreementFile.exists() && agreementFile.isFile()) {
                try {
                    agreementContent = FileUtil.readText(agreementFile, "UTF-8");
                } catch (IOException e) {
                    TermsOfServiceLogger.LOGGER.warnAndDebugDetails("Error while reading agreement file from " + agreementFile + " for agreement id = '" + agreementId + "'", e);
                    continue;
                }
            } else {
                TermsOfServiceLogger.LOGGER.warn("Broken configuration: agreement file '" + agreementFile + "' doesn't exist for agreement id = '" + agreementId + "'");
                continue;
            }


            List<Consent> consents = new ArrayList<>();
            Element consentsEl = agreementEl.getChild("consents");
            if (consentsEl != null) {
                for (Object consent : consentsEl.getChildren("consent")) {
                    Element consentEl = ((Element) consent);
                    String id = consentEl.getAttributeValue("id");
                    String html;

                    if (consentEl.getAttributeValue("file") != null) {
                        File consentContentFile = myConfig.getConfigFile(consentEl.getAttributeValue("file"));
                        if (consentContentFile.exists() && consentContentFile.isFile()) {
                            try {
                                html = FileUtil.readText(consentContentFile, "UTF-8");
                            } catch (IOException e) {
                                TermsOfServiceLogger.LOGGER.warnAndDebugDetails("Error while reading consent file from " + consentContentFile + " for agreement id = '" + agreementId + "'", e);
                                continue;
                            }
                        } else {
                            TermsOfServiceLogger.LOGGER.warn("Broken configuration: consent file '" + consentContentFile + "' doesn't exist for agreement id = '" + agreementId + "', consent is skipped");
                            continue;
                        }
                    } else {
                        html = consentEl.getAttributeValue("text");
                    }

                    if (StringUtil.isEmptyOrSpaces(id)) {
                        TermsOfServiceLogger.LOGGER.warn("Broken configuration: missing consent id, the consent is ignored.");
                        continue;
                    }

                    if (StringUtil.isEmptyOrSpaces(html)) {
                        TermsOfServiceLogger.LOGGER.warn("Broken configuration: missing consent text/file, the consent is ignored.");
                        continue;
                    }

                    boolean checked = Boolean.parseBoolean(consentEl.getAttributeValue("default"));
                    consents.add(new ConsentImpl(id, html, checked, agreementId));
                }
            }

            Predicate<SUser> userPredicate = user -> true;
            String usersFilter = agreementEl.getAttributeValue("user-filter");
            if (usersFilter != null) {
                if (!usersFilter.startsWith("username:")) {
                    TermsOfServiceLogger.LOGGER.warn("Broken configuration: unsupported user filter '" + usersFilter + "'. " +
                            "Currently only username filters are supported, for example 'username:admin'.");
                } else {
                    userPredicate = user -> user.getUsername().equals(usersFilter.substring("username:".length()));
                }
            }

            myAgreements.add(new AgreementImpl(agreementId, agreementContent, params, consents, userPredicate));
        }
    }

    private void readExternalAgreement(@NotNull Element config) {
        externalAgreements.clear();
        for (Object agreementEl : config.getChildren("external-agreement-link")) {
            String text = ((Element) agreementEl).getAttributeValue("text");
            String url = ((Element) agreementEl).getAttributeValue("url");

            if (StringUtil.isEmptyOrSpaces(text)) {
                TermsOfServiceLogger.LOGGER.warn("Broken configuration: missing external agreement text, the agreement is ignored.");
                continue;
            }

            if (StringUtil.isEmptyOrSpaces(url)) {
                TermsOfServiceLogger.LOGGER.warn("Broken configuration: missing external agreement url, the agreement is ignored.");
                continue;
            }

            externalAgreements.add(new ExternalAgreementLinkSettings(text, url));
        }
    }

    private void readGuestNotice(@NotNull Element config) {
        myGuestNotice = null;
        Element guestNoticeEl = config.getChild("guest-notice");
        if (guestNoticeEl != null) {

            if (isEnabled(guestNoticeEl)) {
                TermsOfServiceLogger.LOGGER.info("Guest Notice is disabled, to enable change 'enabled' attribute value to 'true'");
                return;
            }

            Element paramsElement = guestNoticeEl.getChild("parameters");
            Map<String, String> params = paramsElement == null ? emptyMap() : XmlUtil.readParameters(paramsElement);
            String title = params.get("title");
            String note = params.get("note");
            String contentFile = params.get("content-file");

            if (StringUtil.isEmptyOrSpaces(title)) {
                TermsOfServiceLogger.LOGGER.warn("Broken configuration: missing guest notice title, the guest notice is ignored.");
                return;
            }

            if (StringUtil.isEmptyOrSpaces(contentFile)) {
                TermsOfServiceLogger.LOGGER.warn("Broken configuration: missing 'content-file' parameter for a guest notice, the guest notice is ignored.");
                return;
            }

            File guestNoticeFile = myConfig.getConfigFile(params.get("content-file"));

            if (guestNoticeFile.exists() && guestNoticeFile.isFile()) {
                try {
                    String guestNoticeContent = FileUtil.readText(guestNoticeFile, "UTF-8");
                    String cookieName = params.getOrDefault("accepted-cookie-name", "guest-notice-accepted");
                    int cookieDurationMinutes = StringUtil.parseInt(params.getOrDefault("accepted-cookie-max-age-days", "30"), 30);
                    myGuestNotice = new GuestNoticeSettings(title, note, guestNoticeContent, cookieName, cookieDurationMinutes);
                } catch (IOException e) {
                    TermsOfServiceLogger.LOGGER.warnAndDebugDetails("Error while reading guest notice content from " + guestNoticeFile, e);
                }
            } else {
                TermsOfServiceLogger.LOGGER.warn("Broken configuration: guest notice content file '" + guestNoticeFile + "' doesn't exist");
            }
        }
    }

    private boolean isEnabled(@NotNull Element element) {
        return element.getAttributeValue("enabled") != null && !Boolean.parseBoolean(element.getAttributeValue("enabled"));
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

        if (userModel.isSuperUser(user) && !TeamCityProperties.getBooleanOrTrue(TEAMCITY_TERMS_OF_SERVICE_ASK_SUPER_USER_PROPERTY)) {
            return Collections.emptyList();
        }

        return getAgreements(user).stream().filter(a -> !a.isAccepted(user)).collect(toList());
    }

    @NotNull
    public synchronized List<Agreement> getAgreements(@NotNull SUser user) {
        return myAgreements.stream().filter(a -> a.userPredicate.test(user)).collect(toList());
    }

    @NotNull
    @Override
    public synchronized Optional<Agreement> findAgreement(@NotNull String id) {
        Optional<AgreementImpl> found = myAgreements.stream().filter(a -> a.getId().equals(id)).findFirst();
        return Optional.ofNullable(found.orElse(null));
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
        @Nullable
        private final Date enforcementDate;
        @NotNull
        private final Predicate<SUser> userPredicate;

        AgreementImpl(@NotNull String id, @NotNull String html, @NotNull Map<String, String> params, @NotNull List<Consent> consents, @NotNull Predicate<SUser> userPredicate) {
            this.id = id;
            this.html = html;
            this.params = params;
            this.consents = consents;
            this.userPredicate = userPredicate;

            String enforcementDateStr = params.get("enforcement-date");
            Date parsedEnforcementDate = null;
            if (enforcementDateStr != null) {
                String pattern = "yyyy-MM-dd'T'HH:mmZ";
                try {
                    parsedEnforcementDate = new SimpleDateFormat(pattern).parse(enforcementDateStr);
                } catch (ParseException e) {
                    TermsOfServiceLogger.LOGGER.warnAndDebugDetails("Invalid 'enforcement-date' date format for the agreement '" + id + "', supported format is: " + pattern, e);
                    parsedEnforcementDate = null;
                }
            }
            this.enforcementDate = parsedEnforcementDate;
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
        public boolean isEnforcedForActiveSessions() {
            return enforcementDate != null && Dates.isBeforeWithError(enforcementDate, new Date(timeService.now()), 0);
        }

        @Override
        public boolean isAccepted(@NotNull SUser user) {
            String acceptedVersion = user.getPropertyValue(getAcceptedVersionKey());
            return acceptedVersion != null && VersionComparatorUtil.compare(acceptedVersion, getVersion()) >= 0;
        }

        @Override
        public boolean isAnyVersionAccepted(@NotNull SUser user) {
            return user.getPropertyValue(getAcceptedVersionKey()) != null;
        }

        @NotNull
        @Override
        public String getNewUserNote() {
            return StringUtil.notNullize(params.get("new-user-note"),
                    "You have to accept the " + getShortName() + " agreement before you can continue to use TeamCity. " +
                    "Review the terms and click \"I agree\" when you're ready to proceed.");
        }

        @NotNull
        @Override
        public String getNewVersionNote() {
            return StringUtil.notNullize(params.get("new-version-note"),
                    "We've updated the " + getShortName() + " agreement. " +
                    "Review the terms and click \"I agree\" when you're ready to continue using TeamCity.");
        }

        @Override
        public void accept(@NotNull SUser user, @NotNull HttpServletRequest request) {
            user.setUserProperty(getAcceptedVersionKey(), getVersion());
            user.setUserProperty(new SimplePropertyKey("teamcity.termsOfService." + id + ".acceptedDate"), new SimpleDateFormat(ACCEPTED_DATE_FORMAT).format(timeService.now()));
            user.setUserProperty(new SimplePropertyKey("teamcity.termsOfService." + id + ".acceptedFromIP"), WebUtil.getRemoteAddress(request));
        }

        @Override
        public String toString() {
            return "Agreement " + StringUtil.notNullize(params.get("short-name"), "Terms of Service") + " (id = " + id + ")";
        }

        @NotNull
        private SimplePropertyKey getAcceptedVersionKey() {
            return new SimplePropertyKey("teamcity.termsOfService." + id + ".acceptedVersion");
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
        private final String title;
        private final String note;
        private final String htmlContent;
        private final String cookieName;
        private final int cookieDurationDays;

        GuestNoticeSettings(@NotNull String title,
                            @Nullable String note,
                            @NotNull String htmlContent, @NotNull String cookieName, int cookieDurationDays) {
            this.title = title;
            this.note = note;
            this.htmlContent = htmlContent;
            this.cookieName = cookieName;
            this.cookieDurationDays = cookieDurationDays;
        }

        @NotNull
        public String getTitle() {
            return title;
        }

        @Nullable
        public String getNote() {
            return note;
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
        public String getHtml() {
            return text;
        }

        @Override
        public boolean isAccepted(@NotNull SUser user) {
            return user.getBooleanProperty(getAcceptedPropertyKey());
        }

        @Override
        public void changeAcceptedState(@NotNull SUser user, boolean accepted, @NotNull String acceptedFromIp) {
            SimplePropertyKey acceptedProp = getAcceptedPropertyKey();
            SimplePropertyKey acceptedDateProp = new SimplePropertyKey("teamcity.termsOfService." + agreementId + ".consent." + id + ".acceptedDate");
            SimplePropertyKey acceptedIpProp = new SimplePropertyKey("teamcity.termsOfService." + agreementId + ".consent." + id + ".acceptedFromIP");
            if (accepted) {
                if (!user.getBooleanProperty(acceptedProp)){ //don't overwrite if already accepted
                    user.setUserProperty(acceptedProp, "true");
                    user.setUserProperty(acceptedDateProp, new SimpleDateFormat(ACCEPTED_DATE_FORMAT).format(timeService.now()));
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
            return new SimplePropertyKey("teamcity.termsOfService." + agreementId + ".consent." + id + ".accepted");
        }
    }
}
