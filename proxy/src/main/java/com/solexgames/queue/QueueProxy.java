package com.solexgames.queue;

import com.solexgames.queue.adapter.JedisAdapter;
import com.solexgames.queue.adapter.XenonJedisAdapter;
import com.solexgames.queue.commons.constants.QueueGlobalConstants;
import com.solexgames.queue.commons.logger.QueueLogger;
import com.solexgames.queue.commons.model.impl.CachedQueuePlayer;
import com.solexgames.queue.commons.platform.QueuePlatform;
import com.solexgames.queue.commons.platform.QueuePlatforms;
import com.solexgames.queue.commons.queue.impl.child.ChildQueue;
import com.solexgames.queue.handler.QueueHandler;
import com.solexgames.xenon.CorePlugin;
import com.solexgames.xenon.redis.JedisBuilder;
import com.solexgames.xenon.redis.JedisManager;
import com.solexgames.xenon.redis.json.JsonAppender;
import lombok.Getter;
import lombok.SneakyThrows;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Getter
public final class QueueProxy extends Plugin implements QueuePlatform {

    @Getter
    private static QueueProxy instance;

    private QueueHandler queueHandler;

    private JedisManager jedisManager;
    private JedisManager xenonJedisManager;

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

        this.xenonJedisManager = new JedisBuilder()
                .withChannel("scandium:bungee")
                .withHandler(new XenonJedisAdapter())
                .withSettings(CorePlugin.getInstance().getJedisManager().getSettings())
                .build();

        QueuePlatforms.setPlatform(this);

        final ScheduledExecutorService broadcast = Executors.newScheduledThreadPool(1);
        broadcast.scheduleAtFixedRate(() -> {
            if (this.shouldBroadcast) {
                this.jedisManager.publish(
                        new JsonAppender("QUEUE_BROADCAST_ALL")
                                .getAsJson()
                );
            }
        }, 0L, 5L, TimeUnit.SECONDS);

        final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(() -> {
            this.queueHandler.getParentQueueMap().forEach((s, parentQueue) -> {
                if (parentQueue.getSetting("running")) {
                    final List<ChildQueue> sortedList = parentQueue.getSortedChildren();

                    for (final ChildQueue childQueue : sortedList) {
                        if (!childQueue.getQueued().isEmpty()) {
                            final UUID queuePlayer = childQueue.getQueued().poll();

                            if (queuePlayer != null) {
                                QueueProxy.getInstance().getJedisManager().publish(
                                        new JsonAppender("QUEUE_SEND_PLAYER")
                                                .put("PLAYER_ID", queuePlayer.toString())
                                                .put("PARENT", parentQueue.getName())
                                                .put("CHILD", childQueue.getName())
                                                .getAsJson()
                                );
                            }
                        }
                    }
                }
            });
        }, 0L, 1L, TimeUnit.SECONDS);

        this.queueHandler.getParentQueueMap().forEach((s, parentQueue) -> {
            QueueProxy.getInstance().getJedisManager().get((jedis, throwable) -> {
                jedis.hset(QueueGlobalConstants.JEDIS_KEY_SETTING_CACHE, parentQueue.getName(), CorePlugin.GSON.toJson(parentQueue.getSettings()));
            });

            QueueLogger.log("Setup queue by the name " + parentQueue.getName() + ".");
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> this.shouldBroadcast = false));
    }
}
