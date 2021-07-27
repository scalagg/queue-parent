package com.solexgames.queue.adapter;

import com.solexgames.lib.commons.redis.annotation.Subscription;
import com.solexgames.lib.commons.redis.handler.JedisHandler;
import com.solexgames.lib.commons.redis.json.JsonAppender;
import com.solexgames.queue.QueueBukkit;
import com.solexgames.queue.commons.model.impl.CachedQueuePlayer;
import com.solexgames.queue.commons.model.server.ServerData;
import com.solexgames.queue.commons.queue.impl.ParentQueue;
import com.solexgames.queue.commons.queue.impl.child.ChildQueue;
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

public class JedisAdapter implements JedisHandler {

    private static final String[] QUEUE_LANE_CANNOT_JOIN_MESSAGE = new String[]{
            "",
            ChatColor.YELLOW + "<name> Queue Position: " + ChatColor.GOLD + "<position>" + ChatColor.YELLOW + "/" + ChatColor.GOLD + "<max>",
            ChatColor.GRAY + ChatColor.ITALIC.toString() + "<other>",
            ChatColor.GRAY + ChatColor.ITALIC.toString() + "You can leave the queue by executing: /leavequeue",
            ChatColor.RED + "The server you're queued for is currently <server_status>.",
            "",
    };

    private static final String[] QUEUE_LANE_CAN_JOIN_MESSAGE = new String[]{
            "",
            ChatColor.YELLOW + "<name> Queue Position: " + ChatColor.GOLD + "<position>" + ChatColor.YELLOW + "/" + ChatColor.GOLD + "<max>",
            ChatColor.GRAY + ChatColor.ITALIC.toString() + "<other>",
            ChatColor.GRAY + ChatColor.ITALIC.toString() + "You can leave the queue by executing: /leavequeue",
            "",
    };

    @Subscription(action = "QUEUE_DATA_UPDATE")
    public void onQueueDataUpdate(JsonAppender jsonAppender) {
        final String parentQueueName = jsonAppender.getParam("PARENT");
        final ParentQueue parentQueue = QueueBukkit.getInstance().getQueueHandler()
                .getParentQueueMap().get(parentQueueName);

        if (parentQueue != null) {
            parentQueue.getSettings().put(jsonAppender.getParam("KEY"), Boolean.parseBoolean(jsonAppender.getParam("VALUE")));
        }
    }

    @Subscription(action = "QUEUE_ADD_PLAYER")
    public void onQueueAddPlayer(JsonAppender jsonAppender) {
        final String parentQueueName = jsonAppender.getParam("PARENT");
        final String childQueueName = jsonAppender.getParam("CHILD");

        final UUID uuid = UUID.fromString(jsonAppender.getParam("PLAYER"));

        final ParentQueue parentQueue = QueueBukkit.getInstance().getQueueHandler()
                .getParentQueueMap().get(parentQueueName);

        if (parentQueue != null) {
            final Optional<ChildQueue> childQueueOptional = parentQueue.getChildQueue(childQueueName);

            childQueueOptional.ifPresent(childQueue -> {
                childQueue.getQueued().add(uuid);
            });
        }
    }

    @Subscription(action = "QUEUE_REMOVE_PLAYER")
    public void onQueueRemovePlayer(JsonAppender jsonAppender) {
        final String parentQueueName = jsonAppender.getParam("PARENT");
        final String childQueueName = jsonAppender.getParam("CHILD");
        final UUID uuid = UUID.fromString(jsonAppender.getParam("PLAYER"));

        final ParentQueue parentQueue = QueueBukkit.getInstance().getQueueHandler()
                .getParentQueueMap().get(parentQueueName);

        if (parentQueue != null) {
            final Optional<ChildQueue> childQueueOptional = parentQueue.getChildQueue(childQueueName);

            childQueueOptional.ifPresent(childQueue -> {
                childQueue.getQueued().remove(uuid);
            });
        }
    }

