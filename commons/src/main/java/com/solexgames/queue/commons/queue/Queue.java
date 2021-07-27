package com.solexgames.queue.commons.queue;

import com.solexgames.queue.commons.model.QueuePlayer;
import com.solexgames.queue.commons.model.impl.CachedQueuePlayer;
import com.solexgames.queue.commons.queue.impl.child.ChildQueue;

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
