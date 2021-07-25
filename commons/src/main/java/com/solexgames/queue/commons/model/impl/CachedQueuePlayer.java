package com.solexgames.queue.commons.model.impl;

import com.solexgames.queue.commons.model.QueuePlayer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author GrowlyX
 * @since 7/24/2021
 * <p>
 * Cached player-data with queue information
 */

@Getter
@RequiredArgsConstructor
public class CachedQueuePlayer extends QueuePlayer {

    private final String name;
    private final UUID uniqueId;

    private final Map<String, String> queueMap = new HashMap<>();

}
