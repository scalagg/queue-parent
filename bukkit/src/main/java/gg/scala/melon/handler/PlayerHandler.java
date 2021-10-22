package gg.scala.melon.handler;

import gg.scala.banana.Banana;
import gg.scala.melon.commons.constants.QueueGlobalConstants;
import gg.scala.melon.commons.impl.BasicPlayerCache;
import gg.scala.melon.commons.model.impl.CachedQueuePlayer;
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

    private final Banana jedisManager;

    public CompletableFuture<CachedQueuePlayer> fetchCachedDataFromRedis(String name, UUID uniqueId) {
        return CompletableFuture.supplyAsync(() -> {
            final AtomicReference<CachedQueuePlayer> atomicReference = new AtomicReference<>();

            this.jedisManager.useResource(jedis -> {
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

                return null;
            });

            return atomicReference.get();
        });
    }

    public CompletableFuture<Void> updatePlayerDataToRedis(CachedQueuePlayer queuePlayer) {
        return CompletableFuture.runAsync(() -> {
            final String serialized = QueueGlobalConstants.GSON.toJson(queuePlayer);

            this.jedisManager.useResource(jedis -> {
                jedis.hset(QueueGlobalConstants.JEDIS_KEY_PLAYER_CACHE, queuePlayer.getUniqueId().toString(), serialized);
                return null;
            });
        });
    }
}
