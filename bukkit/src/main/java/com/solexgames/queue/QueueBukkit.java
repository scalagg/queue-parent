package com.solexgames.queue;

import com.solexgames.lib.acf.InvalidCommandArgument;
import com.solexgames.lib.acf.PaperCommandManager;
import com.solexgames.lib.commons.redis.JedisBuilder;
import com.solexgames.lib.commons.redis.JedisManager;
import com.solexgames.lib.processor.config.ConfigFactory;
import com.solexgames.queue.adapter.JedisAdapter;
import com.solexgames.queue.command.JoinQueueCommand;
import com.solexgames.queue.command.LeaveQueueCommand;
import com.solexgames.queue.command.QueueMetaCommand;
import com.solexgames.queue.commons.model.impl.CachedQueuePlayer;
import com.solexgames.queue.commons.platform.QueuePlatform;
import com.solexgames.queue.commons.platform.QueuePlatforms;
import com.solexgames.queue.commons.queue.impl.ParentQueue;
import com.solexgames.queue.handler.PlayerHandler;
import com.solexgames.queue.handler.QueueHandler;
import com.solexgames.queue.internal.QueueBukkitSettings;
import com.solexgames.queue.provider.SettingsProvider;
import com.solexgames.queue.provider.impl.DefaultSettingsProvider;
import com.solexgames.queue.provider.impl.ScandiumSettingsProvider;
import com.solexgames.queue.runnable.QueueServerUpdateRunnable;
import lombok.Getter;
import me.lucko.helper.Events;
import me.lucko.helper.Schedulers;
import me.lucko.helper.plugin.ExtendedJavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.concurrent.CompletableFuture;

/**
 * @author GrowlyX
 * @since 7/24/2021
 */

@Getter
public final class QueueBukkit extends ExtendedJavaPlugin implements QueuePlatform {

    @Getter
    private static QueueBukkit instance;

    private PlayerHandler playerHandler;

    private QueueHandler queueHandler;
    private QueueBukkitSettings settings;

    private JedisManager jedisManager;
    private JedisManager bungeeJedisManager;

    private SettingsProvider settingsProvider;

    private final ConfigFactory factory = ConfigFactory.newFactory(this);

    @Override
    public void enable() {
        instance = this;

        QueuePlatforms.setPlatform(this);

        this.settings = this.factory.fromFile("settings", QueueBukkitSettings.class);

        this.saveDefaultConfig();

        this.setupSettingsProvider();
        this.setupJedisManager();
        this.setupQueueHandler();
        this.setupCommandManager();

        this.setupEventSubscriptions();
        this.setupTaskSubscriptions();
    }

    private void setupSettingsProvider() {
        if (Bukkit.getPluginManager().isPluginEnabled("Scandium")) {
            this.settingsProvider = new ScandiumSettingsProvider();
        } else {
            this.settingsProvider = new DefaultSettingsProvider(this.getConfig());
        }
    }

    private void setupTaskSubscriptions() {
        Schedulers.async().runRepeating(new QueueServerUpdateRunnable(this.jedisManager), 0L, 100L);
    }

    private void setupCommandManager() {
        final PaperCommandManager commandManager = new PaperCommandManager(this);

        commandManager.getCommandContexts().registerContext(ParentQueue.class, c -> {
            final String firstArgument = c.getFirstArg();
            final ParentQueue parentQueue = this.getQueueHandler().getParentQueueMap().get(firstArgument);

            if (parentQueue == null) {
                throw new InvalidCommandArgument("There is no queue named " + ChatColor.YELLOW + firstArgument + ChatColor.RED + ".");
            }

            return parentQueue;
        });

        commandManager.registerCommand(new JoinQueueCommand());
        commandManager.registerCommand(new LeaveQueueCommand());
        commandManager.registerCommand(new QueueMetaCommand());
    }

    private void setupQueueHandler() {
        this.queueHandler = new QueueHandler(this.getConfig());
        this.queueHandler.loadQueuesFromConfiguration();
    }

    private void setupEventSubscriptions() {
        Events.subscribe(AsyncPlayerPreLoginEvent.class).handler(event -> {
            final CompletableFuture<CachedQueuePlayer> completableFuture = this.playerHandler
                    .fetchCachedDataFromRedis(event.getName(), event.getUniqueId());

            completableFuture.whenComplete((queuePlayer, throwable) -> {
                if (throwable != null) {
                    throwable.printStackTrace();
                }

                if (queuePlayer != null) {
                    this.playerHandler.getPlayerTypeMap().put(event.getUniqueId(), queuePlayer);
                } else {
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, ChatColor.RED + "Something went wrong, please reconnect.");
                }
            });
        });

        Events.subscribe(PlayerQuitEvent.class).handler(event -> {
            final CachedQueuePlayer queuePlayer = this.playerHandler.getByPlayer(event.getPlayer());

            if (queuePlayer != null) {
                final CompletableFuture<Void> completableFuture = this.playerHandler
                        .updatePlayerDataToRedis(queuePlayer);

                completableFuture.whenCompleteAsync((unused, throwable) -> {
                    if (throwable != null) {
                        throwable.printStackTrace();
                    }
                });
            }
        });
    }

    private void setupJedisManager() {
        this.jedisManager = new JedisBuilder()
                .withChannel("queue_global")
                .withHandler(new JedisAdapter())
                .withSettings(this.settingsProvider.getJedisSettings())
                .build();
        this.bungeeJedisManager = new JedisBuilder()
                .withChannel("scandium:bungee")
                .withSettings(this.settingsProvider.getJedisSettings())
                .build();

        this.playerHandler = new PlayerHandler(this.jedisManager);
    }
}
