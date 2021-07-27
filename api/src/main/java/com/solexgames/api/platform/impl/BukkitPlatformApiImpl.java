package com.solexgames.api.platform.impl;

import com.solexgames.api.platform.PlatformDependantApi;
import com.solexgames.lib.commons.redis.json.JsonAppender;
import com.solexgames.queue.QueueBukkit;
import com.solexgames.queue.commons.model.impl.CachedQueuePlayer;
import com.solexgames.queue.commons.platform.QueuePlatform;
import com.solexgames.queue.commons.platform.QueuePlatforms;
import com.solexgames.queue.commons.queue.impl.ParentQueue;
import com.solexgames.queue.commons.queue.impl.child.ChildQueue;
import org.bukkit.Bukkit;

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
        return QueueBukkit.getInstance().getQueueHandler().fetchPlayerQueuedIn(cachedQueuePlayer);
    }

    @Override
    public QueuePlatform getQueuePlatform() {
        return QueueBukkit.getInstance();
    }
}
