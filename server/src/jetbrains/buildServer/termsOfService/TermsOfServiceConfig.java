package jetbrains.buildServer.termsOfService;

import jetbrains.buildServer.configuration.FileWatcher;
import jetbrains.buildServer.serverSide.BuildServerAdapter;
import jetbrains.buildServer.serverSide.BuildServerListener;
import jetbrains.buildServer.serverSide.ServerPaths;
import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.serverSide.impl.FileWatcherFactory;
import jetbrains.buildServer.users.PropertyKey;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.users.SimplePropertyKey;
import jetbrains.buildServer.util.EventDispatcher;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.util.XmlUtil;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.concurrent.ThreadSafe;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;

@ThreadSafe
public class TermsOfServiceConfig {
    private static final String CONFIG_FILE = "terms-of-service-config.xml";

    private final List<AgreementSettings> myAgreements = new ArrayList<>();

    private final File myConfigDir;
    private final File mySettingsFile;

    public TermsOfServiceConfig(@NotNull EventDispatcher<BuildServerListener> myEvents,
                                @NotNull ServerPaths serverPaths,
                                @NotNull FileWatcherFactory fileWatcherFactory) {
        myConfigDir = new File(serverPaths.getConfigDir(), "termsOfService");
        mySettingsFile = new File(myConfigDir, CONFIG_FILE);

        int watchInterval = TeamCityProperties.getInteger("teamcity.termsOfService.configWatchInterval", 10000);
        FileWatcher filesWatcher = fileWatcherFactory.createSingleFilesWatcher(mySettingsFile, watchInterval);

        filesWatcher.registerListener(requestor -> loadSettings());
        myEvents.addListener(new BuildServerAdapter() {
            @Override
            public void serverStartup() {
                loadSettings();
                filesWatcher.start();
            }

            @Override
            public void serverShutdown() {
                filesWatcher.stop();
            }
        });
    }

    @NotNull
    public synchronized List<AgreementSettings> getAgreements(@NotNull SUser user) {
        return myAgreements.stream().filter(e -> e.userCondition.shouldAccept(user)).collect(toList());
    }

    synchronized void loadSettings() {
        try {
            if (mySettingsFile.exists()) {
                myAgreements.clear();
                Element parsed = FileUtil.parseDocument(mySettingsFile, false);
                for (Object child : parsed.getChildren("agreement")) {
                    for (Object rule : ((Element) child).getChildren("rule")) {
                        if ("ALL_USERS".equals(((Element) rule).getAttributeValue("type"))) {
                            Element paramsElement = ((Element) rule).getChild("parameters");
                            Map<String, String> params = paramsElement == null ? emptyMap() : XmlUtil.readParameters(paramsElement);
                            if (params.get("agreement-file") != null || params.get("agreement-link") != null) {
                                myAgreements.add(new AgreementSettings(((Element) child).getAttributeValue("id"), user -> user.isPermissionGrantedForAnyProject(Permission.CHANGE_OWN_PROFILE), params));
                            }
                        }
                    }
                }
            }

            if (myAgreements.isEmpty()) {
                TermsOfServiceManager.LOGGER.warn("No Terms of Service rules were found in " + FileUtil.getCanonicalFile(mySettingsFile).getPath());
            }
        } catch (IOException | JDOMException e) {
            TermsOfServiceManager.LOGGER.warnAndDebugDetails("Error while loading Terms Of Service settings from " + FileUtil.getCanonicalFile(mySettingsFile).getPath(), e);
        }
    }

    class AgreementSettings {

        private final String id;
        private final UserCondition userCondition;
        private final Map<String, String> params;

        AgreementSettings(@NotNull String id, @NotNull UserCondition userCondition, @NotNull Map<String, String> params) {
            this.id = id;
            this.userCondition = userCondition;
            this.params = params;
        }

        @NotNull
        public String getId() {
            return id;
        }

        @NotNull
        public String getShortName() {
            return StringUtil.notNullize(params.get("short-name"), "Terms of Service");
        }

        @NotNull
        public String getFullName() {
            return StringUtil.notNullize(params.get("full-name"), "Terms of Service");
        }

        @Nullable
        public String getText() {
            String agreementFileParam = params.get("agreement-file");
            if (agreementFileParam == null) {
                return null;
            }
            File agreementFile = new File(myConfigDir, agreementFileParam);
            try {
                return FileUtil.readText(agreementFile, "UTF-8");
            } catch (IOException e) {
                TermsOfServiceManager.LOGGER.warnAndDebugDetails("Error while reading Terms Of Service agreement file from " + agreementFile, e);
                throw new IllegalStateException("Error while reading Terms Of Service agreement file from " + agreementFile, e);
            }
        }

        @Nullable
        public String getLink() {
            if (getText() != null) return null;
            return params.get("agreement-link");
        }

        @NotNull
        public PropertyKey getUserPropertyKey() {
            return new SimplePropertyKey(".teamcity.userTermsOfServices.accepted");
        }
    }
}

