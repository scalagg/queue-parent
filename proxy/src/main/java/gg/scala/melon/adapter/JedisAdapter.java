package gg.scala.melon.adapter;

import gg.scala.banana.annotate.Subscribe;
import gg.scala.banana.message.Message;
import gg.scala.banana.subscribe.marker.BananaHandler;
import gg.scala.melon.MelonProxyPlugin;
import gg.scala.melon.commons.constants.QueueGlobalConstants;
import gg.scala.melon.commons.logger.QueueLogger;
import gg.scala.melon.commons.queue.impl.ParentQueue;
import gg.scala.melon.commons.queue.impl.child.ChildQueue;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @author GrowlyX
 * @since 7/25/2021
 */

public class JedisAdapter implements BananaHandler {

    @Subscribe("QUEUE_DATA_UPDATE")
    public void onQueueDataUpdate(Message jsonAppender) {
        final String parentQueueName = jsonAppender.get("PARENT");
        final ParentQueue parentQueue = MelonProxyPlugin.getInstance().getQueueHandler()
                .getParentQueueMap().get(parentQueueName);

        if (parentQueue != null) {
            parentQueue.getSettings().put(jsonAppender.get("KEY"), Boolean.parseBoolean(jsonAppender.get("VALUE")));

            QueueLogger.log("Updated settings for " + parentQueueName);

            MelonProxyPlugin.getInstance().getJedisManager().useResource(jedis -> {
                jedis.hset(QueueGlobalConstants.JEDIS_KEY_SETTING_CACHE, parentQueue.getName(), QueueGlobalConstants.GSON.toJson(parentQueue.getSettings()));
                return null;
            });
        }
    }

    @Subscribe("QUEUE_ADD_PLAYER")
    public void onQueueAddPlayer(Message jsonAppender) {
        final String parentQueueName = jsonAppender.get("PARENT");
        final String childQueueName = jsonAppender.get("CHILD");

        final UUID uuid = UUID.fromString(jsonAppender.get("PLAYER"));

        final ParentQueue parentQueue = MelonProxyPlugin.getInstance().getQueueHandler()
                .getParentQueueMap().get(parentQueueName);

        if (parentQueue != null) {
            final Optional<ChildQueue> childQueueOptional = parentQueue.getChildQueue(childQueueName);

            childQueueOptional.ifPresent(childQueue -> {
                CompletableFuture.runAsync(() -> {
                    childQueue.getQueued().add(uuid);
                }).whenComplete((aBoolean, throwable) -> {
                    MelonProxyPlugin.getInstance().getJedisManager().useResource(jedis -> {
                        jedis.hset(QueueGlobalConstants.JEDIS_KEY_QUEUE_CACHE, parentQueue.getName() + ":" + childQueue.getName(), QueueGlobalConstants.GSON.toJson(childQueue.getQueued()));
                        return null;
                    });
                });
            });
        }
    }

    @Subscribe("QUEUE_REMOVE_PLAYER")
    public void onQueueRemovePlayer(Message jsonAppender) {
        final String parentQueueName = jsonAppender.get("PARENT");
        final String childQueueName = jsonAppender.get("CHILD");

        final UUID uuid = UUID.fromString(jsonAppender.get("PLAYER"));

        final ParentQueue parentQueue = MelonProxyPlugin.getInstance().getQueueHandler()
                .getParentQueueMap().get(parentQueueName);

        if (parentQueue != null) {
            final Optional<ChildQueue> childQueueOptional = parentQueue.getChildQueue(childQueueName);

            childQueueOptional.ifPresent(childQueue -> {
                CompletableFuture.runAsync(() -> {
                    childQueue.getQueued().remove(uuid);
                }).whenComplete((aBoolean, throwable) -> {
                    MelonProxyPlugin.getInstance().getJedisManager().useResource(jedis -> {
                        jedis.hset(QueueGlobalConstants.JEDIS_KEY_QUEUE_CACHE, parentQueue.getName() + ":" + childQueue.getName(), QueueGlobalConstants.GSON.toJson(childQueue.getQueued()));
                        return null;
                    });
                });
            });
        }
    }

    @Subscribe("QUEUE_FLUSH")
    public void onQueueFlush(Message jsonAppender) {
        final String parentQueueName = jsonAppender.get("PARENT");

        final ParentQueue parentQueue = MelonProxyPlugin.getInstance().getQueueHandler()
                .getParentQueueMap().get(parentQueueName);

        if (parentQueue != null) {
            CompletableFuture.runAsync(() -> {
                parentQueue.getChildren().forEach((integer, childQueue) -> {
                    childQueue.getQueued().clear();
                });
            }).whenComplete((unused1, throwable1) -> {
                MelonProxyPlugin.getInstance().getJedisManager().useResource(jedis -> {
                    parentQueue.getChildren().forEach((integer, childQueue) -> {
                        jedis.hset(QueueGlobalConstants.JEDIS_KEY_QUEUE_CACHE, parentQueue.getName() + ":" + childQueue.getName(), QueueGlobalConstants.GSON.toJson(childQueue.getQueued()));
                    });
                    return null;
                });
            });
        }
    }
}
