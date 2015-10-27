package jetbrains.buildServer.termsOfService;

import jetbrains.buildServer.users.PropertyKey;
import jetbrains.buildServer.users.SimplePropertyKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PropertyBasedConfig implements TermsOfServiceConfig {
    @NotNull
    private final PropertyKey myKey;
    @NotNull
    private final String myContentFile;
    @NotNull
    private final String myPath;
    @NotNull
    private final String myName;

    public PropertyBasedConfig(final @NotNull String key,
                               final @NotNull String name,
                               final @NotNull String entryPoint,
                               final @NotNull String contentFile) {
        myKey = new SimplePropertyKey(key);
        myName = name;
        myContentFile = contentFile;
        myPath = entryPoint;
    }


    @NotNull
    public PropertyKey getKey() {
        return myKey;
    }

    @NotNull
    public String getContentFile() {
        return myContentFile;
    }

    @NotNull
    public String getPath() {
        return myPath;
    }

    @NotNull
    public String getName() {
        return myName;
    }

}

