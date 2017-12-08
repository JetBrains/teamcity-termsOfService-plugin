package jetbrains.buildServer.termsOfService;

import jetbrains.buildServer.serverSide.BuildServerAdapter;
import jetbrains.buildServer.serverSide.BuildServerListener;
import jetbrains.buildServer.serverSide.ServerPaths;
import jetbrains.buildServer.users.PropertyKey;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.users.SimplePropertyKey;
import jetbrains.buildServer.util.EventDispatcher;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.XmlUtil;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.emptyMap;

public class FileBasedConfig implements TermsOfServiceConfig {
    @NotNull
    private final String myShortName;
    @NotNull
    private final String myFullName;
    @NotNull
    private final String myPath;

    private final Map<UserCondition, Rule> myRules = new LinkedHashMap<>();

    private final File myConfigDir;

    public FileBasedConfig(final @NotNull String shortName,
                           final @NotNull String fullName,
                           final @NotNull String entryPoint,
                           final @NotNull EventDispatcher<BuildServerListener> myEvents,
                           final @NotNull ServerPaths serverPaths) {
        myConfigDir = new File(serverPaths.getConfigDir(), "termsOfService");

        myShortName = shortName;
        myFullName = fullName;
        myPath = entryPoint;

        myEvents.addListener(new BuildServerAdapter() {
            @Override
            public void serverStartup() {
                loadSettings();
            }
        });
    }

    @Override
    public synchronized String getShortDisplayName() {
        return myShortName;
    }

    @Override
    public String getFullDisplayName() {
        return myFullName;
    }

    @Nullable
    public String getAgreementText(@NotNull SUser user) {
        Optional<Rule> rule = getRule(user);
        if (rule.isPresent()) {
            File agreementFile = new File(myConfigDir, rule.get().getAgreementFileName());
            try {
                return FileUtil.readText(agreementFile, "UTF-8");
            } catch (IOException e) {
                TermsOfServiceManager.LOGGER.warnAndDebugDetails("Error while reading Terms Of Service agreement file from " + agreementFile, e);
                return null;
            }
        } else {
            TermsOfServiceManager.LOGGER.debug("No Terms of Service rule found for user " + user.describe(false));
            return null;
        }
    }

    @NotNull
    public String getPath() {
        return myPath;
    }

    @NotNull
    @Override
    public Optional<Rule> getRule(@NotNull SUser user) {
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
                            myRules.put(TermsOfServiceUtil.ANY_USER_NO_GUEST,  new Rule() {

                                @NotNull
                                @Override
                                public String getAgreementFileName() {
                                    return params.get("agreement-file");
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

