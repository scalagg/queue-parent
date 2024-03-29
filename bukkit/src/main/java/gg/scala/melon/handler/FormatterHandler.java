package gg.scala.melon.handler;

import gg.scala.melon.commons.model.impl.CachedQueuePlayer;
import gg.scala.melon.commons.queue.impl.child.ChildQueue;
import gg.scala.melon.internal.QueueBukkitSettings;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

/**
 * @author GrowlyX
 * @since 7/27/2021
 */

@RequiredArgsConstructor
public class FormatterHandler {

    private final QueueBukkitSettings bukkitSettings;

    public void sendCanJoinMessageToPlayer(Player player, CachedQueuePlayer queuePlayer, ChildQueue childQueue) {
        for (final String message : this.bukkitSettings.getCanJoinQueueMessage()) {
            player.sendMessage(message
                    .replace("<name>", childQueue.getParent().getFancyName())
                    .replace("<position>", String.valueOf(childQueue.getPosition(queuePlayer)))
                    .replace("<max>", String.valueOf(childQueue.getAllQueued()))
                    .replace("<other>", (childQueue.getName().equals("normal") ? this.bukkitSettings.getDefaultLaneExtended() : this.bukkitSettings.getPremiumLaneExtended().replace("<lane>", childQueue.getFancyName())))
            );
        }
    }

    public void sendCannotJoinMessageToPlayer(Player player, CachedQueuePlayer queuePlayer, ChildQueue childQueue, String reason) {
        for (final String message : this.bukkitSettings.getCannotJoinQueueMessage()) {
            player.sendMessage(message
                    .replace("<name>", childQueue.getParent().getFancyName())
                    .replace("<server_status>", reason)
                    .replace("<position>", String.valueOf(childQueue.getPosition(queuePlayer)))
                    .replace("<max>", String.valueOf(childQueue.getAllQueued()))
                    .replace("<other>", (childQueue.getName().equals("normal") ? this.bukkitSettings.getDefaultLaneExtended() : this.bukkitSettings.getPremiumLaneExtended().replace("<lane>", childQueue.getFancyName())))
            );
        }
    }
}
