package com.solexgames.queue.commons.queue.impl.child;

import com.solexgames.queue.commons.model.QueuePlayer;
import com.solexgames.queue.commons.model.impl.CachedQueuePlayer;
import com.solexgames.queue.commons.queue.Queue;
import com.solexgames.queue.commons.queue.impl.ParentQueue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.Optional;
import java.util.PriorityQueue;
import java.util.UUID;

/**
 * @author GrowlyX
 * @since 7/24/2021
 */

@Getter @Setter
@RequiredArgsConstructor
public class ChildQueue extends Queue {

    private final ParentQueue parent;

    private final String name;
    private final String fancyName;
    private final String permission;

    private PriorityQueue<CachedQueuePlayer> queued;

    {
        this.queued = new PriorityQueue<>();
    }

    public int getAllQueued() {
        return this.queued.size();
    }

    public Optional<CachedQueuePlayer> findQueuePlayerInChildQueue(UUID uuid) {
        return this.queued.stream()
                .filter(queuePlayer -> queuePlayer.getUniqueId().toString().equals(uuid.toString()))
                .findFirst();
    }

    @Override
    public boolean isQueued(CachedQueuePlayer queuePlayer) {
        return this.queued.stream()
                .filter(queuePlayer1 -> queuePlayer.getUniqueId().toString().equals(queuePlayer1.getUniqueId().toString()))
                .findFirst().orElse(null) != null;
    }

    @Override
    public String getTargetServer() {
        return this.parent.getTargetServer();
    }

    @Override
    public int getPosition(CachedQueuePlayer queuePlayer) {
        final PriorityQueue<CachedQueuePlayer> players = new PriorityQueue<>(this.queued);

        for (int i = 0; i <= this.queued.size(); i++) {
            final QueuePlayer player = players.poll();

            if (player != null && player.getUniqueId().equals(queuePlayer.getUniqueId())) {
                return i + 1;
            }
        }

        return 0;
    }
}
