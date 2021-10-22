package gg.scala.melon.commons.queue.impl.child;

import gg.scala.melon.commons.model.impl.CachedQueuePlayer;
import gg.scala.melon.commons.queue.Queue;
import gg.scala.melon.commons.queue.impl.ParentQueue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.Optional;
import java.util.PriorityQueue;
import java.util.UUID;

/**
 * @author GrowlyX
 * @since 7/24/2021
 */

@Getter @Setter
@RequiredArgsConstructor
public class ChildQueue extends Queue {

    private final ParentQueue parent;

    private final String name;
    private final String fancyName;
    private final String permission;

    private PriorityQueue<UUID> queued = new PriorityQueue<>();

    public int getAllQueued() {
        return this.queued.size();
    }

    public Optional<UUID> findQueuePlayerInChildQueue(UUID uuid) {
        return this.queued.stream()
                .filter(queuePlayer -> queuePlayer.equals(uuid))
                .findFirst();
    }

    @Override
    public boolean isQueued(CachedQueuePlayer queuePlayer) {
        return this.queued.stream()
                .filter(queuePlayer1 -> queuePlayer.getUniqueId().equals(queuePlayer1))
                .findFirst().orElse(null) != null;
    }

    @Override
    public String getTargetServer() {
        return this.parent.getTargetServer();
    }

    @Override
    public int getPosition(CachedQueuePlayer queuePlayer) {
        final PriorityQueue<UUID> players = new PriorityQueue<>(this.queued);

        for (int i = 0; i <= this.queued.size(); i++) {
            final UUID player = players.poll();

            if (player != null && player.equals(queuePlayer.getUniqueId())) {
                return i + 1;
            }
        }

        return 0;
    }
}
