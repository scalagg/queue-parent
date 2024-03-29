package gg.scala.melon.commons.queue.impl;

import gg.scala.melon.commons.model.impl.CachedQueuePlayer;
import gg.scala.melon.commons.queue.Queue;
import gg.scala.melon.commons.queue.impl.child.ChildQueue;
import gg.scala.melon.commons.queue.impl.child.impl.DefaultChildQueue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.*;
import java.util.stream.Collectors;

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

    private Map<String, Boolean> settings = new HashMap<>();

    {
        this.settings.put("running", true);
    }

    public boolean getSetting(String key) {
        return this.settings.getOrDefault(key, true);
    }

    public List<ChildQueue> getSortedChildren() {
        final Set<Map.Entry<Integer, ChildQueue>> childQueues = new HashSet<>(this.children.entrySet());

        return childQueues.stream()
                .sorted(Comparator.comparingInt(Map.Entry::getKey))
                .map(Map.Entry::getValue).collect(Collectors.toList());
    }

    public Optional<ChildQueue> getChildQueue(String name) {
        final List<ChildQueue> childQueues = new ArrayList<>(this.children.values());

        return childQueues.stream()
                .filter(childQueue -> childQueue.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    public Optional<ChildQueue> getChildQueue(UUID uuid) {
        final List<ChildQueue> childQueues = new ArrayList<>(this.children.values());

        return childQueues.stream()
                .filter(childQueue -> childQueue.findQueuePlayerInChildQueue(uuid).isPresent())
                .findFirst();
    }

    public Optional<ChildQueue> getChildQueue(CachedQueuePlayer queuePlayer) {
        final List<ChildQueue> childQueues = new ArrayList<>(this.children.values());

        return childQueues.stream()
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
        throw new UnsupportedOperationException("ParentQueue#getPosition is not supported, please use a child queue.");
    }

    @Override
    @Deprecated
    public PriorityQueue<UUID> getQueued() {
        throw new UnsupportedOperationException("ParentQueue#getQueued is not supported, please use a child queue.");
    }
}
