package gg.scala.melon.adapter;

import com.imaginarycode.minecraft.redisbungee.events.PlayerLeftNetworkEvent;
import gg.scala.banana.message.Message;
import gg.scala.melon.MelonProxyPlugin;
import gg.scala.melon.commons.constants.QueueGlobalConstants;
import gg.scala.melon.commons.platform.QueuePlatforms;
import gg.scala.melon.commons.queue.impl.child.ChildQueue;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.Optional;

/**
 * @author GrowlyX
 * @since 7/26/2021
 */

public class RedisBungeeListener implements Listener {

    @EventHandler
    public void onNetworkLeave(PlayerLeftNetworkEvent event)
    {
        QueuePlatforms.get().getQueueHandler().getParentQueueMap().forEach((s, parentQueue) -> {
            final Optional<ChildQueue> optionalChildQueue = parentQueue.getChildQueue(event.getUuid());

            optionalChildQueue.ifPresent(childQueue -> {
                final Message removal = new Message("QUEUE_REMOVE_PLAYER");
                removal.set("PLAYER", event.getUuid().toString());
                removal.set("PARENT", parentQueue.getName());
                removal.set("CHILD", childQueue.getName());

                removal.dispatch(
                        MelonProxyPlugin.getInstance().getJedisManager()
                );

                MelonProxyPlugin.getInstance().getJedisManager().useResource(jedis -> {
                    jedis.hset(QueueGlobalConstants.JEDIS_KEY_SETTING_CACHE, parentQueue.getName(), QueueGlobalConstants.GSON.toJson(parentQueue.getSettings()));
                    return null;
                });
            });
        });
    }
}
