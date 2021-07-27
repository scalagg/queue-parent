package com.solexgames.queue.commons.model.impl;

import com.solexgames.queue.commons.model.QueuePlayer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/**
 * @author GrowlyX
 * @since 7/26/2021
 * <p>
 * Used for storing persistent data, currently not used
 */

@Getter
@RequiredArgsConstructor
public class PersistentQueuePlayer extends QueuePlayer {

    private final String name;
    private final UUID uniqueId;

}
