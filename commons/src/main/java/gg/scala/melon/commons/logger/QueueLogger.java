package gg.scala.melon.commons.logger;

import gg.scala.melon.commons.platform.QueuePlatforms;

/**
 * @author GrowlyX
 * @since 7/25/2021
 * <p>
 * A useful utility to log to console with colors.
 */

public class QueueLogger {

    public static void log(String message) {
        QueuePlatforms.get().getLogger().info(message);
    }
}
