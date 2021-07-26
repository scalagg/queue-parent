package com.solexgames.queue.adapter;

import com.solexgames.queue.QueueProxy;
import com.solexgames.queue.commons.constants.QueueGlobalConstants;
import com.solexgames.queue.commons.model.impl.CachedQueuePlayer;
import com.solexgames.queue.commons.queue.impl.ParentQueue;
import com.solexgames.queue.commons.queue.impl.child.ChildQueue;
import com.solexgames.xenon.CorePlugin;
import com.solexgames.xenon.redis.annotation.Subscription;
import com.solexgames.xenon.redis.handler.JedisHandler;
import com.solexgames.xenon.redis.json.JsonAppender;

import java.util.Optional;
import java.util.UUID;

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

            QueueProxy.getInstance().getJedisManager().get((jedis, throwable) -> {
                jedis.hset(QueueGlobalConstants.JEDIS_KEY_QUEUE_CACHE, parentQueue.getName(), CorePlugin.GSON.toJson(parentQueue.getSettings()));
            });
        }
    }

    @Subscription(action = "QUEUE_ADD_PLAYER")
    public void onQueueAddPlayer(JsonAppender jsonAppender) {
        final String parentQueueName = jsonAppender.getParam("PARENT");
        final String childQueueName = jsonAppender.getParam("CHILD");

        final CachedQueuePlayer queuePlayer = CorePlugin.GSON.fromJson(jsonAppender.getParam("PLAYER"), CachedQueuePlayer.class);

        if (queuePlayer != null) {
            final ParentQueue parentQueue = QueueProxy.getInstance().getQueueHandler()
                    .getParentQueueMap().get(parentQueueName);

            if (parentQueue != null) {
                final Optional<ChildQueue> childQueueOptional = parentQueue.getChildQueue(childQueueName);

                childQueueOptional.ifPresent(childQueue -> {
                    childQueue.getQueued().add(queuePlayer);

                    QueueProxy.getInstance().getJedisManager().get((jedis, throwable) -> {
                        jedis.hset(QueueGlobalConstants.JEDIS_KEY_QUEUE_CACHE, parentQueue.getName() + ":" + childQueue.getName(), CorePlugin.GSON.toJson(childQueue.getQueued()));
                    });
                });
            }
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
                final Optional<CachedQueuePlayer> queuePlayer = childQueue.findQueuePlayerInChildQueue(uuid);

                queuePlayer.ifPresent(queuePlayer1 -> {
                    childQueue.getQueued().remove(queuePlayer1);

                    QueueProxy.getInstance().getJedisManager().get((jedis, throwable) -> {
                        jedis.hset(QueueGlobalConstants.JEDIS_KEY_QUEUE_CACHE, parentQueue.getName() + ":" + childQueue.getName(), CorePlugin.GSON.toJson(childQueue.getQueued()));
                    });
                });
            });
        }
    }
}
