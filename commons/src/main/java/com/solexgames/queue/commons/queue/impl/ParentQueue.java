package com.solexgames.queue.commons.queue.impl;

import com.solexgames.queue.commons.model.QueuePlayer;
import com.solexgames.queue.commons.queue.Queue;
import com.solexgames.queue.commons.queue.impl.child.ChildQueue;
import com.solexgames.queue.commons.queue.impl.child.impl.DefaultChildQueue;
import lombok.*;

import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;

/**
 * @author GrowlyX
 * @since 7/24/2021
 */

@Getter @Setter
@RequiredArgsConstructor
public class ParentQueue extends Queue {

    private final Map<Integer, ChildQueue> children = new HashMap<>();

    {
        this.children.put(0, new DefaultChildQueue(this));
    }

    private final String name;
    private final String fancyName;
    private final String targetServer;

    private boolean running = true;

    public Optional<ChildQueue> getChildQueue(QueuePlayer queuePlayer) {
        return this.children.values().stream()
                .filter(childQueue -> childQueue.isQueued(queuePlayer))
                .findFirst();
    }

    @Override
    public boolean isQueued(QueuePlayer queuePlayer) {
        return this.getChildQueue(queuePlayer).orElse(null) != null;
    }

    @Override
    public int getPosition(QueuePlayer queuePlayer) {
        throw new RuntimeException("ParentQueue#getPosition is not supported, please use a child queue.");
    }

    @Override
    public PriorityQueue<QueuePlayer> getQueued() {
        throw new RuntimeException("ParentQueue#getQueued is not supported, please use a child queue.");
    }
}
