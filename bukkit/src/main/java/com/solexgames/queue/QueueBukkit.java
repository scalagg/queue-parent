package com.solexgames.queue;

import com.solexgames.core.CorePlugin;
import com.solexgames.lib.acf.InvalidCommandArgument;
import com.solexgames.lib.commons.processor.AcfCommandProcessor;
import com.solexgames.lib.commons.redis.JedisBuilder;
import com.solexgames.lib.commons.redis.JedisManager;
import com.solexgames.lib.processor.config.ConfigFactory;
import com.solexgames.queue.adapter.JedisAdapter;
import com.solexgames.queue.command.JoinQueueCommand;
import com.solexgames.queue.command.LeaveQueueCommand;
import com.solexgames.queue.command.QueueSettingsCommand;
import com.solexgames.queue.commons.model.impl.CachedQueuePlayer;
import com.solexgames.queue.commons.queue.impl.ParentQueue;
import com.solexgames.queue.handler.PlayerHandler;
import com.solexgames.queue.handler.QueueHandler;
import me.lucko.helper.Events;
import me.lucko.helper.plugin.ExtendedJavaPlugin;
import org.bukkit.ChatColor;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import lombok.Getter;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * @author GrowlyX
 * @since 7/24/2021
 */

@Getter
public final class QueueBukkit extends ExtendedJavaPlugin {

    @Getter
    private static QueueBukkit instance;

    private PlayerHandler playerHandler;
    private QueueHandler queueHandler;

    private JedisManager jedisManager;
    private JedisManager bungeeJedisManager;

    @Override
    public void enable() {
        instance = this;

        this.saveDefaultConfig();
        this.setupJedisManager();
        this.setupQueueHandler();
        this.setupCommandManager();
        this.setupTasks();
    }

    private void setupCommandManager() {
        final AcfCommandProcessor processor = new AcfCommandProcessor(this);

        processor.getCommandContexts().registerContext(ParentQueue.class, c -> {
            final String firstArgument = c.getFirstArg();
            final ParentQueue parentQueue = this.getQueueHandler().getParentQueueMap().get(firstArgument);

            if (parentQueue == null) {
                throw new InvalidCommandArgument("There is no queue named " + ChatColor.YELLOW + firstArgument + ChatColor.RED + ".");
            }

            return parentQueue;
        });

        processor.registerCommand(new JoinQueueCommand());
        processor.registerCommand(new LeaveQueueCommand());
        processor.registerCommand(new QueueSettingsCommand());
    }

    private void setupQueueHandler() {
        this.queueHandler = new QueueHandler(this.getConfig());
        this.queueHandler.loadQueuesFromConfiguration();
    }

    private void setupTasks() {
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
                .withSettings(CorePlugin.getInstance().getDefaultJedisSettings())
                .build();
        this.bungeeJedisManager = new JedisBuilder()
                .withChannel("scandium:bungee")
                .withSettings(CorePlugin.getInstance().getDefaultJedisSettings())
                .build();

        this.playerHandler = new PlayerHandler(this.jedisManager);
    }
}
