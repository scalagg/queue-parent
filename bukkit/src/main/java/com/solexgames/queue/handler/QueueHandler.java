package com.solexgames.queue.handler;

import com.solexgames.core.CorePlugin;
import com.solexgames.queue.QueueBukkit;
import com.solexgames.queue.commons.constants.QueueGlobalConstants;
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
@SuppressWarnings("all")
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

            jedisValues.forEach((s, s2) -> {
                final String[] dataSplit = s.split(":");
                final ParentQueue parentQueue = this.parentQueueMap.get(dataSplit[0]);

                if (parentQueue != null) {
                    final Optional<ChildQueue> childQueueOptional = parentQueue.getChildQueue(dataSplit[1]);

                    childQueueOptional.ifPresent(childQueue -> {
                        childQueue.setQueued(CorePlugin.GSON.fromJson(s2, PriorityQueue.class));
                    });
                }
            });
        });

        QueueBukkit.getInstance().getJedisManager().runCommand(jedis -> {
            final Map<String, String> jedisValues = jedis.hgetAll(QueueGlobalConstants.JEDIS_KEY_QUEUE_CACHE);

            jedisValues.forEach((s, s2) -> {
                final ParentQueue parentQueue = this.parentQueueMap.get(s);

                if (parentQueue != null) {
                    parentQueue.setSettings(CorePlugin.GSON.fromJson(s2, Map.class));
                }
            });
        });
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
