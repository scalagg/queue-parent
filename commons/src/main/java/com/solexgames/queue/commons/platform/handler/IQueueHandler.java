package com.solexgames.queue.commons.platform.handler;

import com.solexgames.queue.commons.queue.impl.ParentQueue;

import java.util.Map;

/**
 * @author GrowlyX
 * @since 7/27/2021
 */

public interface IQueueHandler {

    Map<String, ParentQueue> getParentQueueMap();

    void loadQueuesFromConfiguration();

}
