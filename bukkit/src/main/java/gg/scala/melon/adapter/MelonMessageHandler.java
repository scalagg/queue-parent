package gg.scala.melon.adapter;

import gg.scala.banana.annotate.Subscribe;
import gg.scala.banana.message.Message;
import gg.scala.banana.subscribe.marker.BananaHandler;
import gg.scala.melon.MelonSpigotPlugin;
import gg.scala.melon.commons.constants.QueueGlobalConstants;
import gg.scala.melon.commons.model.impl.CachedQueuePlayer;
import gg.scala.melon.commons.model.server.ServerData;
import gg.scala.melon.commons.platform.QueuePlatforms;
import gg.scala.melon.commons.queue.impl.ParentQueue;
import gg.scala.melon.commons.queue.impl.child.ChildQueue;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @author GrowlyX
 * @since 7/24/2021
 */

public class MelonMessageHandler implements BananaHandler {

    @Subscribe("QUEUE_DATA_UPDATE")
    public void onQueueDataUpdate(Message jsonAppender) {
        final String parentQueueName = jsonAppender.get("PARENT");
        final ParentQueue parentQueue = MelonSpigotPlugin.getInstance().getQueueHandler()
                .getParentQueueMap().get(parentQueueName);

        if (parentQueue != null) {
            parentQueue.getSettings().put(jsonAppender.get("KEY"), Boolean.parseBoolean(jsonAppender.get("VALUE")));
        }
    }

    @Subscribe("QUEUE_ADD_PLAYER")
    public void onQueueAddPlayer(Message jsonAppender) {
        final String parentQueueName = jsonAppender.get("PARENT");
        final String childQueueName = jsonAppender.get("CHILD");

        final UUID uuid = UUID.fromString(jsonAppender.get("PLAYER"));

        final ParentQueue parentQueue = MelonSpigotPlugin.getInstance().getQueueHandler()
                .getParentQueueMap().get(parentQueueName);

        if (parentQueue != null) {
            final Optional<ChildQueue> childQueueOptional = parentQueue.getChildQueue(childQueueName);

            childQueueOptional.ifPresent(childQueue -> {
                childQueue.getQueued().add(uuid);
            });
        }
    }

    @Subscribe("QUEUE_REMOVE_PLAYER")
    public void onQueueRemovePlayer(Message jsonAppender) {
        final String parentQueueName = jsonAppender.get("PARENT");
        final String childQueueName = jsonAppender.get("CHILD");
        final UUID uuid = UUID.fromString(jsonAppender.get("PLAYER"));

        final ParentQueue parentQueue = MelonSpigotPlugin.getInstance().getQueueHandler()
                .getParentQueueMap().get(parentQueueName);

        if (parentQueue != null) {
            final Optional<ChildQueue> childQueueOptional = parentQueue.getChildQueue(childQueueName);

            childQueueOptional.ifPresent(childQueue -> {
                childQueue.getQueued().remove(uuid);
            });
        }
    }

    @Subscribe("QUEUE_FLUSH")
    public void onQueueFlush(Message jsonAppender) {
        final String parentQueueName = jsonAppender.get("PARENT");

        final ParentQueue parentQueue = MelonSpigotPlugin.getInstance().getQueueHandler()
                .getParentQueueMap().get(parentQueueName);

        if (parentQueue != null) {
            CompletableFuture.runAsync(() -> {
                parentQueue.getChildren().forEach((integer, childQueue) -> {
                    childQueue.getQueued().forEach(uuid -> {
                        final Player player = Bukkit.getPlayer(uuid);

                        if (player != null) {
                            player.sendMessage(ChatColor.RED + "You've been kicked from the " + ChatColor.YELLOW + parentQueue.getFancyName() + ChatColor.RED + " queue.");
                        }
                    });
                });
            }).whenComplete((unused, throwable) -> {
                parentQueue.getChildren().forEach((integer, childQueue) -> {
                    childQueue.getQueued().clear();
                });
            });
        }
    }

    @Subscribe("QUEUE_SEND_PLAYER")
    public void onQueueSend(Message jsonAppender) {
        final UUID uuid = UUID.fromString(jsonAppender.get("PLAYER_ID"));
        final CachedQueuePlayer queuePlayer = MelonSpigotPlugin.getInstance().getPlayerHandler().getByUuid(uuid);

        if (queuePlayer != null) {
            final ParentQueue parentQueue = MelonSpigotPlugin.getInstance().getQueueHandler()
                    .getParentQueueMap().get(jsonAppender.get("PARENT"));

            if (parentQueue != null) {
                final ChildQueue childQueue = parentQueue.getChildQueue(jsonAppender.get("CHILD"))
                        .orElse(null);

                if (childQueue != null) {
                    final Player player = Bukkit.getPlayer(uuid);

                    if (player != null) {
                        player.sendMessage(ChatColor.GREEN + "You're now being sent to " + ChatColor.YELLOW + parentQueue.getFancyName() + ChatColor.GREEN + ".");

                        CompletableFuture.runAsync(() -> {
                            final Message message = new Message("SEND_SERVER");
                            message.set("PLAYER", queuePlayer.getName());
                            message.set("SERVER", parentQueue.getTargetServer());

                            message.dispatch(
                                    MelonSpigotPlugin.getInstance().getJedisManager()
                            );
                        }).whenComplete((unused, throwable) -> {
                            if (throwable != null) {
                                throwable.printStackTrace();
                            }
                        });
                    }
                }
            }
        }
    }

    @Subscribe("QUEUE_BROADCAST_ALL")
    public void onQueueBroadcast(Message jsonAppender) {
        QueuePlatforms.get().getQueueHandler().getParentQueueMap().values().forEach(parentQueue -> {
            final CompletableFuture<ServerData> completableFuture = MelonSpigotPlugin.getInstance().getQueueHandler()
                    .fetchServerData(parentQueue.getTargetServer());

            completableFuture.whenComplete((serverData, throwable) -> {
                if (throwable != null) {
                    throwable.printStackTrace();
                }

                parentQueue.getChildren().forEach((integer, childQueue) -> {
                    if (childQueue.getQueued() != null) {
                        childQueue.getQueued().forEach(uuid -> {
                            final Player bukkitPlayer = Bukkit.getPlayer(uuid);

                            if (bukkitPlayer != null) {
                                final CachedQueuePlayer queuePlayer = MelonSpigotPlugin.getInstance().getPlayerHandler().getByUuid(uuid);

                                if (serverData == null || serverData.getLastUpdate() + QueueGlobalConstants.FIFTEEN_SECONDS < System.currentTimeMillis()) {
                                    MelonSpigotPlugin.getInstance().getFormatterHandler()
                                            .sendCannotJoinMessageToPlayer(bukkitPlayer, queuePlayer, childQueue, "offline");
                                    return;
                                }

                                if (serverData.isWhitelisted()) {
                                    MelonSpigotPlugin.getInstance().getFormatterHandler()
                                            .sendCannotJoinMessageToPlayer(bukkitPlayer, queuePlayer, childQueue, "whitelisted");
                                } else if (serverData.getOnlinePlayers() >= serverData.getMaxPlayers()) {
                                    MelonSpigotPlugin.getInstance().getFormatterHandler()
                                            .sendCannotJoinMessageToPlayer(bukkitPlayer, queuePlayer, childQueue, "full");
                                } else {
                                    MelonSpigotPlugin.getInstance().getFormatterHandler()
                                            .sendCanJoinMessageToPlayer(bukkitPlayer, queuePlayer, childQueue);
                                }
                            }
                        });
                    }
                });
            });
        });
    }
}
