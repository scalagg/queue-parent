package com.solexgames.queue.commons.queue.impl.child.impl;

import com.solexgames.queue.commons.queue.impl.ParentQueue;
import com.solexgames.queue.commons.queue.impl.child.ChildQueue;

/**
 * @author GrowlyX
 * @since 7/24/2021
 */

public class DefaultChildQueue extends ChildQueue {

    public DefaultChildQueue(ParentQueue parent) {
        super(parent, "Normal");
    }
}
