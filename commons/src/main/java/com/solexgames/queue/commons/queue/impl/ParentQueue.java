package com.solexgames.queue.commons.queue.impl;

import com.solexgames.queue.commons.model.QueuePlayer;
import com.solexgames.queue.commons.model.impl.CachedQueuePlayer;
import com.solexgames.queue.commons.queue.Queue;
import com.solexgames.queue.commons.queue.impl.child.ChildQueue;
import com.solexgames.queue.commons.queue.impl.child.impl.DefaultChildQueue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.Optional;
import java.util.PriorityQueue;
import java.util.TreeMap;

/**
 * @author GrowlyX
 * @since 7/24/2021
 */

@Getter @Setter
@RequiredArgsConstructor
public class ParentQueue extends Queue {

    private final TreeMap<Integer, ChildQueue> children = new TreeMap<>();

    {
        this.children.put(0, new DefaultChildQueue(this));
    }

    private final String name;
    private final String fancyName;
    private final String targetServer;

    private boolean running = true;

    public Optional<ChildQueue> getChildQueue(String name) {
        return this.children.values().stream()
                .filter(childQueue -> childQueue.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    public Optional<ChildQueue> getChildQueue(CachedQueuePlayer queuePlayer) {
        return this.children.values().stream()
                .filter(childQueue -> childQueue.isQueued(queuePlayer))
                .findFirst();
    }

    public int getAllQueued() {
        return this.children.values().stream()
                .mapToInt(childQueue -> childQueue.getQueued().size()).sum();
    }

    @Override
    public boolean isQueued(CachedQueuePlayer queuePlayer) {
        return this.getChildQueue(queuePlayer).orElse(null) != null;
    }

    @Override
    @Deprecated
    public int getPosition(CachedQueuePlayer queuePlayer) {
        throw new RuntimeException("ParentQueue#getPosition is not supported, please use a child queue.");
    }

    @Override
    @Deprecated
    public PriorityQueue<CachedQueuePlayer> getQueued() {
        throw new RuntimeException("ParentQueue#getQueued is not supported, please use a child queue.");
    }
}