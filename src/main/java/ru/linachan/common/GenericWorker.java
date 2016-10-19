package ru.linachan.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface GenericWorker extends Runnable {

    Logger logger = LoggerFactory.getLogger(GenericWorker.class);

    void onInit();
    void onShutDown();

}
