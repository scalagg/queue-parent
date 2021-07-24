package com.solexgames.queue.commons.queue.impl.child;

import com.solexgames.queue.commons.model.QueuePlayer;
import com.solexgames.queue.commons.queue.Queue;
import com.solexgames.queue.commons.queue.impl.ParentQueue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.PriorityQueue;

/**
 * @author GrowlyX
 * @since 7/24/2021
 */

@Getter
@RequiredArgsConstructor
public class ChildQueue extends Queue {

    private final ParentQueue parent;

    private final String name;

    private final PriorityQueue<QueuePlayer> queued = new PriorityQueue<>();

    @Override
    public boolean isQueued(QueuePlayer queuePlayer) {
        return this.queued.contains(queuePlayer);
    }

    @Override
    public String getTargetServer() {
        return this.parent.getTargetServer();
    }

    @Override
    public int getPosition(QueuePlayer queuePlayer) {
        final PriorityQueue<QueuePlayer> players = new PriorityQueue<>(this.queued);

        for (int i = 0; i <= this.queued.size(); i++) {
            final QueuePlayer player = players.poll();

            if (player != null && player.getUniqueId().equals(queuePlayer.getUniqueId())) {
                return i + 1;
            }
        }

        return 0;
    }
}
