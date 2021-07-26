package com.solexgames.queue.command;

import com.solexgames.core.CorePlugin;
import com.solexgames.lib.acf.BaseCommand;
import com.solexgames.lib.acf.InvalidCommandArgument;
import com.solexgames.lib.acf.annotation.CommandAlias;
import com.solexgames.lib.acf.annotation.Default;
import com.solexgames.lib.acf.annotation.Syntax;
import com.solexgames.lib.commons.redis.json.JsonAppender;
import com.solexgames.queue.QueueBukkit;
import com.solexgames.queue.QueueBukkitConstants;
import com.solexgames.queue.commons.model.impl.CachedQueuePlayer;
import com.solexgames.queue.commons.queue.impl.ParentQueue;
import com.solexgames.queue.commons.queue.impl.child.ChildQueue;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

/**
 * @author GrowlyX
 * @since 7/25/2021
 */

@CommandAlias("joinqueue|joinq|queue")
public class JoinQueueCommand extends BaseCommand {

    @Default
    @Syntax("<queue id>")
    public void onDefault(Player player, ParentQueue parentQueue) {
        final CachedQueuePlayer queuePlayer = QueueBukkit.getInstance().getPlayerHandler().getByPlayer(player);

        if (queuePlayer == null) {
            throw new InvalidCommandArgument("Something went wrong.");
        }

        final boolean alreadyIn = queuePlayer.getQueueMap().contains(parentQueue.getName());

        if (alreadyIn) {
            throw new InvalidCommandArgument("You're already in the " + ChatColor.YELLOW + parentQueue.getFancyName() + ChatColor.RED + " queue.");
        }

        final int queueAmount = queuePlayer.getQueueMap().size() + 1;

        if (queueAmount > 2) {
            throw new InvalidCommandArgument("You cannot join more than " + ChatColor.YELLOW + "2" + ChatColor.RED + " queues at a time.");
        }

        CompletableFuture.supplyAsync(() -> {
            final ChildQueue bestChildQueue = QueueBukkit.getInstance().getQueueHandler()
                    .fetchBestChildQueue(parentQueue, player);

            QueueBukkit.getInstance().getJedisManager().publish(
                    new JsonAppender("QUEUE_ADD_PLAYER")
                            .put("PARENT", parentQueue.getName())
                            .put("CHILD", bestChildQueue.getName())
                            .put("PLAYER", CorePlugin.GSON.toJson(queuePlayer))
                            .getAsJson()
            );

            return bestChildQueue;
        }).whenComplete((childQueue, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();

                player.sendMessage(ChatColor.RED + "We couldn't add you to the " + ChatColor.YELLOW + parentQueue.getFancyName() + ChatColor.RED + " queue.");
            } else {
                queuePlayer.getQueueMap().add(parentQueue.getName());

                player.sendMessage(ChatColor.GREEN + "You've been added to the " + ChatColor.YELLOW + parentQueue.getFancyName() + ChatColor.GREEN + " queue.");
            }
        });
    }
}
