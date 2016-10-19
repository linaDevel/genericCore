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

public class GenericCore {

    private static GenericCore instance;

    private final ArgParser.Args args;
    private final ConfigFile config;

    private final Reflections discoveryHelper;

    private final Map<Class<? extends GenericManager<?>>, GenericManager<?>> managers = new ConcurrentHashMap<>();
    private final ExecutorService threadPool;
    private final Map<String, Queue<?>> queueMap = new HashMap<>();

    private GenericWorker worker;

    private boolean isRunning = true;
    private boolean isReadyForShutDown = false;

    private static final Logger logger = LoggerFactory.getLogger(GenericCore.class);
    private static String configName = "config.ini";

    public GenericCore(ArgParser.Args commandArgs) throws IOException {
        args = commandArgs;

        config = new ConfigFile(
            new File(args.kwargs("config", configName))
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

    public static GenericCore instance(String[] cmdLine) {
        ArgParser.Args args = ArgParser.parse(cmdLine);

        try {
            GenericCore.instance = new GenericCore(args);
        } catch (IOException e) {
            logger.error("Unable to open config file: {}", e.getMessage());
            System.exit(1);
        }

        return GenericCore.instance;
    }

    public static GenericCore instance() {
        return instance;
    }

    public static void setConfigName(String configName) {
        GenericCore.configName = configName;
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

    public void worker(GenericWorker worker) {
        this.worker = worker;
    }

    public GenericWorker worker() {
        return worker;
    }

    public void mainLoop() {
        if (worker != null) {
            logger.info("Core started");
            execute(new ConfigWatch(config));
            worker.onInit();

            worker.run();

            logger.info("Core is going to shutdown. Waiting for remaining operations...");
            worker.onShutDown();
            managers.values().forEach(GenericManager::onShutDown);
            threadPool.shutdown();
        } else {
            logger.error("Main Worker is not set. Shutting down.");
            isRunning = false;
        }

        isReadyForShutDown = true;
        logger.info("Core is down...");
        Runtime.getRuntime().exit(0);
    }

    public boolean running() {
        return isRunning;
    }

    public boolean readyForShutDown() {
        return isReadyForShutDown;
    }

    public void shutDown() {
        isRunning = false;
    }
}
