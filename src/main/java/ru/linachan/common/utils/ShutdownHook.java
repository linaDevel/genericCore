package ru.linachan.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.linachan.common.GenericCore;

public class ShutdownHook implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ShutdownHook.class);

    @Override
    public void run() {
        GenericCore.instance().shutDown();

        while (!GenericCore.instance().readyForShutDown()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.error("Unable to shutDown properly", e);
            }
        }
    }
}
