package com.solexgames.queue.handler;

import com.solexgames.queue.QueueProxy;
import com.solexgames.queue.commons.constants.QueueGlobalConstants;
import com.solexgames.queue.commons.model.server.ServerData;
import com.solexgames.queue.commons.queue.impl.ParentQueue;
import com.solexgames.queue.commons.queue.impl.child.ChildQueue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.config.Configuration;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author GrowlyX
 * @since 7/24/2021
 */

@Getter
@RequiredArgsConstructor
public class QueueHandler {

    private final Map<String, ParentQueue> parentQueueMap = new HashMap<>();

    private final Configuration configuration;

    public void loadQueuesFromConfiguration() {
        final Configuration section = this.configuration.getSection("queues");

        section.getKeys().forEach(key -> {
            final String configurationPrefix = "queues." + key + ".";
            final ParentQueue parentQueue = new ParentQueue(
                    key,
                    this.configuration.getString(configurationPrefix + "fancyName"),
                    this.configuration.getString(configurationPrefix + "serverName")
            );

            final Collection<String> children = section.getSection(key + ".children").getKeys();

            children.forEach(child -> {
                final ChildQueue childQueue = new ChildQueue(parentQueue, child, this.configuration.getString(configurationPrefix + "children." + child + ".fancyName"), this.configuration.getString(configurationPrefix + "children." + child + ".permission"));
                parentQueue.getChildren().put(this.configuration.getInt(configurationPrefix + "children." + child + ".priority"), childQueue);
            });

            this.parentQueueMap.put(key, parentQueue);
        });
    }

    public CompletableFuture<ServerData> fetchServerData(String serverName) {
        return CompletableFuture.supplyAsync(() -> {
            final AtomicReference<ServerData> serverDataAtomicReference = new AtomicReference<>();

            QueueProxy.getInstance().getJedisManager().get((jedis, throwable) -> {
                final String jedisValue = jedis.hget(QueueGlobalConstants.JEDIS_KEY_SERVER_DATA_CACHE, serverName);

                serverDataAtomicReference.set(QueueGlobalConstants.GSON.fromJson(jedisValue, ServerData.class));
            });

            return serverDataAtomicReference.get();
        });
    }
}
