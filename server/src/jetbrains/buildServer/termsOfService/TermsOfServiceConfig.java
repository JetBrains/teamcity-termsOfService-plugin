package jetbrains.buildServer.termsOfService;


public interface TermsOfServiceConfig {
  String getShortDisplayName();

  String getFullDisplayName();

  String getContentFile();

  String getPath();
}
