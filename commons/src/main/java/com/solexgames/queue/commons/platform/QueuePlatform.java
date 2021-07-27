package com.solexgames.queue.commons.platform;

import com.solexgames.queue.commons.platform.handler.IQueueHandler;

import java.util.logging.Logger;

/**
 * @author GrowlyX
 * @since 7/25/2021
 */

public interface QueuePlatform {

    Logger getLogger();

    IQueueHandler getQueueHandler();

}
