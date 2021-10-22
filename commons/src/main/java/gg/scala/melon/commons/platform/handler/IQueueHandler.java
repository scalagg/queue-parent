package gg.scala.melon.commons.platform.handler;

import gg.scala.melon.commons.queue.impl.ParentQueue;

import java.util.Map;

/**
 * @author GrowlyX
 * @since 7/27/2021
 */

public interface IQueueHandler {

    Map<String, ParentQueue> getParentQueueMap();
    Map<String, Integer> getPermissionPriorityMap();

    void loadQueuesFromConfiguration();

    boolean shouldPrioritizePlayers();

}
