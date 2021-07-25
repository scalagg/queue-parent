package com.solexgames.queue.handler;

import com.solexgames.queue.commons.queue.impl.ParentQueue;
import com.solexgames.queue.commons.queue.impl.child.ChildQueue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.config.Configuration;

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
                final ChildQueue childQueue = new ChildQueue(parentQueue, this.configuration.getString(configurationPrefix + "children." + child + ".fancyName"), this.configuration.getString(configurationPrefix + "children." + child + ".permission"));
                parentQueue.getChildren().put(this.configuration.getInt(configurationPrefix + "children." + child + ".priority"), childQueue);
            });

            this.parentQueueMap.put(key, parentQueue);

            System.out.println("Loaded parent queue " + key + " with " + children.size() + " child queues.");
        });
    }
}
