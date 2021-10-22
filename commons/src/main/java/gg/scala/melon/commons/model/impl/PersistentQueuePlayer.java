package gg.scala.melon.commons.model.impl;

import gg.scala.melon.commons.model.QueuePlayer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * @author GrowlyX
 * @since 7/26/2021
 * <p>
 * Used for storing persistent data
 */

@Getter @Setter
@RequiredArgsConstructor
public class PersistentQueuePlayer extends QueuePlayer {

    private final String name;
    private final UUID uniqueId;

    private boolean canViewSpoofAlerts = true;

}
