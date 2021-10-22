package gg.scala.melon.runnable;

import gg.scala.melon.QueueProxy;
import gg.scala.melon.commons.constants.QueueGlobalConstants;
import gg.scala.melon.commons.model.server.ServerData;
import gg.scala.melon.commons.queue.impl.child.ChildQueue;
import gg.scala.melon.handler.QueueHandler;
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
        CompletableFuture.runAsync(() -> {
            this.queueHandler.getParentQueueMap().forEach((s, parentQueue) -> {
                final CompletableFuture<ServerData> serverDataCompletableFuture = QueueProxy.getInstance()
                        .getQueueHandler().fetchServerData(parentQueue.getTargetServer());

                serverDataCompletableFuture.whenComplete((serverData, throwable) -> {
                    if (throwable != null) {
                        throwable.printStackTrace();
                    }

                    if (serverData == null || serverData.getLastUpdate() + QueueGlobalConstants.FIFTEEN_SECONDS < System.currentTimeMillis()) {
                        return;
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
                        if (!childQueue.getQueued().isEmpty() && parentQueue.getSettings().get("paused:" + childQueue.getName()) == null) {
                            final UUID uuid = childQueue.getQueued().poll();

                            if (uuid != null) {
                                QueueProxy.getInstance().getJedisManager().publish(
                                        new JsonAppender("QUEUE_SEND_PLAYER")
                                                .put("PLAYER_ID", uuid.toString())
                                                .put("PARENT", parentQueue.getName())
                                                .put("CHILD", childQueue.getName())
                                                .getAsJson()
                                );

                                QueueProxy.getInstance().getJedisManager().publish(
                                        new JsonAppender("QUEUE_REMOVE_PLAYER")
                                                .put("PARENT", parentQueue.getName())
                                                .put("CHILD", childQueue.getName())
                                                .put("PLAYER", uuid.toString())
                                                .getAsJson()
                                );
                            }

                            return;
                        }
                    }
                });
            });
        });
    }
}