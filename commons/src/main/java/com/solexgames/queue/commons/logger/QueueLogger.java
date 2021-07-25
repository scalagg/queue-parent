package com.solexgames.queue.commons.logger;

/**
 * @author GrowlyX
 * @since 7/25/2021
 */

public class QueueLogger {

    public static void log(String message) {
        System.out.println("\u001B[32m" + "[Queue] " + message);
    }
}