    @Subscription(action = "QUEUE_FLUSH")
    public void onQueueFlush(JsonAppender jsonAppender) {
        final String parentQueueName = jsonAppender.getParam("PARENT");

        final ParentQueue parentQueue = QueueBukkit.getInstance().getQueueHandler()
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

    @Subscription(action = "QUEUE_SEND_PLAYER")
    public void onQueueSend(JsonAppender jsonAppender) {
        final UUID uuid = UUID.fromString(jsonAppender.getParam("PLAYER_ID"));
        final CachedQueuePlayer queuePlayer = QueueBukkit.getInstance().getPlayerHandler().getByUuid(uuid);

        if (queuePlayer != null) {
            final ParentQueue parentQueue = QueueBukkit.getInstance().getQueueHandler()
                    .getParentQueueMap().get(jsonAppender.getParam("PARENT"));

            if (parentQueue != null) {
                final ChildQueue childQueue = parentQueue.getChildQueue(jsonAppender.getParam("CHILD"))
                        .orElse(null);

                if (childQueue != null) {
                    final Player player = Bukkit.getPlayer(uuid);

                    if (player != null) {
                        player.sendMessage(ChatColor.GREEN + "You're now being sent to " + ChatColor.YELLOW + parentQueue.getFancyName() + ChatColor.GREEN + ".");

                        CompletableFuture.runAsync(() -> {
                            QueueBukkit.getInstance().getBungeeJedisManager().publish(
                                    new JsonAppender("SEND_SERVER")
                                            .put("PLAYER", queuePlayer.getName())
                                            .put("SERVER", parentQueue.getTargetServer())
                                            .getAsJson()
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

    @Subscription(action = "QUEUE_BROADCAST_ALL")
    public void onQueueBroadcast(JsonAppender jsonAppender) {
        QueueBukkit.getInstance().getQueueHandler().getParentQueueMap().values().forEach(parentQueue -> {
            final CompletableFuture<ServerData> completableFuture = QueueBukkit.getInstance().getQueueHandler()
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
                                final CachedQueuePlayer queuePlayer = QueueBukkit.getInstance().getPlayerHandler().getByUuid(uuid);

                                if (serverData == null) {
                                    this.sendNonJoinable(bukkitPlayer, queuePlayer, childQueue, "offline");
                                    return;
                                }

                                if (serverData.isWhitelisted()) {
                                    this.sendNonJoinable(bukkitPlayer, queuePlayer, childQueue, "whitelisted");
                                } else if (serverData.getOnlinePlayers() >= serverData.getMaxPlayers()) {
                                    this.sendNonJoinable(bukkitPlayer, queuePlayer, childQueue, "full");
                                } else {
                                    this.sendJoinable(bukkitPlayer, queuePlayer, childQueue);
                                }
                            }
                        });
                    }
                });
            });
        });
    }

    public void sendNonJoinable(Player player, CachedQueuePlayer queuePlayer, ChildQueue childQueue, String status) {
        for (String message : JedisAdapter.QUEUE_LANE_CANNOT_JOIN_MESSAGE) {
            player.sendMessage(message
                    .replace("<name>", childQueue.getParent().getFancyName())
                    .replace("<position>", String.valueOf(childQueue.getPosition(queuePlayer)))
                    .replace("<max>", String.valueOf(childQueue.getAllQueued()))
                    .replace("<other>", (childQueue.getName().equals("normal") ? ChatColor.GRAY + ChatColor.ITALIC.toString() + "Want access to the fast pass? Purchase access at " + ChatColor.GOLD + "store.pvp.bar" + ChatColor.GRAY + ChatColor.ITALIC.toString() + "." : ChatColor.GRAY + ChatColor.ITALIC.toString() + "You're in the " + childQueue.getFancyName() + ChatColor.GRAY + ChatColor.ITALIC.toString() + " lane."))
                    .replace("<server_status>", status)
            );
        }
    }

    public void sendJoinable(Player player, CachedQueuePlayer queuePlayer, ChildQueue childQueue) {
        for (String message : JedisAdapter.QUEUE_LANE_CAN_JOIN_MESSAGE) {
            player.sendMessage(message
                    .replace("<name>", childQueue.getParent().getFancyName())
                    .replace("<position>", String.valueOf(childQueue.getPosition(queuePlayer)))
                    .replace("<max>", String.valueOf(childQueue.getAllQueued()))
                    .replace("<other>", (childQueue.getName().equals("normal") ? ChatColor.GRAY + ChatColor.ITALIC.toString() + "Want access to the fast pass? Purchase access at " + ChatColor.GOLD + "store.pvp.bar" + ChatColor.GRAY + ChatColor.ITALIC.toString() + "." : ChatColor.GRAY + ChatColor.ITALIC.toString() + "You're in the " + childQueue.getFancyName() + ChatColor.GRAY + ChatColor.ITALIC.toString() + " lane."))
            );
        }
    }
}
