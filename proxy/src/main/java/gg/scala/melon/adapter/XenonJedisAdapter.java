package gg.scala.melon.adapter;

import gg.scala.banana.annotate.Subscribe;
import gg.scala.banana.message.Message;
import gg.scala.banana.subscribe.marker.BananaHandler;
import gg.scala.melon.MelonProxyPlugin;
import gg.scala.melon.commons.constants.QueueGlobalConstants;
import gg.scala.melon.commons.platform.QueuePlatforms;
import gg.scala.melon.commons.queue.impl.child.ChildQueue;

import java.util.Optional;
import java.util.UUID;

/**
 * @author GrowlyX
 * @since 7/26/2021
 */

public class XenonJedisAdapter implements BananaHandler {

    @Subscribe("GLOBAL_DISCONNECT")
    public void onGlobalDisconnect(Message jsonAppender) {
        final UUID uuid = UUID.fromString(jsonAppender.get("UUID"));

        QueuePlatforms.get().getQueueHandler().getParentQueueMap().forEach((s, parentQueue) -> {
            final Optional<ChildQueue> optionalChildQueue = parentQueue.getChildQueue(uuid);

            optionalChildQueue.ifPresent(childQueue -> {
                final Message removal = new Message("QUEUE_REMOVE_PLAYER");
                removal.set("PLAYER", uuid.toString());
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
