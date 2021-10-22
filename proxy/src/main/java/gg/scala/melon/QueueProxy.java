package gg.scala.melon;

import gg.scala.melon.adapter.JedisAdapter;
import gg.scala.melon.adapter.XenonJedisAdapter;
import gg.scala.melon.commons.constants.QueueGlobalConstants;
import gg.scala.melon.commons.logger.QueueLogger;
import gg.scala.melon.commons.platform.QueuePlatform;
import gg.scala.melon.commons.platform.QueuePlatforms;
import gg.scala.melon.handler.QueueHandler;
import gg.scala.melon.runnable.QueueSendRunnable;
import com.solexgames.xenon.CorePlugin;
import com.solexgames.xenon.redis.JedisBuilder;
import com.solexgames.xenon.redis.JedisManager;
import com.solexgames.xenon.redis.json.JsonAppender;
import lombok.Getter;
import lombok.SneakyThrows;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
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
                .load(new File(this.getDataFolder(), "config.yml"));

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

        this.setupTaskSubscriptions();

        this.queueHandler.getParentQueueMap().forEach((s, parentQueue) -> {
            QueueProxy.getInstance().getJedisManager().get((jedis, throwable) -> {
                jedis.hset(QueueGlobalConstants.JEDIS_KEY_SETTING_CACHE, parentQueue.getName(), QueueGlobalConstants.GSON.toJson(parentQueue.getSettings()));
            });

            QueueLogger.log("Setup queue by the name " + parentQueue.getName() + ".");
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> this.shouldBroadcast = false));
    }

    private void setupTaskSubscriptions() {
        final ScheduledExecutorService broadcast = Executors.newScheduledThreadPool(1);
        broadcast.scheduleAtFixedRate(() -> {
            if (this.shouldBroadcast) {
                this.jedisManager.publish(
                        new JsonAppender("QUEUE_BROADCAST_ALL")
                                .getAsJson()
                );
            }
        }, 0L, 5L, TimeUnit.SECONDS);

        ProxyServer.getInstance().getScheduler().schedule(
                this, new QueueSendRunnable(this.queueHandler), 0L, 20L, TimeUnit.MILLISECONDS
        );
    }
}