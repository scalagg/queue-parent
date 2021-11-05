package gg.scala.melon.runnable;

import gg.scala.banana.message.Message;
import gg.scala.melon.MelonProxyPlugin;
import gg.scala.melon.commons.constants.QueueGlobalConstants;
import gg.scala.melon.commons.model.server.ServerData;
import gg.scala.melon.commons.queue.impl.child.ChildQueue;
import gg.scala.melon.handler.QueueHandler;
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
                final CompletableFuture<ServerData> serverDataCompletableFuture = MelonProxyPlugin.getInstance()
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
                                final Message message = new Message("QUEUE_SEND_PLAYER");
                                message.set("PLAYER_ID", uuid.toString());
                                message.set("PARENT", parentQueue.getName());
                                message.set("CHILD", childQueue.getName());

                                message.dispatch(
                                        MelonProxyPlugin.getInstance().getJedisManager()
                                );

                                final Message removal = new Message("QUEUE_REMOVE_PLAYER");
                                removal.set("PLAYER", uuid.toString());
                                removal.set("PARENT", parentQueue.getName());
                                removal.set("CHILD", childQueue.getName());

                                removal.dispatch(
                                        MelonProxyPlugin.getInstance().getJedisManager()
                                );
                            }

                            return;
                        }
                    }
                });
            });
        }).whenComplete((d, c) -> {
            c.printStackTrace();
        });
    }
}
