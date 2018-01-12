package jetbrains.buildServer.termsOfService;

import jetbrains.buildServer.configuration.FilesWatcher;
import jetbrains.buildServer.serverSide.BuildServerAdapter;
import jetbrains.buildServer.serverSide.BuildServerListener;
import jetbrains.buildServer.serverSide.ServerPaths;
import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.serverSide.impl.FileWatcherFactory;
import jetbrains.buildServer.util.EventDispatcher;
import jetbrains.buildServer.util.FileUtil;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;

import javax.annotation.concurrent.ThreadSafe;
import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

@ThreadSafe
public class TermsOfServiceConfig {
    private static final String CONFIG_FILE = "settings.xml";

    private final File myConfigDir;
    private final File mySettingsFile;
    private Consumer<Element> onChangeListener;

    public TermsOfServiceConfig(@NotNull EventDispatcher<BuildServerListener> myEvents,
                                @NotNull ServerPaths serverPaths,
                                @NotNull FileWatcherFactory fileWatcherFactory) {
        myConfigDir = new File(serverPaths.getConfigDir(), "termsOfService");
        mySettingsFile = new File(myConfigDir, CONFIG_FILE);

        int watchInterval = TeamCityProperties.getInteger("teamcity.termsOfService.configWatchInterval", 10000);
        FilesWatcher filesWatcher = fileWatcherFactory.createManyFilesWatcher(
                () -> FileUtil.listFiles(myConfigDir, (dir, name) -> true),
                watchInterval);

        filesWatcher.registerListener((newFiles, modified, removed) -> loadSettings());
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
    public File getConfigFile(@NotNull String filename) {
        return FileUtil.getCanonicalFile(new File(myConfigDir, filename));
    }

    @NotNull
    public File getMainConfig() {
        return FileUtil.getCanonicalFile(getConfigFile(CONFIG_FILE));
    }

    @NotNull
    public File getConfigDir() {
        return FileUtil.getCanonicalFile(myConfigDir);
    }

    synchronized void loadSettings() {
        try {
            if (mySettingsFile.exists()) {
                Element parsed = FileUtil.parseDocument(mySettingsFile, false);
                onChangeListener.accept(parsed);
            }
        } catch (IOException | JDOMException e) {
            TermsOfServiceLogger.LOGGER.warnAndDebugDetails("Error while loading Terms Of Service settings from " + FileUtil.getCanonicalFile(mySettingsFile).getPath(), e);
        }
    }

    public void setOnChange(@NotNull Consumer<Element> listener) {
        this.onChangeListener = listener;
    }

}

