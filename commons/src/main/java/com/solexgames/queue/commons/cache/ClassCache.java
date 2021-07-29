package com.solexgames.queue.commons.cache;

import java.util.Map;

/**
 * @author GrowlyX
 * @since 7/29/2021
 */

@SuppressWarnings("all")
public abstract class ClassCache<T> {

    public abstract Map<Class<? extends T>, T> getCache();

    public <U> U get(Class<U> clazz) {
        return (U) this.getCache().getOrDefault(clazz, null);
    }
}
