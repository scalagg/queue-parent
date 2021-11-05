package gg.scala.melon.handler;

import gg.scala.melon.MelonProxyPlugin;
import gg.scala.melon.commons.constants.QueueGlobalConstants;
import gg.scala.melon.commons.model.server.ServerData;
import gg.scala.melon.commons.platform.handler.IQueueHandler;
import gg.scala.melon.commons.queue.impl.ParentQueue;
import gg.scala.melon.commons.queue.impl.child.ChildQueue;
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
public class QueueHandler implements IQueueHandler {

    private final Map<String, ParentQueue> parentQueueMap = new HashMap<>();

    private final Configuration configuration;

    private boolean prioritizePlayers;

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

        this.prioritizePlayers = this.configuration.getBoolean("prioritize-players-by-permission");
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

            MelonProxyPlugin.getInstance().getJedisManager().useResource(jedis -> {
                final String jedisValue = jedis.hget(QueueGlobalConstants.JEDIS_KEY_SERVER_DATA_CACHE, serverName);

                serverDataAtomicReference.set(QueueGlobalConstants.GSON.fromJson(jedisValue, ServerData.class));
            });

            return serverDataAtomicReference.get();
        });
    }

    @Override
    public boolean shouldPrioritizePlayers() {
        return this.prioritizePlayers;
    }

    @Override
    public Map<String, Integer> getPermissionPriorityMap() {
        throw new UnsupportedOperationException("IQueueHandler#getPermissionPriorityMap is not supported on BungeeCord");
    }
}
