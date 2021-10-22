package gg.scala.melon.command;

import gg.scala.banana.message.Message;
import gg.scala.melon.MelonSpigotPlugin;
import gg.scala.melon.commons.model.impl.CachedQueuePlayer;
import gg.scala.melon.commons.queue.impl.ParentQueue;
import gg.scala.melon.commons.queue.impl.child.ChildQueue;
import gg.scala.melon.internal.QueueBukkitSettings;
import net.evilblock.cubed.acf.BaseCommand;
import net.evilblock.cubed.acf.ConditionFailedException;
import net.evilblock.cubed.acf.annotation.*;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author GrowlyX
 * @since 7/25/2021
 */

@CommandAlias("leavequeue|leaveq")
@CommandPermission("queue.command.leavequeue")
public class LeaveQueueCommand extends BaseCommand {

    @Dependency
    private QueueBukkitSettings settings;

    @Default
    @Syntax("[optional: <queue id>]")
    public void onDefault(Player player, @Optional ParentQueue parentQueue) {
        final CachedQueuePlayer queuePlayer = MelonSpigotPlugin.getInstance().getPlayerHandler().getByPlayer(player);

        if (queuePlayer == null) {
            throw new ConditionFailedException("Something went wrong.");
        }

        if (parentQueue != null) {
            this.leaveQueue(parentQueue, player, queuePlayer, true);
            return;
        }

        final List<ParentQueue> queuedServers = MelonSpigotPlugin.getInstance().getQueueHandler()
                .fetchPlayerQueuedIn(queuePlayer);

        final int queuesQueuedIn = queuedServers.size();

        if (queuesQueuedIn > 1) {
            player.sendMessage(ChatColor.RED + "You're currently queued for more than one server.");
            player.sendMessage(ChatColor.RED + "Use " + ChatColor.YELLOW + "/leavequeue <queue id>" + ChatColor.RED + " to leave a specific server.");
            player.sendMessage(" ");
            player.sendMessage(ChatColor.GOLD + ChatColor.BOLD.toString() + "Queued Servers:");

            queuedServers.forEach(queue -> {
                player.sendMessage(ChatColor.GRAY + " - " + ChatColor.YELLOW + queue.getName());
            });
            return;
        }

        final ParentQueue fetchedQueue = queuedServers.size() == 0 ? null : queuedServers.get(0);

        if (fetchedQueue != null) {
            this.leaveQueue(fetchedQueue, player, queuePlayer, false);
        } else {
            player.sendMessage(ChatColor.RED + "You're not currently queued for a server.");
        }
    }

    public void leaveQueue(ParentQueue parentQueue, Player player, CachedQueuePlayer queuePlayer, boolean specified) {
        final ChildQueue childQueue = parentQueue.getChildQueue(queuePlayer).orElse(null);

        if (childQueue != null) {
            CompletableFuture.runAsync(() -> {
                final Message removal = new Message("QUEUE_REMOVE_PLAYER");
                removal.set("PLAYER", queuePlayer.getUniqueId().toString());
                removal.set("PARENT", parentQueue.getName());
                removal.set("CHILD", childQueue.getName());

                removal.dispatch(
                        MelonSpigotPlugin.getInstance().getJedisManager()
                );
            }).whenComplete((unused, throwable) -> {
                if (throwable != null) {
                    throwable.printStackTrace();
                }

                player.sendMessage(this.settings.getLeaveQueueMessage().replace("<queue>", parentQueue.getFancyName()));
            });
        } else {
            player.sendMessage(ChatColor.RED + "You're not currently queued for " + (specified ? ChatColor.YELLOW + parentQueue.getFancyName() : "a server") + ChatColor.RED + ".");
        }
    }
}
