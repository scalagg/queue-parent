package gg.scala.melon.command;

import gg.scala.banana.message.Message;
import net.evilblock.cubed.acf.BaseCommand;
import net.evilblock.cubed.acf.ConditionFailedException;
import net.evilblock.cubed.acf.annotation.*;
import gg.scala.melon.MelonSpigotPlugin;
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
        final CachedQueuePlayer queuePlayer = MelonSpigotPlugin.getInstance()
                .getPlayerHandler().getByPlayer(player);

        if (queuePlayer == null) {
            throw new ConditionFailedException("Something went wrong.");
        }

        if (!queuePlayer.isCanQueue()) {
            throw new ConditionFailedException("Please wait a moment before joining another queue");
        }

        final List<ParentQueue> queuedServers = MelonSpigotPlugin.getInstance().getQueueHandler()
                .fetchPlayerQueuedIn(queuePlayer);

        final boolean alreadyIn = queuedServers.contains(parentQueue);

        if (alreadyIn) {
            throw new ConditionFailedException("You're already in the " + ChatColor.YELLOW + parentQueue.getFancyName() + ChatColor.RED + " queue.");
        }

        final int queueAmount = queuedServers.size() + 1;

        if (MelonSpigotPlugin.getInstance().getSettings().isAllowMultipleQueues()) {
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
            final ChildQueue bestChildQueue = MelonSpigotPlugin.getInstance().getQueueHandler()
                    .fetchBestChildQueue(parentQueue, player);

            final Message removal = new Message("QUEUE_ADD_PLAYER");
            removal.set("PLAYER", queuePlayer.getUniqueId().toString());
            removal.set("PARENT", parentQueue.getName());
            removal.set("CHILD", bestChildQueue.getName());

            removal.dispatch(
                    MelonSpigotPlugin.getInstance().getJedisManager()
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
