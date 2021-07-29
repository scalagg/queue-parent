package com.solexgames.api;

import com.solexgames.api.platform.PlatformDependantApi;
import com.solexgames.api.platform.impl.BukkitPlatformApiImpl;
import com.solexgames.api.platform.impl.BungeeCordPlatformApiImpl;
import com.solexgames.queue.commons.cache.ClassCache;

import java.util.HashMap;
import java.util.Map;

/**
 * @author GrowlyX
 * @since 7/27/2021
 */

public class QueuePlatformAPI extends ClassCache<PlatformDependantApi> {

    private static final QueuePlatformAPI INSTANCE;

    static {
        INSTANCE = new QueuePlatformAPI();
    }

    private final Map<Class<? extends PlatformDependantApi>, PlatformDependantApi> classPlatformDependantApiMap = new HashMap<>();

    {
        this.getCache().put(BukkitPlatformApiImpl.class, new BukkitPlatformApiImpl());
        this.getCache().put(BungeeCordPlatformApiImpl.class, new BungeeCordPlatformApiImpl());
    }

    @Override
    public Map<Class<? extends PlatformDependantApi>, PlatformDependantApi> getCache() {
        return this.classPlatformDependantApiMap;
    }

    public static QueuePlatformAPI get() {
        return QueuePlatformAPI.INSTANCE;
    }
}
