package com.solexgames.queue.runnable;

import com.solexgames.queue.QueueProxy;
import com.solexgames.queue.commons.model.server.ServerData;
import com.solexgames.queue.commons.queue.impl.child.ChildQueue;
import com.solexgames.queue.handler.QueueHandler;
import com.solexgames.xenon.redis.json.JsonAppender;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @author GrowlyX
 * @since 7/26/2021
 */

@RequiredArgsConstructor
public class QueueSendRunnable implements Runnable {

    private final QueueHandler queueHandler;

    @Override
    public void run() {
        this.queueHandler.getParentQueueMap().forEach((s, parentQueue) -> {
            final CompletableFuture<ServerData> serverDataCompletableFuture = QueueProxy.getInstance()
                    .getQueueHandler().fetchServerData(parentQueue.getTargetServer());

            serverDataCompletableFuture.whenComplete((serverData, throwable) -> {
                if (throwable != null) {
                    throwable.printStackTrace();
                }

                if (!parentQueue.getSetting("running")) {
                    return;
                }

                if (serverData.isWhitelisted()) {
                    return;
                }

                if (serverData.getOnlinePlayers() >= serverData.getMaxPlayers()) {
                    return;
                }

                final List<ChildQueue> sortedList = parentQueue.getSortedChildren();

                for (final ChildQueue childQueue : sortedList) {
                    if (!childQueue.getQueued().isEmpty()) {
                        final UUID queuePlayer = childQueue.getQueued().poll();

                        if (queuePlayer != null) {
                            QueueProxy.getInstance().getJedisManager().publish(
                                    new JsonAppender("QUEUE_SEND_PLAYER")
                                            .put("PLAYER_ID", queuePlayer.toString())
                                            .put("PARENT", parentQueue.getName())
                                            .put("CHILD", childQueue.getName())
                                            .getAsJson()
                            );
                        }

                        return;
                    }
                }
            });
        });
    }
}
