package com.solexgames.queue.commons.model.impl;

import com.solexgames.queue.commons.model.QueuePlayer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.*;

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

    private final List<String> queueMap = new ArrayList<>();

    @Override
    public int compareTo(CachedQueuePlayer cachedQueuePlayer) {
        return cachedQueuePlayer.getUniqueId().compareTo(this.getUniqueId());
    }
}
