package com.solexgames.queue.commons.spoof;

import com.solexgames.queue.commons.queue.impl.ParentQueue;
import com.solexgames.queue.commons.scheme.NamingScheme;
import com.solexgames.queue.commons.spoof.runnable.QueueSpoofRunnable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author GrowlyX
 * @since 7/29/2021
 */

@Getter
@RequiredArgsConstructor
public class QueueSpoof {

    private final QueueSpoofRunnable runnable = new QueueSpoofRunnable();

    private final ParentQueue parentQueue;
    private final NamingScheme namingScheme;

    private final int delay;

    private boolean running = true;

    public void init() {
        if (this.running) {
            throw new UnsupportedOperationException("Cannot initialize spoofer while it's already running");
        }
    }

    public void cancel() {
        if (!this.running) {
            throw new UnsupportedOperationException("Cannot cancel spoofer while it's not running");
        }
    }

    public void resume() {
        if (this.running) {
            throw new UnsupportedOperationException("Cannot resume spoofer while it's already running");
        }
    }

    public void pause() {
        if (!this.running) {
            throw new UnsupportedOperationException("Cannot pause spoofer while it's not running");
        }
    }
}
