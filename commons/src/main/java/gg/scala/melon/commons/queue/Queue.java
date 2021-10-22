package gg.scala.melon.commons.queue;

import gg.scala.melon.commons.model.impl.CachedQueuePlayer;

import java.util.PriorityQueue;
import java.util.UUID;

/**
 * @author GrowlyX
 * @since 7/24/2021
 */

public abstract class Queue {

    public abstract String getName();

    public abstract boolean isQueued(CachedQueuePlayer queuePlayer);
    public abstract int getPosition(CachedQueuePlayer queuePlayer);

    public abstract String getTargetServer();

    public abstract PriorityQueue<UUID> getQueued();

}
