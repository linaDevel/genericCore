package ru.linachan.common;

import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.linachan.common.cmd.ArgParser;
import ru.linachan.common.config.ConfigFile;
import ru.linachan.common.config.ConfigWatch;
import ru.linachan.common.utils.Queue;
import ru.linachan.common.utils.ShutdownHook;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class GenericCore {

    private static GenericCore instance;

    private final ArgParser.Args args;
    private final ConfigFile config;

    private final Reflections discoveryHelper;

    private final Map<Class<? extends GenericManager<?>>, GenericManager<?>> managers = new ConcurrentHashMap<>();
    private final ExecutorService threadPool;
    private final Map<String, Queue<?>> queueMap = new HashMap<>();

    private boolean isRunning = true;
    private boolean isReadyForShutDown = false;

    private static final Logger logger = LoggerFactory.getLogger(GenericCore.class);

    public GenericCore(ArgParser.Args commandArgs) throws IOException {
        args = commandArgs;

        config = new ConfigFile(
            new File(args.kwargs("config", "config.ini"))
        );

        discoveryHelper = new Reflections(
            ClasspathHelper.forPackage("ru.linachan"),
            new SubTypesScanner()
        );

        threadPool = Executors.newWorkStealingPool(
            Runtime.getRuntime().availableProcessors() * config.getInt("core.threads_per_cpu", 4)
        );

        Runtime.getRuntime()
            .addShutdownHook(new Thread(new ShutdownHook()));
    }

    public static GenericCore getInstance() {
        return instance;
    }

    public ConfigFile config() {
        return config;
    }

    public void execute(Runnable runnable) {
        threadPool.submit(runnable);
    }

    public <T> Set<Class<? extends T>> discover(Class<T> parentClass) {
        return discoveryHelper.getSubTypesOf(parentClass);
    }

    public <T extends GenericManager<?>> T manager(Class<T> manager) {
        if (managers.containsKey(manager)) {
            return manager.cast(managers.get(manager));
        } else {
            try {
                GenericManager<?> managerInstance = manager.newInstance();
                managers.put(manager, managerInstance);
                managerInstance.setUp();

                managerInstance.discover();

                return manager.cast(managerInstance);
            } catch (InstantiationException | IllegalAccessException e) {
                logger.error("Unable to instantiate manager", e);
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> Queue<T> queue(Class<T> type, String name) {
        if (!queueMap.containsKey(name)) {
            logger.info("Creating queue: [{}] {}", type.getSimpleName(), name);
            Queue<T> queue = new Queue<>();
            queueMap.put(name, queue);
            return queue;
        }

        return (Queue<T>) queueMap.get(name);
    }

    public void mainLoop() {
        logger.info("Core started");
        execute(new ConfigWatch(config));

        run();

        logger.info("Core is going to shutdown. Waiting for remaining operations...");
        isReadyForShutDown = true;

        managers.values().forEach(GenericManager::onShutDown);

        logger.info("Core is down...");
        Runtime.getRuntime().exit(0);
    }

    public abstract void run();

    public boolean isRunning() {
        return isRunning;
    }

    public boolean isReadyForShutDown() {
        return isReadyForShutDown;
    }

    public void shutDown() {
        isRunning = false;
    }
}
