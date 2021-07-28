package com.solexgames.queue.adapter;

import com.solexgames.queue.QueueProxy;
import com.solexgames.queue.commons.constants.QueueGlobalConstants;
import com.solexgames.queue.commons.logger.QueueLogger;
import com.solexgames.queue.commons.platform.QueuePlatforms;
import com.solexgames.queue.commons.queue.impl.child.ChildQueue;
import com.solexgames.xenon.CorePlugin;
import com.solexgames.xenon.redis.annotation.Subscription;
import com.solexgames.xenon.redis.handler.JedisHandler;
import com.solexgames.xenon.redis.json.JsonAppender;

import java.util.Optional;
import java.util.UUID;

/**
 * @author GrowlyX
 * @since 7/26/2021
 */

public class XenonJedisAdapter implements JedisHandler {

    @Subscription(action = "GLOBAL_DISCONNECT")
    public void onGlobalDisconnect(JsonAppender jsonAppender) {
        final UUID uuid = UUID.fromString(jsonAppender.getParam("UUID"));

        QueuePlatforms.get().getQueueHandler().getParentQueueMap().forEach((s, parentQueue) -> {
            final Optional<ChildQueue> optionalChildQueue = parentQueue.getChildQueue(uuid);

            optionalChildQueue.ifPresent(childQueue -> {
                CorePlugin.getInstance().getJedisManager().publish(
                        new JsonAppender("QUEUE_REMOVE_PLAYER")
                                .put("PARENT", parentQueue.getName())
                                .put("CHILD", childQueue.getName())
                                .put("PLAYER", uuid.toString())
                                .getAsJson()
                );

                QueueProxy.getInstance().getJedisManager().get((jedis, throwable) -> {
                    jedis.hset(QueueGlobalConstants.JEDIS_KEY_SETTING_CACHE, parentQueue.getName(), QueueGlobalConstants.GSON.toJson(parentQueue.getSettings()));
                });

                QueueLogger.log("Found player and sent remove queue packet");
            });
        });

        QueueLogger.log("Received global disconnect packet");
    }
}
