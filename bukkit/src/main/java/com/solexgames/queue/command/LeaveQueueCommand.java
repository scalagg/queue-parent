package com.solexgames.queue.command;

import com.solexgames.lib.acf.BaseCommand;
import com.solexgames.lib.acf.InvalidCommandArgument;
import com.solexgames.lib.acf.annotation.*;
import com.solexgames.lib.commons.redis.json.JsonAppender;
import com.solexgames.queue.QueueBukkit;
import com.solexgames.queue.commons.model.impl.CachedQueuePlayer;
import com.solexgames.queue.commons.queue.impl.ParentQueue;
import com.solexgames.queue.commons.queue.impl.child.ChildQueue;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

/**
 * @author GrowlyX
 * @since 7/25/2021
 */

@CommandAlias("leavequeue|leaveq")
public class LeaveQueueCommand extends BaseCommand {

    @Default
    @Syntax("[optional: <queue id>]")
    public void onDefault(Player player, @Optional ParentQueue parentQueue) {
        final CachedQueuePlayer queuePlayer = QueueBukkit.getInstance().getPlayerHandler().getByPlayer(player);

        if (queuePlayer == null) {
            throw new InvalidCommandArgument("Something went wrong.");
        }

        if (parentQueue != null) {
            this.leaveQueue(parentQueue, player, queuePlayer, true);
            return;
        }

        final int queuesQueuedIn = queuePlayer.getQueueMap().size();

        if (queuesQueuedIn > 1) {
            player.sendMessage(ChatColor.RED + "You're currently queued for more than one server.");
            player.sendMessage(ChatColor.RED + "Use " + ChatColor.YELLOW + "/leavequeue <queue id>" + ChatColor.RED + " to leave a specific server.");
            player.sendMessage(" ");
            player.sendMessage(ChatColor.GOLD + ChatColor.BOLD.toString() + "Queued Servers:");

            queuePlayer.getQueueMap().forEach(s -> {
                player.sendMessage(ChatColor.GRAY + " - " + ChatColor.YELLOW + s);
            });
            return;
        }

        final ParentQueue fetchedQueue = QueueBukkit.getInstance().getQueueHandler()
                .getParentQueueMap().getOrDefault(queuePlayer.getQueueMap().size() == 0 ? null : queuePlayer.getQueueMap().get(0), null);

        if (fetchedQueue != null) {
            this.leaveQueue(fetchedQueue, player, queuePlayer, false);
        } else {
            player.sendMessage(ChatColor.RED + "You're not currently queued for a server. 1");
        }
    }

    public void leaveQueue(ParentQueue parentQueue, Player player, CachedQueuePlayer queuePlayer, boolean specified) {
        final ChildQueue childQueue = parentQueue.getChildQueue(queuePlayer).orElse(null);

        if (childQueue != null) {
            CompletableFuture.runAsync(() -> {
                QueueBukkit.getInstance().getJedisManager().publish(
                        new JsonAppender("QUEUE_REMOVE_PLAYER")
                                .put("PARENT", parentQueue.getName())
                                .put("CHILD", childQueue.getName())
                                .put("PLAYER", queuePlayer.getUniqueId().toString())
                                .getAsJson()
                );
            }).whenComplete((unused, throwable) -> {
                if (throwable != null) {
                    throwable.printStackTrace();
                }

                queuePlayer.getQueueMap().remove(parentQueue.getName());

                player.sendMessage(ChatColor.RED + "You've left the " + ChatColor.YELLOW + parentQueue.getFancyName() + ChatColor.RED + " queue.");
            });
        } else {
            player.sendMessage(ChatColor.RED + "You're not currently queued for " + (specified ? ChatColor.YELLOW + parentQueue.getFancyName() : "a server") + ChatColor.RED + ".");
        }
    }
}
