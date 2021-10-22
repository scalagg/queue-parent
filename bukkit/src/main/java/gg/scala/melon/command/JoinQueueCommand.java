package gg.scala.melon.command;

import com.solexgames.lib.acf.BaseCommand;
import com.solexgames.lib.acf.ConditionFailedException;
import com.solexgames.lib.acf.annotation.*;
import com.solexgames.lib.commons.redis.json.JsonAppender;
import gg.scala.melon.QueueBukkit;
import gg.scala.melon.commons.model.impl.CachedQueuePlayer;
import gg.scala.melon.commons.queue.impl.ParentQueue;
import gg.scala.melon.commons.queue.impl.child.ChildQueue;
import gg.scala.melon.internal.QueueBukkitSettings;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author GrowlyX
 * @since 7/25/2021
 */

@CommandAlias("joinqueue|joinq|queue")
@CommandPermission("queue.command.joinqueue")
public class JoinQueueCommand extends BaseCommand {

    @Dependency
    private QueueBukkitSettings settings;

    @Default
    @Syntax("<queue id>")
    public void onDefault(Player player, ParentQueue parentQueue) {
        final CachedQueuePlayer queuePlayer = QueueBukkit.getInstance().getPlayerHandler().getByPlayer(player);

        if (queuePlayer == null) {
            throw new ConditionFailedException("Something went wrong.");
        }

        if (!queuePlayer.isCanQueue()) {
            throw new ConditionFailedException("Please wait a moment before joining another queue");
        }

        final List<ParentQueue> queuedServers = QueueBukkit.getInstance().getQueueHandler()
                .fetchPlayerQueuedIn(queuePlayer);

        final boolean alreadyIn = queuedServers.contains(parentQueue);

        if (alreadyIn) {
            throw new ConditionFailedException("You're already in the " + ChatColor.YELLOW + parentQueue.getFancyName() + ChatColor.RED + " queue.");
        }

        final int queueAmount = queuedServers.size() + 1;

        if (QueueBukkit.getInstance().getSettings().isAllowMultipleQueues()) {
            if (queueAmount > 2) {
                throw new ConditionFailedException("You cannot join more than " + ChatColor.YELLOW + "2" + ChatColor.RED + " queues at a time.");
            }
        } else {
            if (queueAmount > 1) {
                final String queueName = queuedServers.get(0).getFancyName();

                throw new ConditionFailedException("You cannot join " + ChatColor.YELLOW + parentQueue.getFancyName() + ChatColor.RED + " as you're queued for " + ChatColor.YELLOW + queueName + ChatColor.RED + ".");
            }
        }

        CompletableFuture.supplyAsync(() -> {
            final ChildQueue bestChildQueue = QueueBukkit.getInstance().getQueueHandler()
                    .fetchBestChildQueue(parentQueue, player);

            QueueBukkit.getInstance().getJedisManager().publish(
                    new JsonAppender("QUEUE_ADD_PLAYER")
                            .put("PARENT", parentQueue.getName())
                            .put("CHILD", bestChildQueue.getName())
                            .put("PLAYER", queuePlayer.getUniqueId().toString())
                            .getAsJson()
            );

            return bestChildQueue;
        }).whenComplete((childQueue, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();

                player.sendMessage(ChatColor.RED + "We couldn't add you to the " + ChatColor.YELLOW + parentQueue.getFancyName() + ChatColor.RED + " queue.");
            } else {
                player.sendMessage(this.settings.getJoinQueueMessage().replace("<queue>", parentQueue.getFancyName()));
            }
        });
    }
}
