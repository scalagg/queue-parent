package com.solexgames.api.platform;

import com.solexgames.queue.commons.model.impl.CachedQueuePlayer;
import com.solexgames.queue.commons.platform.QueuePlatform;
import com.solexgames.queue.commons.queue.impl.ParentQueue;
import com.solexgames.queue.commons.queue.impl.child.ChildQueue;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @author GrowlyX
 * @since 7/27/2021
 */

public interface PlatformDependantApi {

    CompletableFuture<Void> addUserToQueue(CachedQueuePlayer cachedQueuePlayer, ParentQueue parentQueue);

    CompletableFuture<Void> addUserToQueue(CachedQueuePlayer cachedQueuePlayer, ParentQueue parentQueue, ChildQueue childQueue);

    CompletableFuture<Void> removeUserFromQueue(CachedQueuePlayer cachedQueuePlayer);

    CompletableFuture<Void> removeUserFromQueue(CachedQueuePlayer cachedQueuePlayer, ParentQueue parentQueue);

    CompletableFuture<Void> flushParentQueue(ParentQueue parentQueue);

    CachedQueuePlayer getCachedQueuePlayer(UUID uuid);

    CachedQueuePlayer getCachedQueuePlayer(String name);

    ParentQueue getParentQueueByName(String name);

    List<ParentQueue> getParentQueuesByPlayer(CachedQueuePlayer cachedQueuePlayer);

    QueuePlatform getQueuePlatform();

}
