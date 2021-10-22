package gg.scala.melon.commons.model.impl;

import gg.scala.melon.commons.constants.QueueGlobalConstants;
import gg.scala.melon.commons.model.QueuePlayer;
import gg.scala.melon.commons.platform.QueuePlatforms;
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

@Getter @Setter
@RequiredArgsConstructor
public class CachedQueuePlayer extends QueuePlayer implements Comparable<CachedQueuePlayer> {

    private final String name;
    private final UUID uniqueId;

    private int priority;

    private long lastQueued = -1;

    public boolean isCanQueue() {
        return this.lastQueued == -1 || System.currentTimeMillis() >= this.lastQueued + QueueGlobalConstants.TWO_SECONDS;
    }

    @Override
    public int compareTo(CachedQueuePlayer cachedQueuePlayer) {
        final boolean priorityBased = QueuePlatforms.get().getQueueHandler().shouldPrioritizePlayers();

        return priorityBased ?
                cachedQueuePlayer.getUniqueId().compareTo(this.getUniqueId()) :
                this.priority < cachedQueuePlayer.getPriority() ? -1 : 1;
    }
}
