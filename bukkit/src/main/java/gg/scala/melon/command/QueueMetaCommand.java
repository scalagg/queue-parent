package gg.scala.melon.command;

import gg.scala.banana.message.Message;
import net.evilblock.cubed.acf.BaseCommand;
import net.evilblock.cubed.acf.CommandHelp;
import net.evilblock.cubed.acf.ConditionFailedException;
import net.evilblock.cubed.acf.annotation.*;
import gg.scala.melon.MelonSpigotPlugin;
import gg.scala.melon.MelonSpigotConstants;
import gg.scala.melon.commons.constants.QueueGlobalConstants;
import gg.scala.melon.commons.model.server.ServerData;
import gg.scala.melon.commons.platform.QueuePlatforms;
import gg.scala.melon.commons.queue.impl.ParentQueue;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

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
    @CommandCompletion("@parents")
    @Syntax("<queue id> <key> <value>")
    @Description("Update a meta data value.")
    public void onDefault(Player player, String parent /* not using object cuz acf weird */, String key, boolean value) {
        final ParentQueue parentQueue = MelonSpigotPlugin.getInstance().getQueueHandler()
                .getParentQueueMap().get(parent);

        if (parentQueue == null) {
            throw new ConditionFailedException("There is no queue named " + ChatColor.YELLOW + parent + ChatColor.RED + ".");
        }

        final Message message = new Message("QUEUE_DATA_UPDATE");
        message.set("PARENT", parentQueue.getName());
        message.set("KEY", key);
        message.set("VALUE", Boolean.toString(value));

        message.dispatch(
                MelonSpigotPlugin.getInstance().getJedisManager()
        );

        player.sendMessage(MelonSpigotConstants.PREFIX + ChatColor.YELLOW + "Updated " + ChatColor.GOLD + key + ChatColor.YELLOW + " to " + ChatColor.AQUA + value + ChatColor.YELLOW + ".");
    }

    @Subcommand("flush")
    @Syntax("<queue id>")
    @CommandCompletion("@parents")
    @Description("Kick all players (in all lanes) out of a parent queue.")
    public void onFlush(Player player, ParentQueue parentQueue) {
        final Message message = new Message("QUEUE_FLUSH");
        message.set("PARENT", parentQueue.getName());

        message.dispatch(
                MelonSpigotPlugin.getInstance().getJedisManager()
        );

        player.sendMessage(MelonSpigotConstants.PREFIX + ChatColor.YELLOW + "Flushed parent queue " + ChatColor.GOLD + parentQueue.getFancyName() + ChatColor.YELLOW + ".");
    }

    @Subcommand("list")
    @Syntax("<queue id>")
    @CommandCompletion("@parents")
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

        final CompletableFuture<Map<String, ServerData>> completableFuture = MelonSpigotPlugin
                .getInstance().getQueueHandler().fetchAllServerData();

        completableFuture.whenComplete((stringServerDataMap, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
            }

            stringServerDataMap.forEach((s, serverData) -> {
                final boolean isQueue = QueuePlatforms.get().getQueueHandler().getParentQueueMap().get(s) != null;

                player.sendMessage(ChatColor.GRAY + " - " + ChatColor.YELLOW + s + ChatColor.GRAY + " (WL: " + (serverData.isWhitelisted() ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No") + ChatColor.GRAY + ") (Queue: " + (isQueue ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No") + ChatColor.GRAY + ")");
            });
        });
    }

    @Syntax("<server id>")
    @Subcommand("info|information")
    @Description("Fetches information for a specific server and returns it in a fancy format.")
    public void onServerId(Player player, String serverName) {
        player.sendMessage(ChatColor.GREEN + "Fetching server data from redis...");

        final CompletableFuture<ServerData> completableFuture = MelonSpigotPlugin.getInstance()
                .getQueueHandler().fetchServerData(serverName);

        completableFuture.whenComplete((serverData, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
            }

            if (serverData == null) {
                player.sendMessage(ChatColor.RED + "That server does not exist in our cache.");
                return;
            }

            final boolean isQueue = QueuePlatforms.get().getQueueHandler().getParentQueueMap().get(serverName) != null;
            final boolean isHanging = serverData.getLastUpdate() + QueueGlobalConstants.FIFTEEN_SECONDS < System.currentTimeMillis();

            player.sendMessage(new String[]{
                    ChatColor.GOLD + ChatColor.BOLD.toString() + serverName + ":",
                    " ",
                    ChatColor.GRAY + "Queueable: " + this.getFancyBoolean(isQueue),
                    ChatColor.GRAY + "Whitelisted: " + this.getFancyBoolean(serverData.isWhitelisted()),
                    " ",
                    ChatColor.GRAY + "Online Players: " + ChatColor.WHITE + serverData.getOnlinePlayers(),
                    ChatColor.GRAY + "Max Players: " + ChatColor.WHITE + serverData.getMaxPlayers(),
                    " ",
                    ChatColor.GRAY + "Status: " + (isHanging ? ChatColor.RED + "May be offline, we haven't received an update from the server for more than 15 seconds." : ChatColor.GREEN + "Online and Updating")
            });
        });
    }

    @Subcommand("update")
    @Description("Force update this server instance to redis.")
    public void onUpdate(Player player) {
        MelonSpigotPlugin.getInstance().getUpdateRunnable().run();

        player.sendMessage(MelonSpigotConstants.PREFIX + ChatColor.GREEN + "You've force updated this server instance.");
    }

    public String getFancyBoolean(boolean aBoolean) {
        return aBoolean ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No";
    }
}
