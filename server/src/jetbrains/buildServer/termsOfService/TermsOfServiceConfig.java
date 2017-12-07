package jetbrains.buildServer.termsOfService;


import org.jetbrains.annotations.Nullable;

public interface TermsOfServiceConfig {
    String getShortDisplayName();

    String getFullDisplayName();

    /**
     * @return agreement text or null when text is not defined.
     */
    @Nullable
    String getAgreementText();

    String getPath();
}
