package jetbrains.buildServer.termsOfService;

import jetbrains.buildServer.serverSide.ServerPaths;
import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.users.PropertyKey;
import jetbrains.buildServer.users.SimplePropertyKey;
import jetbrains.buildServer.util.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

public class PropertyBasedConfig implements TermsOfServiceConfig {
    @NotNull
    private final PropertyKey myKey;
    @NotNull
    private final String myShortName;
    @NotNull
    private final String myFullName;
    @NotNull
    private final String myAgreementFileName;
    @NotNull
    private final String myPath;

    @NotNull
    private final ServerPaths myServerPaths;

    public PropertyBasedConfig(final @NotNull String key,
                               final @NotNull String shortName,
                               final @NotNull String fullName,
                               final @NotNull String entryPoint,
                               final @NotNull String contentFileName,
                               final @NotNull ServerPaths serverPaths) {
        myKey = new SimplePropertyKey(key);
        myShortName = shortName;
        myFullName = fullName;
        myAgreementFileName = contentFileName;
        myPath = entryPoint;
        myServerPaths = serverPaths;
    }


    @NotNull
    public PropertyKey getKey() {
        return myKey;
    }

    @Override
    public String getShortDisplayName() {
        return myShortName;
    }

    @Override
    public String getFullDisplayName() {
        return myFullName;
    }

    @Nullable
    public String getAgreementText() {
        File termsOfServiceConfigDir = new File(myServerPaths.getConfigDir(), "termsOfService");
        String customLocation = TeamCityProperties.getPropertyOrNull("teamcity.termsOfService.agreementPath");
        File agreementFile;
        if (customLocation == null) {
            agreementFile = new File(termsOfServiceConfigDir, myAgreementFileName);
        } else {
            agreementFile = new File(customLocation, myAgreementFileName);
        }
        try {
            return FileUtil.readText(agreementFile, "UTF-8");
        } catch (IOException e) {
            TermsOfServiceManager.LOGGER.warnAndDebugDetails("Error while reading Terms Of Service agreement file from " + agreementFile, e);
            return null;
        }
    }

    @NotNull
    public String getPath() {
        return myPath;
    }


}

