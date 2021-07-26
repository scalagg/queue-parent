package com.solexgames.queue.commons.platform;

import lombok.Setter;

/**
 * @author GrowlyX
 * @since 7/25/2021
 */

public class QueuePlatforms {

    @Setter
    private static QueuePlatform platform;

    public static QueuePlatform get() {
        return platform;
    }
}
