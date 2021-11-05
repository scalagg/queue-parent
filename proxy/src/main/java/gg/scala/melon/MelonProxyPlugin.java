package gg.scala.melon;

import gg.scala.banana.Banana;
import gg.scala.banana.BananaBuilder;
import gg.scala.banana.credentials.BananaCredentials;
import gg.scala.banana.handler.impl.DefaultExceptionHandler;
import gg.scala.banana.message.Message;
import gg.scala.banana.options.BananaOptions;
import gg.scala.cocoa.CocoaProxyPlugin;
import gg.scala.melon.adapter.JedisAdapter;
import gg.scala.melon.adapter.RedisBungeeListener;
import gg.scala.melon.commons.constants.QueueGlobalConstants;
import gg.scala.melon.commons.logger.QueueLogger;
import gg.scala.melon.commons.platform.QueuePlatform;
import gg.scala.melon.commons.platform.QueuePlatforms;
import gg.scala.melon.handler.QueueHandler;
import gg.scala.melon.runnable.QueueSendRunnable;
import lombok.Getter;
import lombok.SneakyThrows;
import net.evilblock.cubed.serializers.Serializers;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Getter
public final class MelonProxyPlugin extends Plugin implements QueuePlatform {

    @Getter
    private static MelonProxyPlugin instance;

    private QueueHandler queueHandler;

    private Banana jedisManager;

    @Override
    @SneakyThrows
    public void onEnable() {
        instance = this;

        final Configuration configuration = ConfigurationProvider.getProvider(YamlConfiguration.class)
                .load(new File(this.getDataFolder(), "config.yml"));

        this.queueHandler = new QueueHandler(configuration);
        this.queueHandler.loadQueuesFromConfiguration();

        this.jedisManager = new BananaBuilder()
                .credentials(
                        new BananaCredentials(
                                CocoaProxyPlugin.getInstance().getConfig().getString("redis.address"),
                                CocoaProxyPlugin.getInstance().getConfig().getInt("redis.port"),
                                CocoaProxyPlugin.getInstance().getConfig().getBoolean("redis.authentication"),
                                CocoaProxyPlugin.getInstance().getConfig().getString("redis.password")
                        )
                )
                .options(
                        new BananaOptions(
                                "queue_global",
                                true, false, true,
                                Serializers.getGson(),
                                DefaultExceptionHandler.INSTANCE,
                                ForkJoinPool.commonPool()
                        )
                )
                .build();
        this.jedisManager.registerClass(new JedisAdapter());
        this.jedisManager.subscribe();

        QueuePlatforms.setPlatform(this);

        this.setupTaskSubscriptions();

        this.queueHandler.getParentQueueMap().forEach((s, parentQueue) -> {
            this.jedisManager.useResource(jedis -> {
                jedis.hset(QueueGlobalConstants.JEDIS_KEY_SETTING_CACHE, parentQueue.getName(), QueueGlobalConstants.GSON.toJson(parentQueue.getSettings()));
                return null;
            });

            QueueLogger.log("Setup queue by the name " + parentQueue.getName() + ".");
        });

        this.getProxy().getPluginManager().registerListener(this, new RedisBungeeListener());
    }

    private void setupTaskSubscriptions() {
        final ScheduledExecutorService broadcast = Executors.newScheduledThreadPool(1);
        final Message broadcastPacket = new Message("QUEUE_BROADCAST_ALL");

        broadcast.scheduleAtFixedRate(() -> {
            broadcastPacket.dispatch(this.jedisManager);
        }, 0L, 5L, TimeUnit.SECONDS);

        ProxyServer.getInstance().getScheduler().schedule(
                this, new QueueSendRunnable(this.queueHandler),
                0L, 250L, TimeUnit.MILLISECONDS
        );
    }
}
