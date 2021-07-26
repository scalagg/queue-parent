package com.solexgames.queue.task;

import com.solexgames.queue.QueueProxy;
import com.solexgames.queue.commons.logger.QueueLogger;
import com.solexgames.queue.commons.model.impl.CachedQueuePlayer;
import com.solexgames.queue.commons.queue.impl.ParentQueue;
import com.solexgames.queue.commons.queue.impl.child.ChildQueue;
import com.solexgames.xenon.redis.json.JsonAppender;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;

import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author GrowlyX
 * @since 7/24/2021
 */

@RequiredArgsConstructor
public class QueueUpdateThread extends Thread {

    private static final long DELAY = 500L;

    private final ParentQueue parentQueue;

    @Override
    @SneakyThrows
    public void run() {
        while (this.parentQueue.getSetting("running")) {
            final List<ChildQueue> sortedList = this.parentQueue.getSortedChildren();

            for (final ChildQueue childQueue : sortedList) {
                if (!childQueue.getQueued().isEmpty()) {
                    final CachedQueuePlayer queuePlayer = childQueue.getQueued().poll();

                    if (queuePlayer != null) {
                        QueueProxy.getInstance().getJedisManager().publish(
                                new JsonAppender("QUEUE_SEND_PLAYER")
                                        .put("PLAYER_ID", queuePlayer.getUniqueId().toString())
                                        .put("PARENT", this.parentQueue.getName())
                                        .put("CHILD", childQueue.getName())
                                        .getAsJson()
                        );
                    }
                }
            }

            Thread.sleep(QueueUpdateThread.DELAY);
        }
    }
}
