package com.solexgames.queue.commons.model.impl;

import com.solexgames.queue.commons.model.QueuePlayer;
import com.solexgames.queue.commons.platform.QueuePlatforms;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * @author GrowlyX
 * @since 7/24/2021
 * <p>
 * Cached player-data with queue information
 */

@Getter
@RequiredArgsConstructor
public class CachedQueuePlayer extends QueuePlayer implements Comparable<CachedQueuePlayer> {

    private final String name;
    private final UUID uniqueId;

    @Setter
    private int priority;

    @Override
    public int compareTo(CachedQueuePlayer cachedQueuePlayer) {
        final boolean priorityBased = QueuePlatforms.get().getQueueHandler().shouldPrioritizePlayers();

        return priorityBased ?
                cachedQueuePlayer.getUniqueId().compareTo(this.getUniqueId()) :
                this.priority < cachedQueuePlayer.getPriority() ? -1 : 1;
    }
}
