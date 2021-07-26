package com.solexgames.queue.adapter;

import com.solexgames.queue.QueueProxy;
import com.solexgames.queue.commons.constants.QueueGlobalConstants;
import com.solexgames.queue.commons.logger.QueueLogger;
import com.solexgames.queue.commons.model.impl.CachedQueuePlayer;
import com.solexgames.queue.commons.queue.impl.ParentQueue;
import com.solexgames.queue.commons.queue.impl.child.ChildQueue;
import com.solexgames.xenon.CorePlugin;
import com.solexgames.xenon.redis.annotation.Subscription;
import com.solexgames.xenon.redis.handler.JedisHandler;
import com.solexgames.xenon.redis.json.JsonAppender;
import com.solexgames.xenon.redis.packet.JedisAction;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author GrowlyX
 * @since 7/25/2021
 */

public class JedisAdapter implements JedisHandler, Listener {

    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent event) {
        ProxyServer.getInstance().getScheduler().schedule(CorePlugin.getInstance(), () -> CompletableFuture.runAsync(() -> {
            if (ProxyServer.getInstance().getPlayer(event.getPlayer().getName()) == null) {
                QueueProxy.getInstance().getQueueHandler().getParentQueueMap().forEach((s, parentQueue) -> {
                    parentQueue.getChildQueue(event.getPlayer().getUniqueId()).ifPresent(childQueue ->
                            CorePlugin.getInstance().getJedisManager().publish(
                                    new JsonAppender("QUEUE_REMOVE_PLAYER")
                                            .put("PARENT", parentQueue.getName())
                                            .put("CHILD", childQueue.getName())
                                            .put("PLAYER", event.getPlayer().getUniqueId().toString())
                                            .getAsJson()
                            )
                    );
                });
            }
        }), 1L, TimeUnit.SECONDS);
    }

    @Subscription(action = "QUEUE_DATA_UPDATE")
    public void onQueueDataUpdate(JsonAppender jsonAppender) {
        final String parentQueueName = jsonAppender.getParam("PARENT");
        final ParentQueue parentQueue = QueueProxy.getInstance().getQueueHandler()
                .getParentQueueMap().get(parentQueueName);

        if (parentQueue != null) {
            parentQueue.getSettings().put(jsonAppender.getParam("KEY"), Boolean.parseBoolean(jsonAppender.getParam("VALUE")));

            QueueLogger.log("Updated settings for " + parentQueueName);

            QueueProxy.getInstance().getJedisManager().get((jedis, throwable) -> {
                jedis.hset(QueueGlobalConstants.JEDIS_KEY_SETTING_CACHE, parentQueue.getName(), CorePlugin.GSON.toJson(parentQueue.getSettings()));
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
