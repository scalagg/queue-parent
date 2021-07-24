package com.solexgames.queue;

import com.solexgames.core.CorePlugin;
import com.solexgames.lib.commons.redis.JedisBuilder;
import com.solexgames.lib.commons.redis.JedisManager;
import com.solexgames.lib.processor.config.ConfigFactory;
import com.solexgames.queue.adapter.JedisAdapter;
import com.solexgames.queue.handler.PlayerHandler;
import me.lucko.helper.Events;
import me.lucko.helper.plugin.ExtendedJavaPlugin;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import lombok.Getter;

/**
 * @author GrowlyX
 * @since 7/24/2021
 */

@Getter
public final class QueueBukkit extends ExtendedJavaPlugin {

    @Getter
    private static QueueBukkit instance;

    private PlayerHandler playerHandler;

    private JedisManager jedisManager;

    @Override
    public void enable() {
        instance = this;

        this.setupJedisManager();
        this.setupTasks();
    }

    private void setupTasks() {
        Events.subscribe(AsyncPlayerPreLoginEvent.class).handler(event -> {

        });

        Events.subscribe(PlayerQuitEvent.class).handler(event -> {

        });
    }

    private void setupJedisManager() {
        this.jedisManager = new JedisBuilder()
                .withChannel("queue_global")
                .withHandler(new JedisAdapter())
                .withSettings(CorePlugin.getInstance().getDefaultJedisSettings())
                .build();

        this.playerHandler = new PlayerHandler(this.jedisManager);
    }

    @Override
    public void disable() {

    }
}
