package com.solexgames.queue.handler;

import com.solexgames.lib.commons.game.impl.BasicPlayerCache;
import com.solexgames.lib.commons.redis.JedisManager;
import com.solexgames.queue.commons.constants.QueueGlobalConstants;
import com.solexgames.queue.commons.model.impl.CachedQueuePlayer;
import lombok.RequiredArgsConstructor;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author GrowlyX
 * @since 7/24/2021
 */

@RequiredArgsConstructor
public class PlayerHandler extends BasicPlayerCache<CachedQueuePlayer> {

    private final JedisManager jedisManager;

    public CompletableFuture<CachedQueuePlayer> fetchCachedDataFromRedis(String name, UUID uniqueId) {
        return CompletableFuture.supplyAsync(() -> {
            final AtomicReference<CachedQueuePlayer> atomicReference = new AtomicReference<>();

            this.jedisManager.runCommand(jedis -> {
                final String jedisValue = jedis.hget(QueueGlobalConstants.JEDIS_KEY_PLAYER_CACHE, uniqueId.toString());

                if (jedisValue != null) {
                    final CachedQueuePlayer queuePlayer = QueueGlobalConstants.GSON.fromJson(jedisValue, CachedQueuePlayer.class);

                    if (queuePlayer != null) {
                        atomicReference.set(queuePlayer);
                    }
                } else {
                    final CachedQueuePlayer newQueuePlayer = new CachedQueuePlayer(name, uniqueId);
                    atomicReference.set(newQueuePlayer);
                }
            });

            return atomicReference.get();
        });
    }

    public CompletableFuture<Void> updatePlayerDataToRedis(CachedQueuePlayer queuePlayer) {
        return CompletableFuture.runAsync(() -> {
            final String serialized = QueueGlobalConstants.GSON.toJson(queuePlayer);

            this.jedisManager.runCommand(jedis -> {
                jedis.hset(QueueGlobalConstants.JEDIS_KEY_PLAYER_CACHE, queuePlayer.getUniqueId().toString(), serialized);
            });
        });
    }
}
