package com.solexgames.api.platform.impl;

import com.solexgames.api.platform.PlatformDependantApi;
import com.solexgames.lib.commons.redis.json.JsonAppender;
import com.solexgames.queue.QueueBukkit;
import com.solexgames.queue.QueueProxy;
import com.solexgames.queue.commons.model.impl.CachedQueuePlayer;
import com.solexgames.queue.commons.platform.QueuePlatform;
import com.solexgames.queue.commons.platform.QueuePlatforms;
import com.solexgames.queue.commons.queue.impl.ParentQueue;
import com.solexgames.queue.commons.queue.impl.child.ChildQueue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @author GrowlyX
 * @since 7/27/2021
 */

public class BungeeCordPlatformApiImpl implements PlatformDependantApi {

    @Override
    public CompletableFuture<Void> addUserToQueue(CachedQueuePlayer cachedQueuePlayer, ParentQueue parentQueue) {
        throw new UnsupportedOperationException("BungeeCordPlatformApiImpl#addUserToQueue is not supported.");
    }

    @Override
    public CompletableFuture<Void> addUserToQueue(CachedQueuePlayer cachedQueuePlayer, ParentQueue parentQueue, ChildQueue childQueue) {
        throw new UnsupportedOperationException("BungeeCordPlatformApiImpl#addUserToQueue is not supported.");
    }

    @Override
    public CompletableFuture<Void> removeUserFromQueue(CachedQueuePlayer uuid) {
        throw new UnsupportedOperationException("BungeeCordPlatformApiImpl#removeUserFromQueue is not supported.");
    }

    @Override
    public CompletableFuture<Void> removeUserFromQueue(CachedQueuePlayer uuid, ParentQueue parentQueue) {
        throw new UnsupportedOperationException("BungeeCordPlatformApiImpl#removeUserFromQueue is not supported.");
    }

    @Override
    public CompletableFuture<Void> flushParentQueue(ParentQueue parentQueue) {
        return CompletableFuture.runAsync(() -> {
            QueueProxy.getInstance().getJedisManager().publish(
                    new JsonAppender("QUEUE_FLUSH")
                            .put("PARENT", parentQueue.getName())
                            .getAsJson()
            );
        });
    }

    @Override
    public CachedQueuePlayer getCachedQueuePlayer(UUID uuid) {
        throw new UnsupportedOperationException("BungeeCordPlatformApiImpl#getCachedQueuePlayer is not supported.");
    }

    @Override
    public CachedQueuePlayer getCachedQueuePlayer(String name) {
        throw new UnsupportedOperationException("BungeeCordPlatformApiImpl#getCachedQueuePlayer is not supported.");
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
        return QueueProxy.getInstance();
    }
}
