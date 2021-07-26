package com.solexgames.queue.internal;

import com.solexgames.lib.processor.config.comment.Comment;
import lombok.Data;

/**
 * @author GrowlyX
 * @since 7/26/2021
 */

@Data
public class QueueBukkitSettings {

    @Comment("Should we allow players to join multiple queues at a time?")
    private final boolean allowMultipleQueues = true;

}
