package ru.linachan.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.linachan.common.GenericCore;

import java.io.IOException;
import java.nio.file.*;

public class ConfigWatch implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(ConfigWatch.class);

    private ConfigFile config;

    public ConfigWatch(ConfigFile config) {
        this.config = config;
    }

    @Override
    public void run() {
        try {
            Path configDir = config.getConfigDir().toPath();
            Path configFile = config.getConfigFile().toPath();

            logger.info("Watchdog started");
            try (final WatchService watchService = FileSystems.getDefault().newWatchService()) {
                final WatchKey watchKey = configDir.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
                while (GenericCore.getInstance().isRunning()) {
                    final WatchKey wk = watchService.take();
                    for (WatchEvent<?> event : wk.pollEvents()) {
                        final Path changed = (Path) event.context();
                        if (changed.equals(configFile)) {
                            logger.info("Config file changed. Reloading...");
                            GenericCore.getInstance().config().readConfig();
                        }
                    }

                    boolean valid = wk.reset();
                    if (!valid) {
                        logger.error("Key has been unregistered");
                    }
                }
            } catch (IOException | InterruptedException e) {
                logger.error("Error[{}]: {}", e.getClass().getSimpleName(), e.getMessage());
            }
        } catch (Exception e) {
            logger.error("Unexpected error: [{}]: {}", e.getClass().getSimpleName(), e.getMessage());
        }
    }
}
