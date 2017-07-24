package jetbrains.buildServer.termsOfService;

import jetbrains.buildServer.users.PropertyKey;
import jetbrains.buildServer.users.SimplePropertyKey;
import org.jetbrains.annotations.NotNull;

public class PropertyBasedConfig implements TermsOfServiceConfig {
    @NotNull
    private final PropertyKey myKey;
    @NotNull
    private final String myShortName;
    @NotNull
    private final String myFullName;
    @NotNull
    private final String myContentFile;
    @NotNull
    private final String myPath;

    public PropertyBasedConfig(final @NotNull String key,
                               final @NotNull String shortName,
                               final @NotNull String fullName,
                               final @NotNull String entryPoint,
                               final @NotNull String contentFile) {
        myKey = new SimplePropertyKey(key);
        myShortName = shortName;
        myFullName = fullName;

        myContentFile = contentFile;
        myPath = entryPoint;
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

    @NotNull
    public String getContentFile() {
        return myContentFile;
    }

    @NotNull
    public String getPath() {
        return myPath;
    }


}

