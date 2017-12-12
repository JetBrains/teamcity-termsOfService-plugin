package jetbrains.buildServer.termsOfService;

import jetbrains.buildServer.serverSide.BuildServerAdapter;
import jetbrains.buildServer.serverSide.BuildServerListener;
import jetbrains.buildServer.serverSide.ServerPaths;
import jetbrains.buildServer.serverSide.auth.Permission;
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

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.emptyMap;

public class TermsOfServiceConfig {
    private final Map<UserCondition, TermsOfServiceManager.Agreement> myRules = new LinkedHashMap<>();

    private final File myConfigDir;

    public TermsOfServiceConfig(
                           final @NotNull EventDispatcher<BuildServerListener> myEvents,
                           final @NotNull ServerPaths serverPaths) {
        myConfigDir = new File(serverPaths.getConfigDir(), "termsOfService");

        myEvents.addListener(new BuildServerAdapter() {
            @Override
            public void serverStartup() {
                loadSettings();
            }
        });
    }

    @NotNull
    public Optional<TermsOfServiceManager.Agreement> getRule(@NotNull SUser user) {
        return myRules.entrySet().stream().filter(e -> e.getKey().shouldAccept(user)).findFirst().map(Map.Entry::getValue);
    }

    private void loadSettings() {
        File settingsFile = new File(myConfigDir, "terms-of-service-config.xml");
        try {
            if (settingsFile.exists()) {

                Element parsed = FileUtil.parseDocument(settingsFile, false);
                for (final Object child : parsed.getChildren("rule")) {
                    if ("ALL_USERS".equals(((Element) child).getAttributeValue("type"))) {
                        Element paramsElement = ((Element) child).getChild("parameters");
                        Map<String, String> params = paramsElement == null ? emptyMap() : XmlUtil.readParameters(paramsElement);
                        if (params.get("agreement-file") != null) {
                            myRules.put(user -> user.isPermissionGrantedForAnyProject(Permission.CHANGE_OWN_PROFILE),  new TermsOfServiceManager.Agreement() {

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
                                public String getText() {
                                    File agreementFile = new File(myConfigDir, params.get("agreement-file"));
                                    try {
                                        return FileUtil.readText(agreementFile, "UTF-8");
                                    } catch (IOException e) {
                                        TermsOfServiceManager.LOGGER.warnAndDebugDetails("Error while reading Terms Of Service agreement file from " + agreementFile, e);
                                        throw new IllegalStateException("Error while reading Terms Of Service agreement file from " + agreementFile, e);
                                    }
                                }

                                @NotNull
                                @Override
                                public PropertyKey getUserPropertyKey() {
                                    return new SimplePropertyKey(".teamcity.userTermsOfServices.accepted");
                                }
                            });
                            break;
                        }
                    }
                }
            }

            if (myRules.isEmpty()) {
                TermsOfServiceManager.LOGGER.warn("No Terms of Service rules were found in " + FileUtil.getCanonicalFile(settingsFile).getPath());
            }
        } catch (IOException|JDOMException e) {
            TermsOfServiceManager.LOGGER.warnAndDebugDetails("Error while loading Terms Of Service settings from " + FileUtil.getCanonicalFile(settingsFile).getPath(), e);
        }
    }

}

