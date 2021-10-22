package gg.scala.melon;

import gg.scala.banana.Banana;
import gg.scala.banana.BananaBuilder;
import gg.scala.banana.handler.impl.DefaultExceptionHandler;
import gg.scala.banana.options.BananaOptions;
import gg.scala.commons.ExtendedScalaPlugin;
import gg.scala.lemon.Lemon;
import gg.scala.melon.cache.NamingSchemeCache;
import gg.scala.melon.command.JoinQueueCommand;
import gg.scala.melon.command.LeaveQueueCommand;
import gg.scala.melon.command.QueueMetaCommand;
import gg.scala.melon.commons.constants.QueueGlobalConstants;
import gg.scala.melon.commons.model.impl.CachedQueuePlayer;
import gg.scala.melon.commons.platform.QueuePlatform;
import gg.scala.melon.commons.platform.QueuePlatforms;
import gg.scala.melon.commons.queue.impl.ParentQueue;
import gg.scala.melon.handler.FormatterHandler;
import gg.scala.melon.handler.PlayerHandler;
import gg.scala.melon.handler.QueueHandler;
import gg.scala.melon.internal.QueueBukkitSettings;
import gg.scala.melon.runnable.QueueServerUpdateRunnable;
import lombok.Getter;
import me.lucko.helper.Events;
import me.lucko.helper.Schedulers;
import net.evilblock.cubed.acf.ConditionFailedException;
import net.evilblock.cubed.command.manager.CubedCommandManager;
import net.evilblock.cubed.serializers.Serializers;
import org.bukkit.ChatColor;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import xyz.mkotb.configapi.ConfigFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

/**
 * @author GrowlyX
 * @since 7/24/2021
 */

@Getter
public final class MelonSpigotPlugin extends ExtendedScalaPlugin implements QueuePlatform {

    @Getter
    private static MelonSpigotPlugin instance;

    private final ConfigFactory factory = ConfigFactory.newFactory(this);

    private PlayerHandler playerHandler;
    private QueueHandler queueHandler;
    private FormatterHandler formatterHandler;

    private Banana jedisManager;
    private QueueBukkitSettings settings;
    private QueueServerUpdateRunnable updateRunnable;

    @Override
    public void enable() {
        instance = this;

        QueuePlatforms.setPlatform(this);

        this.settings = this.factory.fromFile("settings", QueueBukkitSettings.class);

        this.saveDefaultConfig();

        this.setupJedisManager();
        this.setupQueueHandler();
        this.setupCommandManager();

        this.setupEventSubscriptions();
        this.setupTaskSubscriptions();
    }

    private void setupTaskSubscriptions() {
        Schedulers.async().runRepeating(
                this.updateRunnable = new QueueServerUpdateRunnable(this.jedisManager), 0L, 100L
        );
    }

    private void setupCommandManager() {
        final CubedCommandManager commandManager = new CubedCommandManager(
                this,
                ChatColor.valueOf(Lemon.instance.lemonWebData.getPrimary()),
                ChatColor.valueOf(Lemon.instance.lemonWebData.getSecondary())
        );

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
                }
            });
        });

        Events.subscribe(PlayerJoinEvent.class).handler(event -> {
            final CachedQueuePlayer queuePlayer = this.playerHandler.getByPlayer(event.getPlayer());

            if (queuePlayer != null) {
                this.queueHandler.handlePostLogin(queuePlayer, event.getPlayer());
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
        this.jedisManager = new BananaBuilder()
                .credentials(
                        Lemon.getInstance().getCredentials()
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

        this.playerHandler = new PlayerHandler(this.jedisManager);
    }

    @Override
    public void disable() {
        this.jedisManager.runCommand(jedis -> {
            jedis.hdel(QueueGlobalConstants.JEDIS_KEY_SERVER_DATA_CACHE, this.settingsProvider.getServerName());
        });
    }
}
