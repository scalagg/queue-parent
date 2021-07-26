package com.solexgames.queue.adapter;

import com.solexgames.core.CorePlugin;
import com.solexgames.core.enums.NetworkServerStatusType;
import com.solexgames.core.server.NetworkServer;
import com.solexgames.lib.commons.redis.annotation.Subscription;
import com.solexgames.lib.commons.redis.handler.JedisHandler;
import com.solexgames.lib.commons.redis.json.JsonAppender;
import com.solexgames.queue.QueueBukkit;
import com.solexgames.queue.commons.model.impl.CachedQueuePlayer;
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
            ChatColor.GRAY + ChatColor.ITALIC.toString() + "You're queued in the <child_name> lane.",
            ChatColor.GRAY + ChatColor.ITALIC.toString() + "You can leave the queue by executing: /leavequeue",
            ChatColor.RED + "The server you're queued for is currently <server_status>.",
            "",
    };

    private static final String[] QUEUE_LANE_CAN_JOIN_MESSAGE = new String[]{
            "",
            ChatColor.YELLOW + "<name> Queue Position: " + ChatColor.GOLD + "<position>" + ChatColor.YELLOW + "/" + ChatColor.GOLD + "<max>",
            ChatColor.GRAY + ChatColor.ITALIC.toString() + "You're in the <child_name> lane.",
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

        final CachedQueuePlayer queuePlayer = CorePlugin.GSON.fromJson(jsonAppender.getParam("PLAYER"), CachedQueuePlayer.class);

        if (queuePlayer != null) {
            final ParentQueue parentQueue = QueueBukkit.getInstance().getQueueHandler()
                    .getParentQueueMap().get(parentQueueName);

            if (parentQueue != null) {
                final Optional<ChildQueue> childQueueOptional = parentQueue.getChildQueue(childQueueName);

                childQueueOptional.ifPresent(childQueue -> {
                    childQueue.getQueued().add(queuePlayer);
                });
            }
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
                final Optional<CachedQueuePlayer> queuePlayer = childQueue.findQueuePlayerInChildQueue(uuid);

                queuePlayer.ifPresent(queuePlayer1 -> {
                    childQueue.getQueued().remove(queuePlayer1);
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
                        queuePlayer.getQueueMap().remove(parentQueue.getName());
                        player.sendMessage(ChatColor.GREEN + "You're now being sent to " + ChatColor.YELLOW + parentQueue.getFancyName() + ChatColor.GREEN + ".");

                        CompletableFuture.runAsync(() -> {
                            QueueBukkit.getInstance().getJedisManager().publish(
                                    new JsonAppender("QUEUE_REMOVE_PLAYER")
                                            .put("PARENT", parentQueue.getName())
                                            .put("CHILD", childQueue.getName())
                                            .put("PLAYER", queuePlayer.getUniqueId().toString())
                                            .getAsJson()
                            );

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
            parentQueue.getChildren().forEach((integer, childQueue) -> {
                childQueue.getQueued().forEach(queuePlayer -> {
                    final Player bukkitPlayer = Bukkit.getPlayer(queuePlayer.getUniqueId());

                    if (bukkitPlayer != null) {
                        final NetworkServer networkServer = NetworkServer.getByName(parentQueue.getTargetServer());

                        if (networkServer == null) {
                            this.sendNonJoinable(bukkitPlayer, queuePlayer, childQueue, "offline");
                        } else if (networkServer.isWhitelistEnabled()) {
                            this.sendNonJoinable(bukkitPlayer, queuePlayer, childQueue, "whitelisted");
                        } else if (networkServer.getServerStatus().equals(NetworkServerStatusType.ONLINE)) {
                            this.sendJoinable(bukkitPlayer, queuePlayer, childQueue);
                        } else {
                            this.sendNonJoinable(bukkitPlayer, queuePlayer, childQueue, networkServer.getServerStatus().serverStatusString.toLowerCase());
                        }
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
                    .replace("<child_name>", childQueue.getFancyName())
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
                    .replace("<child_name>", childQueue.getFancyName())
            );
        }
    }
}
