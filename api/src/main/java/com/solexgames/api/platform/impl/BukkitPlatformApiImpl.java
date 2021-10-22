package com.solexgames.api.platform.impl;

import com.solexgames.api.platform.PlatformDependantApi;
import com.solexgames.lib.commons.redis.json.JsonAppender;
import gg.scala.melon.QueueBukkit;
import gg.scala.melon.commons.model.impl.CachedQueuePlayer;
import gg.scala.melon.commons.platform.QueuePlatform;
import gg.scala.melon.commons.platform.QueuePlatforms;
import gg.scala.melon.commons.queue.impl.ParentQueue;
import gg.scala.melon.commons.queue.impl.child.ChildQueue;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @author GrowlyX
 * @since 7/27/2021
 */

public class BukkitPlatformApiImpl implements PlatformDependantApi {

    @Override
    public CompletableFuture<Void> addUserToQueue(CachedQueuePlayer cachedQueuePlayer, ParentQueue parentQueue) {
        return CompletableFuture.runAsync(() -> {
            final ChildQueue bestChildQueue = QueueBukkit.getInstance().getQueueHandler()
                    .fetchBestChildQueue(parentQueue, Bukkit.getPlayer(cachedQueuePlayer.getUniqueId()));

            this.addUserToQueue(cachedQueuePlayer, parentQueue, bestChildQueue).whenComplete((unused, throwable) -> {
                if (throwable != null) {
                    throwable.printStackTrace();
                }
            });
        });
    }

    @Override
    public CompletableFuture<Void> addUserToQueue(CachedQueuePlayer cachedQueuePlayer, ParentQueue parentQueue, ChildQueue childQueue) {
        return CompletableFuture.runAsync(() -> {
            QueueBukkit.getInstance().getJedisManager().publish(
                    new JsonAppender("QUEUE_ADD_PLAYER")
                            .put("PARENT", parentQueue.getName())
                            .put("CHILD", childQueue.getName())
                            .put("PLAYER", cachedQueuePlayer.getUniqueId().toString())
                            .getAsJson()
            );
        });
    }

    @Override
    public CompletableFuture<Void> removeUserFromQueue(CachedQueuePlayer cachedQueuePlayer) {
        return CompletableFuture.runAsync(() -> {
            final List<ParentQueue> queuedServers = QueueBukkit.getInstance().getQueueHandler()
                    .fetchPlayerQueuedIn(cachedQueuePlayer);

            queuedServers.forEach(parentQueue -> {
                this.removeUserFromQueue(cachedQueuePlayer, parentQueue).whenComplete((unused, throwable) -> {
                    if (throwable != null) {
                        throwable.printStackTrace();
                    }
                });
            });
        });
    }

    @Override
    public CompletableFuture<Void> removeUserFromQueue(CachedQueuePlayer cachedQueuePlayer, ParentQueue parentQueue) {
        return CompletableFuture.runAsync(() -> {
            QueueBukkit.getInstance().getJedisManager().publish(
                    new JsonAppender("QUEUE_REMOVE_PLAYER")
                            .put("PARENT", parentQueue.getName())
                            .put("CHILD", parentQueue.getChildQueue(cachedQueuePlayer).get().getName())
                            .put("PLAYER", cachedQueuePlayer.getUniqueId().toString())
                            .getAsJson()
            );
        });
    }

    @Override
    public CompletableFuture<Void> flushParentQueue(ParentQueue parentQueue) {
        return CompletableFuture.runAsync(() -> {
            QueueBukkit.getInstance().getJedisManager().publish(
                    new JsonAppender("QUEUE_FLUSH")
                            .put("PARENT", parentQueue.getName())
                            .getAsJson()
            );
        });
    }

    @Override
    public CachedQueuePlayer getCachedQueuePlayer(UUID uuid) {
        return QueueBukkit.getInstance().getPlayerHandler().getByUuid(uuid);
    }

    @Override
    public CachedQueuePlayer getCachedQueuePlayer(String name) {
        return QueueBukkit.getInstance().getPlayerHandler().getByName(name);
    }

    @Override
    public ParentQueue getParentQueueByName(String name) {
        return QueuePlatforms.get().getQueueHandler().getParentQueueMap().get(name);
    }

    @Override
    public List<ParentQueue> getParentQueuesByPlayer(CachedQueuePlayer cachedQueuePlayer) {
        final List<ParentQueue> parentQueues = new ArrayList<>();

        QueuePlatforms.get().getQueueHandler().getParentQueueMap().forEach((s, parentQueue) -> {
            if (parentQueue.isQueued(cachedQueuePlayer)) {
                parentQueues.add(parentQueue);
            }
        });

        return parentQueues;
    }

    @Override
    public QueuePlatform getQueuePlatform() {
        return QueueBukkit.getInstance();
    }
}
