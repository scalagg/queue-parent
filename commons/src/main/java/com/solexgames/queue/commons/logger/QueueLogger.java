package com.solexgames.queue.commons.logger;

import com.solexgames.queue.commons.platform.QueuePlatforms;

/**
 * @author GrowlyX
 * @since 7/25/2021
 */

public class QueueLogger {

    public static void log(String message) {
        QueuePlatforms.get().getLogger().info("\u001B[32m" + "[Internal] " + message);
    }
}
