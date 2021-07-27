package com.solexgames.queue.command;

import com.solexgames.core.CorePlugin;
import com.solexgames.lib.acf.BaseCommand;
import com.solexgames.lib.acf.CommandHelp;
import com.solexgames.lib.acf.InvalidCommandArgument;
import com.solexgames.lib.acf.annotation.*;
import com.solexgames.lib.commons.redis.json.JsonAppender;
import com.solexgames.queue.QueueBukkit;
import com.solexgames.queue.commons.queue.impl.ParentQueue;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * @author GrowlyX
 * @since 7/25/2021
 */

@CommandAlias("queuemeta|qmeta")
@CommandPermission("queue.command.meta")
public class QueueMetaCommand extends BaseCommand {

    @Default
    @HelpCommand
    @Syntax("[page]")
    public void onHelp(CommandHelp help) {
        help.showHelp();
    }

    @Subcommand("update")
    @Syntax("<queue id> <key> <value>")
    @Description("Update a meta data value.")
    public void onDefault(Player player, String parent /* not using object cuz acf weird */, String key, boolean value) {
        final ParentQueue parentQueue = QueueBukkit.getInstance().getQueueHandler()
                .getParentQueueMap().get(parent);

        if (parentQueue == null) {
            throw new InvalidCommandArgument("There is no queue named " + ChatColor.YELLOW + parent + ChatColor.RED + ".");
        }

        QueueBukkit.getInstance().getJedisManager().publish(
                new JsonAppender("QUEUE_DATA_UPDATE")
                        .put("PARENT", parentQueue.getName())
                        .put("KEY", key)
                        .put("VALUE", value)
                        .getAsJson()
        );

        player.sendMessage(ChatColor.YELLOW + "Updated " + ChatColor.GOLD + key + ChatColor.YELLOW + " to " + ChatColor.AQUA + value + ChatColor.YELLOW + ".");
    }

    @Subcommand("list")
    @Syntax("<queue id>")
    @Description("List all meta data for a queue.")
    public void onList(Player player, ParentQueue parentQueue) {
        player.sendMessage(ChatColor.GOLD + ChatColor.BOLD.toString() + "Meta Data for " + parentQueue.getFancyName() + ":");
        player.sendMessage(ChatColor.GRAY + "Available Values: " + ChatColor.WHITE + parentQueue.getSettings().size());
        player.sendMessage(" ");

        parentQueue.getSettings().forEach((s, aBoolean) -> {
            player.sendMessage(ChatColor.GRAY + " - " + ChatColor.YELLOW + s + ChatColor.GOLD + " (" + aBoolean + ")");
        });
    }

    @Subcommand("list-all-queues")
    @Description("Fetch and return all available queues and their information.")
    public void onQueueList(Player player) {

    }
}
