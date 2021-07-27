package com.solexgames.queue.handler;

import com.solexgames.queue.QueueBukkit;
import com.solexgames.queue.commons.constants.QueueGlobalConstants;
import com.solexgames.queue.commons.logger.QueueLogger;
import com.solexgames.queue.commons.model.impl.CachedQueuePlayer;
import com.solexgames.queue.commons.model.server.ServerData;
import com.solexgames.queue.commons.platform.handler.IQueueHandler;
import com.solexgames.queue.commons.queue.impl.ParentQueue;
import com.solexgames.queue.commons.queue.impl.child.ChildQueue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author GrowlyX
 * @since 7/24/2021
 */

@Getter
@RequiredArgsConstructor
public class QueueHandler implements IQueueHandler {

    private final Map<String, ParentQueue> parentQueueMap = new HashMap<>();

    private final Configuration configuration;

    public void loadQueuesFromConfiguration() {
        final ConfigurationSection section = this.configuration.getConfigurationSection("queues");

        section.getKeys(false).forEach(key -> {
            final String configurationPrefix = "queues." + key + ".";
            final ParentQueue parentQueue = new ParentQueue(
                    key,
                    this.configuration.getString(configurationPrefix + "fancyName"),
                    this.configuration.getString(configurationPrefix + "serverName")
            );

            final Set<String> children = section.getConfigurationSection(key + ".children").getKeys(false);

            children.forEach(child -> {
                final ChildQueue childQueue = new ChildQueue(parentQueue, child, this.configuration.getString(configurationPrefix + "children." + child + ".fancyName"), this.configuration.getString(configurationPrefix + "children." + child + ".permission"));
                parentQueue.getChildren().put(this.configuration.getInt(configurationPrefix + "children." + child + ".priority"), childQueue);
            });

            this.parentQueueMap.put(key, parentQueue);

            QueueLogger.log("Loaded parent queue " + key + " with " + children.size() + " child queues.");
        });

        QueueBukkit.getInstance().getJedisManager().runCommand(jedis -> {
            final Map<String, String> jedisValues = jedis.hgetAll(QueueGlobalConstants.JEDIS_KEY_QUEUE_CACHE);

            if (jedisValues != null) {
                jedisValues.forEach((s, s2) -> {
                    final String[] dataSplit = s.split(":");

                    if (dataSplit.length == 2) {
                        final ParentQueue parentQueue = this.parentQueueMap.get(dataSplit[0]);

                        if (parentQueue != null) {
                            final Optional<ChildQueue> childQueueOptional = parentQueue.getChildQueue(dataSplit[1]);

                            childQueueOptional.ifPresent(childQueue -> {
                                childQueue.setQueued(QueueGlobalConstants.GSON.fromJson(s2, QueueGlobalConstants.PRIORITY_QUEUE_PLAYER_TYPE));
                            });
                        }
                    } else {
                        QueueLogger.log("Something went wrong while trying to update a parent queue from jedis! " + s + " - " + s2);
                    }
                });
            }
        });

        QueueBukkit.getInstance().getJedisManager().runCommand(jedis -> {
            final Map<String, String> jedisValues = jedis.hgetAll(QueueGlobalConstants.JEDIS_KEY_QUEUE_CACHE);

            if (jedisValues != null) {
                jedisValues.forEach((s, s2) -> {
                    final ParentQueue parentQueue = this.parentQueueMap.get(s);

                    if (parentQueue != null) {
                        parentQueue.setSettings(QueueGlobalConstants.GSON.fromJson(s2, QueueGlobalConstants.STRING_BOOLEAN_MAP_TYPE));
                    }
                });
            }
        });
    }

    /**
     * Retrieves server data for a specific server
     * from jedis and returns it in an object form.
     *
     * @param serverName The server to get data of
     *
     * @return a {@link ServerData} object
     * @see ServerData
     */
    public CompletableFuture<ServerData> fetchServerData(String serverName) {
        return CompletableFuture.supplyAsync(() -> {
            final AtomicReference<ServerData> serverDataAtomicReference = new AtomicReference<>();

            QueueBukkit.getInstance().getJedisManager().runCommand(jedis -> {
                final String jedisValue = jedis.hget(QueueGlobalConstants.JEDIS_KEY_SERVER_DATA_CACHE, serverName);

                serverDataAtomicReference.set(QueueGlobalConstants.GSON.fromJson(jedisValue, ServerData.class));
            });

            return serverDataAtomicReference.get();
        });
    }

    /**
     * Retrieves server data for all servers
     * from jedis and returns it in an object form.
     *
     * @return a completable future of a map of {@link ServerData} objects and their id
     */
    public CompletableFuture<Map<String, ServerData>> fetchAllServerData() {
        return CompletableFuture.supplyAsync(() -> {
            final AtomicReference<Map<String, ServerData>> serverDataAtomicReference = new AtomicReference<>();

            QueueBukkit.getInstance().getJedisManager().runCommand(jedis -> {
                final Map<String, String> jedisValue = jedis.hgetAll(QueueGlobalConstants.JEDIS_KEY_SERVER_DATA_CACHE);
                final Map<String, ServerData> deserialized = new HashMap<>();

                jedisValue.forEach((s, s2) -> {
                    deserialized.put(s, QueueGlobalConstants.GSON.fromJson(s2, ServerData.class));
                });

                serverDataAtomicReference.set(deserialized);
            });

            return serverDataAtomicReference.get();
        });
    }

    /**
     * Finds all queues that a player's queued
     * in and returns it in a list.
     *
     * @param queuePlayer a {@link CachedQueuePlayer} object
     *
     * @return All available queues the specified player is queued in
     */
    public List<ParentQueue> fetchPlayerQueuedIn(CachedQueuePlayer queuePlayer) {
        final List<ParentQueue> parentQueueList = new ArrayList<>();

        for (final Map.Entry<String, ParentQueue> stringParentQueueEntry : this.getParentQueueMap().entrySet()) {
            final ParentQueue parentQueue = stringParentQueueEntry.getValue();

            if (stringParentQueueEntry.getValue().isQueued(queuePlayer)) {
                parentQueueList.add(parentQueue);
            }
        }

        return parentQueueList;
    }

    /**
     * Calculates the best possible child queue for a
     * player based on what permissions they have.
     *
     * @param parentQueue The parent queue used to get the child queue from
     * @param player Player to get best child queue for
     *
     * @return a {@link ChildQueue} object
     */
    public ChildQueue fetchBestChildQueue(ParentQueue parentQueue, Player player) {
        for (final ChildQueue value : parentQueue.getSortedChildren()) {
            if (value.getPermission() != null && player.hasPermission(value.getPermission())) {
                return value;
            }
        }

        // this should never be null, if it is, something is very very very wrong
        return parentQueue.getChildren().get(0);
    }
}
