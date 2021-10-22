package gg.scala.melon.internal;

import lombok.Data;
import org.bukkit.ChatColor;
import xyz.mkotb.configapi.Coloured;
import xyz.mkotb.configapi.RequiredField;
import xyz.mkotb.configapi.comment.Comment;

import java.util.Arrays;
import java.util.List;

/**
 * @author GrowlyX
 * @since 7/26/2021
 */

@Data
public class QueueBukkitSettings {

    @Comment("Should we allow players to join multiple queues at a time?")
    private final boolean allowMultipleQueues = true;

    @Comment("What should we format the message which a player is sent as an update for the queue being online?")
    private final List<String> canJoinQueueMessage = Arrays.asList(
            "",
            ChatColor.YELLOW + "<name> Queue Position: " + ChatColor.GOLD + "<position>" + ChatColor.YELLOW + "/" + ChatColor.GOLD + "<max>",
            ChatColor.GRAY + ChatColor.ITALIC.toString() + "<other>",
            ChatColor.GRAY + ChatColor.ITALIC.toString() + "You can leave the queue by executing: /leavequeue",
            ""
    );

    @Comment("What should we format the message which a player is sent as an update for the queue they are in being offline, or whitelisted?")
    private final List<String> cannotJoinQueueMessage = Arrays.asList(
            "",
            ChatColor.YELLOW + "<name> Queue Position: " + ChatColor.GOLD + "<position>" + ChatColor.YELLOW + "/" + ChatColor.GOLD + "<max>",
            ChatColor.GRAY + ChatColor.ITALIC.toString() + "<other>",
            ChatColor.GRAY + ChatColor.ITALIC.toString() + "You can leave the queue by executing: /leavequeue",
            ChatColor.RED + "The server you're queued for is currently <server_status>.",
            ""
    );

    @Coloured
    @Comment("What should we replace <other> with in the queue broadcasts if the player's in a default lane?")
    private final String defaultLaneExtended = ChatColor.GRAY + ChatColor.ITALIC.toString() + "Want access to the fast pass? Purchase access at " + ChatColor.GOLD + "store.pvp.bar" + ChatColor.GRAY + ChatColor.ITALIC.toString() + ".";

    @Coloured
    @Comment("What should we replace <other> with in the queue broadcasts if the player's in a premium lane?")
    private final String premiumLaneExtended = ChatColor.GRAY + ChatColor.ITALIC.toString() + "You're in the <lane> lane.";

    @Coloured
    @Comment("What message should we send to a player when they join a queue?")
    private final String joinQueueMessage = ChatColor.GREEN + "You've been added to the " + ChatColor.YELLOW + "<queue>" + ChatColor.GREEN + " queue.";

    @Coloured
    @Comment("What message should we send to a player when they leave a queue?")
    private final String leaveQueueMessage = ChatColor.RED + "You've left the " + ChatColor.YELLOW + "<queue>" + ChatColor.RED + " queue.";

    @RequiredField
    @Comment("Should we prioritize players in the queue even though we use lanes?")
    private final boolean shouldPrioritizePlayers = false;

}
