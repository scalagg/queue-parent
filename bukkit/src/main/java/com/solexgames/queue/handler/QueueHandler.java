package com.solexgames.queue.handler;

import com.solexgames.queue.commons.queue.impl.ParentQueue;
import com.solexgames.queue.commons.queue.impl.child.ChildQueue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
                final ChildQueue childQueue = new ChildQueue(parentQueue, this.configuration.getString(configurationPrefix + "children." + child + ".fancyName"), this.configuration.getString(configurationPrefix + "children." + child + ".permission"));
                parentQueue.getChildren().put(this.configuration.getInt(configurationPrefix + "children." + child + ".priority"), childQueue);
            });

            this.parentQueueMap.put(key, parentQueue);

            System.out.println("Loaded parent queue " + key + " with " + children.size() + " child queues.");
        });
    }

    public ChildQueue fetchBestChildQueue(ParentQueue parentQueue, Player player) {
        for (ChildQueue value : parentQueue.getChildren().descendingMap().values()) {
            if (value.getPermission() != null && player.hasPermission(value.getPermission())) {
                return value;
            }
        }

        // this should never be null
        return parentQueue.getChildren().get(0);
    }
}
