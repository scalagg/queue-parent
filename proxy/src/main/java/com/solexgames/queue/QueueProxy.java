package com.solexgames.queue;

import com.solexgames.queue.adapter.JedisAdapter;
import com.solexgames.queue.handler.QueueHandler;
import com.solexgames.queue.task.QueueBroadcastThread;
import com.solexgames.queue.task.QueueUpdateThread;
import com.solexgames.xenon.CorePlugin;
import com.solexgames.xenon.redis.JedisBuilder;
import com.solexgames.xenon.redis.JedisManager;
import lombok.Getter;
import lombok.SneakyThrows;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;

@Getter
public final class QueueProxy extends Plugin {

    @Getter
    private static QueueProxy instance;

    private QueueHandler queueHandler;
    private JedisManager jedisManager;

    private boolean shouldBroadcast = true;

    @Override
    @SneakyThrows
    public void onEnable() {
        instance = this;

        final Configuration configuration = ConfigurationProvider.getProvider(YamlConfiguration.class)
                .load(new File(getDataFolder(), "config.yml"));

        this.queueHandler = new QueueHandler(configuration);
        this.queueHandler.loadQueuesFromConfiguration();

        this.jedisManager = new JedisBuilder()
                .withChannel("queue_global")
                .withHandler(new JedisAdapter())
                .withSettings(CorePlugin.getInstance().getJedisManager().getSettings())
                .build();

        new QueueBroadcastThread(this.jedisManager).start();

        this.queueHandler.getParentQueueMap().forEach((s, parentQueue) -> {
            new QueueUpdateThread(parentQueue).start();
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> this.shouldBroadcast = false));
    }
}
