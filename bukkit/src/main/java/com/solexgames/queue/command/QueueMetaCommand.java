package com.solexgames.queue.command;

import com.solexgames.core.CorePlugin;
import com.solexgames.lib.acf.BaseCommand;
import com.solexgames.lib.acf.CommandHelp;
import com.solexgames.lib.acf.InvalidCommandArgument;
import com.solexgames.lib.acf.annotation.*;
import com.solexgames.lib.commons.redis.json.JsonAppender;
import com.solexgames.queue.QueueBukkit;
import com.solexgames.queue.commons.model.server.ServerData;
import com.solexgames.queue.commons.queue.impl.ParentQueue;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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

    @Subcommand("servers")
    @Description("Fetch and return all available servers and their information.")
    public void onQueueList(Player player) {
        player.sendMessage(ChatColor.GREEN + "Fetching server data from redis...");

        final CompletableFuture<Map<String, ServerData>> completableFuture = QueueBukkit
                .getInstance().getQueueHandler().fetchAllServerData();

        completableFuture.whenComplete((stringServerDataMap, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
            }

            stringServerDataMap.forEach((s, serverData) -> {
                final boolean isQueue = QueueBukkit.getInstance().getQueueHandler().getParentQueueMap().get(s) != null;

                player.sendMessage(ChatColor.GRAY + " - " + ChatColor.YELLOW + s + ChatColor.GRAY + " (WL: " + (serverData.isWhitelisted() ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No") + ChatColor.GRAY + ") (Queue: " + (isQueue ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No") + ChatColor.GRAY + ")");
            });
        });
    }

    @Syntax("<server id>")
    @Subcommand("info|information")
    @Description("Fetches information for a specific server and returns it in a fancy format.")
    public void onServerId(Player player, String serverName) {
        player.sendMessage(ChatColor.GREEN + "Fetching server data from redis...");

        final CompletableFuture<ServerData> completableFuture = QueueBukkit.getInstance()
                .getQueueHandler().fetchServerData(serverName);

        completableFuture.whenComplete((serverData, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
            }

            if (serverData == null) {
                throw new InvalidCommandArgument("That server's not online.");
            }

            player.sendMessage(" ");
            player.sendMessage(" ");
            player.sendMessage(" ");
        });
    }
}
