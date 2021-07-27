package com.solexgames.queue.handler;

import com.solexgames.core.CorePlugin;
import com.solexgames.queue.QueueBukkit;
import com.solexgames.queue.commons.constants.QueueGlobalConstants;
import com.solexgames.queue.commons.logger.QueueLogger;
import com.solexgames.queue.commons.model.impl.CachedQueuePlayer;
import com.solexgames.queue.commons.queue.impl.ParentQueue;
import com.solexgames.queue.commons.queue.impl.child.ChildQueue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;

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

            System.out.println("Loaded parent queue " + key + " with " + children.size() + " child queues.");
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
                                childQueue.setQueued(CorePlugin.GSON.fromJson(s2, QueueGlobalConstants.CACHED_QUEUE_PLAYER_TYPE));
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
                        parentQueue.setSettings(CorePlugin.GSON.fromJson(s2, QueueGlobalConstants.STRING_BOOLEAN_MAP_TYPE));
                    }
                });
            }
        });
    }

    public List<ParentQueue> fetchPlayerQueuedIn(CachedQueuePlayer queuePlayer) {
        final List<ParentQueue> parentQueueList = new ArrayList<>();

        for (Map.Entry<String, ParentQueue> stringParentQueueEntry : this.getParentQueueMap().entrySet()) {
            final ParentQueue parentQueue = stringParentQueueEntry.getValue();

            if (stringParentQueueEntry.getValue().isQueued(queuePlayer)) {
                parentQueueList.add(parentQueue);
            }
        }

        return parentQueueList;
    }

    public ChildQueue fetchBestChildQueue(ParentQueue parentQueue, Player player) {
        for (ChildQueue value : parentQueue.getSortedChildren()) {
            if (value.getPermission() != null && player.hasPermission(value.getPermission())) {
                return value;
            }
        }

        // this should never be null, if it is, something is very very wrong
        return parentQueue.getChildren().get(0);
    }
}
