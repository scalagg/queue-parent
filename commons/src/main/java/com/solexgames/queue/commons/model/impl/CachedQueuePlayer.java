package com.solexgames.queue.commons.model.impl;

import com.solexgames.queue.commons.model.QueuePlayer;
import com.solexgames.queue.commons.queue.impl.ParentQueue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author GrowlyX
 * @since 7/24/2021
 */

@Getter
@RequiredArgsConstructor
public class CachedQueuePlayer extends QueuePlayer {

    private final String name;
    private final UUID uniqueId;

    private final Map<String, ParentQueue> queueMap = new HashMap<>();

}
