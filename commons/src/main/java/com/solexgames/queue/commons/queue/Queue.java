package com.solexgames.queue.commons.queue;

import com.solexgames.queue.commons.model.QueuePlayer;

import java.util.PriorityQueue;

/**
 * @author GrowlyX
 * @since 7/24/2021
 */

public abstract class Queue {

    public abstract String getName();

    public abstract boolean isQueued(QueuePlayer queuePlayer);
    public abstract int getPosition(QueuePlayer queuePlayer);

    public abstract String getTargetServer();

    public abstract PriorityQueue<QueuePlayer> getQueued();

}
