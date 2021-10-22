package gg.scala.melon.adapter;

import gg.scala.melon.QueueProxy;
import gg.scala.melon.commons.constants.QueueGlobalConstants;
import gg.scala.melon.commons.logger.QueueLogger;
import gg.scala.melon.commons.queue.impl.ParentQueue;
import gg.scala.melon.commons.queue.impl.child.ChildQueue;
import com.solexgames.xenon.redis.annotation.Subscription;
import com.solexgames.xenon.redis.handler.JedisHandler;
import com.solexgames.xenon.redis.json.JsonAppender;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @author GrowlyX
 * @since 7/25/2021
 */

public class JedisAdapter implements JedisHandler {

    @Subscription(action = "QUEUE_DATA_UPDATE")
    public void onQueueDataUpdate(JsonAppender jsonAppender) {
        final String parentQueueName = jsonAppender.getParam("PARENT");
        final ParentQueue parentQueue = QueueProxy.getInstance().getQueueHandler()
                .getParentQueueMap().get(parentQueueName);

        if (parentQueue != null) {
            parentQueue.getSettings().put(jsonAppender.getParam("KEY"), Boolean.parseBoolean(jsonAppender.getParam("VALUE")));

            QueueLogger.log("Updated settings for " + parentQueueName);

            QueueProxy.getInstance().getJedisManager().get((jedis, throwable) -> {
                jedis.hset(QueueGlobalConstants.JEDIS_KEY_SETTING_CACHE, parentQueue.getName(), QueueGlobalConstants.GSON.toJson(parentQueue.getSettings()));
            });
        }
    }

    @Subscription(action = "QUEUE_ADD_PLAYER")
    public void onQueueAddPlayer(JsonAppender jsonAppender) {
        final String parentQueueName = jsonAppender.getParam("PARENT");
        final String childQueueName = jsonAppender.getParam("CHILD");

        final UUID uuid = UUID.fromString(jsonAppender.getParam("PLAYER"));

        final ParentQueue parentQueue = QueueProxy.getInstance().getQueueHandler()
                .getParentQueueMap().get(parentQueueName);

        if (parentQueue != null) {
            final Optional<ChildQueue> childQueueOptional = parentQueue.getChildQueue(childQueueName);

            childQueueOptional.ifPresent(childQueue -> {
                CompletableFuture.runAsync(() -> {
                    childQueue.getQueued().add(uuid);
                }).whenComplete((aBoolean, throwable) -> {
                    QueueProxy.getInstance().getJedisManager().get((jedis, throwableTwo) -> {
                        jedis.hset(QueueGlobalConstants.JEDIS_KEY_QUEUE_CACHE, parentQueue.getName() + ":" + childQueue.getName(), QueueGlobalConstants.GSON.toJson(childQueue.getQueued()));
                    });
                });
            });
        }
    }

    @Subscription(action = "QUEUE_REMOVE_PLAYER")
    public void onQueueRemovePlayer(JsonAppender jsonAppender) {
        final String parentQueueName = jsonAppender.getParam("PARENT");
        final String childQueueName = jsonAppender.getParam("CHILD");

        final UUID uuid = UUID.fromString(jsonAppender.getParam("PLAYER"));

        final ParentQueue parentQueue = QueueProxy.getInstance().getQueueHandler()
                .getParentQueueMap().get(parentQueueName);

        if (parentQueue != null) {
            final Optional<ChildQueue> childQueueOptional = parentQueue.getChildQueue(childQueueName);

            childQueueOptional.ifPresent(childQueue -> {
                CompletableFuture.runAsync(() -> {
                    childQueue.getQueued().remove(uuid);
                }).whenComplete((aBoolean, throwable) -> {
                    QueueProxy.getInstance().getJedisManager().get((jedis, throwableTwo) -> {
                        jedis.hset(QueueGlobalConstants.JEDIS_KEY_QUEUE_CACHE, parentQueue.getName() + ":" + childQueue.getName(), QueueGlobalConstants.GSON.toJson(childQueue.getQueued()));
                    });
                });
            });
        }
    }

    @Subscription(action = "QUEUE_FLUSH")
    public void onQueueFlush(JsonAppender jsonAppender) {
        final String parentQueueName = jsonAppender.getParam("PARENT");

        final ParentQueue parentQueue = QueueProxy.getInstance().getQueueHandler()
                .getParentQueueMap().get(parentQueueName);

        if (parentQueue != null) {
            CompletableFuture.runAsync(() -> {
                parentQueue.getChildren().forEach((integer, childQueue) -> {
                    childQueue.getQueued().clear();
                });
            }).whenComplete((unused1, throwable1) -> {
                QueueProxy.getInstance().getJedisManager().get((jedis, throwableTwo) -> {
                    parentQueue.getChildren().forEach((integer, childQueue) -> {
                        jedis.hset(QueueGlobalConstants.JEDIS_KEY_QUEUE_CACHE, parentQueue.getName() + ":" + childQueue.getName(), QueueGlobalConstants.GSON.toJson(childQueue.getQueued()));
                    });
                });
            });
        }
    }
}
