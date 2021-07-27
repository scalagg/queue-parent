package com.solexgames.queue.commons.queue.impl.child.impl;

import com.solexgames.queue.commons.queue.impl.ParentQueue;
import com.solexgames.queue.commons.queue.impl.child.ChildQueue;

/**
 * @author GrowlyX
 * @since 7/24/2021
 * <p>
 * Default {@link ChildQueue} implementation set as the first
 * priority for players who don't have permission for a
 * higher priority child queue.
 */

public class DefaultChildQueue extends ChildQueue {

    public DefaultChildQueue(ParentQueue parent) {
        super(parent, "normal", "Normal", null);
    }
}
