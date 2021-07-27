package com.solexgames.api;

import com.solexgames.api.platform.PlatformDependantApi;
import com.solexgames.api.platform.impl.BukkitPlatformApiImpl;
import com.solexgames.api.platform.impl.BungeeCordPlatformApiImpl;

import java.util.HashMap;
import java.util.Map;

/**
 * @author GrowlyX
 * @since 7/27/2021
 */

@SuppressWarnings("all")
public class QueuePlatformAPI {

    private static final Map<Class<? extends PlatformDependantApi>, PlatformDependantApi> PLATFORM_DEPENDANT_API_MAP = new HashMap<>();

    static {
        PLATFORM_DEPENDANT_API_MAP.put(BukkitPlatformApiImpl.class, new BukkitPlatformApiImpl());
        PLATFORM_DEPENDANT_API_MAP.put(BungeeCordPlatformApiImpl.class, new BungeeCordPlatformApiImpl());
    }

    public static <T> T get(Class<T> clazz) {
        return (T) QueuePlatformAPI.PLATFORM_DEPENDANT_API_MAP.getOrDefault(clazz, null);
    }
}
