package com.solexgames.queue;

import com.solexgames.lib.acf.ConditionFailedException;
import com.solexgames.lib.acf.PaperCommandManager;
import com.solexgames.lib.commons.redis.JedisBuilder;
import com.solexgames.lib.commons.redis.JedisManager;
import com.solexgames.lib.processor.config.ConfigFactory;
import com.solexgames.queue.adapter.JedisAdapter;
import com.solexgames.queue.cache.NamingSchemeCache;
import com.solexgames.queue.command.JoinQueueCommand;
import com.solexgames.queue.command.LeaveQueueCommand;
import com.solexgames.queue.command.QueueMetaCommand;
import com.solexgames.queue.commons.constants.QueueGlobalConstants;
import com.solexgames.queue.commons.model.impl.CachedQueuePlayer;
import com.solexgames.queue.commons.platform.QueuePlatform;
import com.solexgames.queue.commons.platform.QueuePlatforms;
import com.solexgames.queue.commons.queue.impl.ParentQueue;
import com.solexgames.queue.commons.scheme.NamingScheme;
import com.solexgames.queue.handler.FormatterHandler;
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
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

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
    private FormatterHandler formatterHandler;

    private JedisManager jedisManager;
    private JedisManager bungeeJedisManager;

    private SettingsProvider settingsProvider;
    private QueueBukkitSettings settings;

    private QueueServerUpdateRunnable updateRunnable;

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
        Schedulers.async().runRepeating(this.updateRunnable = new QueueServerUpdateRunnable(this.jedisManager), 0L, 100L);
    }

    private void setupCommandManager() {
        final PaperCommandManager commandManager = new PaperCommandManager(this);

        commandManager.enableUnstableAPI("help");

        commandManager.getCommandContexts().registerContext(ParentQueue.class, c -> {
            final String firstArgument = c.popFirstArg();
            final ParentQueue parentQueue = this.getQueueHandler().getParentQueueMap().get(firstArgument);

            if (parentQueue == null) {
                throw new ConditionFailedException("There is no queue named " + ChatColor.YELLOW + firstArgument + ChatColor.RED + ".");
            }

            return parentQueue;
        });

        final List<String> parentQueueNames = this.queueHandler.getParentQueueMap()
                .values().stream().map(ParentQueue::getName).collect(Collectors.toList());

        commandManager.getCommandCompletions().registerAsyncCompletion("parents", context -> parentQueueNames);

        final List<String> schemeNames = NamingSchemeCache.get().getCache()
                .values().stream().map(namingScheme -> namingScheme.getClass().getSimpleName()).collect(Collectors.toList());

        commandManager.getCommandCompletions().registerAsyncCompletion("schemes", context -> schemeNames);

        commandManager.registerDependency(QueueBukkitSettings.class, this.settings);

        commandManager.registerCommand(new JoinQueueCommand());
        commandManager.registerCommand(new LeaveQueueCommand());
        commandManager.registerCommand(new QueueMetaCommand());

        this.formatterHandler = new FormatterHandler(this.settings);
    }

    private void setupQueueHandler() {
        this.queueHandler = new QueueHandler(this.getConfig());
        this.queueHandler.loadQueuesFromConfiguration();
    }

    private void forceUpdateInstance() {
        this.updateRunnable.run();
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

        Events.subscribe(PlayerJoinEvent.class).handler(event -> {
            final CachedQueuePlayer queuePlayer = this.playerHandler.getByPlayer(event.getPlayer());

            if (queuePlayer != null) {
                this.queueHandler.handlePostLogin(queuePlayer);
            }
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

    @Override
    public void disable() {
        this.jedisManager.runCommand(jedis -> {
            jedis.hdel(QueueGlobalConstants.JEDIS_KEY_SERVER_DATA_CACHE, this.settingsProvider.getServerName());
        });
    }
}
