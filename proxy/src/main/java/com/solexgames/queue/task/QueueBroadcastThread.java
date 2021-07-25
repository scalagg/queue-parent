package com.solexgames.queue.task;

import com.solexgames.queue.QueueProxy;
import com.solexgames.queue.commons.logger.QueueLogger;
import com.solexgames.xenon.redis.JedisManager;
import com.solexgames.xenon.redis.json.JsonAppender;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

/**
 * @author GrowlyX
 * @since 7/24/2021
 */

@RequiredArgsConstructor
public class QueueBroadcastThread extends Thread {

    private static final long DELAY = 5000L;

    private final JedisManager jedisManager;

    @Override
    @SneakyThrows
    public void run() {
        while (QueueProxy.getInstance().isShouldBroadcast()) {
            this.jedisManager.publish(
                    new JsonAppender("QUEUE_BROADCAST_ALL")
                            .getAsJson()
            );

            Thread.sleep(QueueBroadcastThread.DELAY);
        }
    }
}
