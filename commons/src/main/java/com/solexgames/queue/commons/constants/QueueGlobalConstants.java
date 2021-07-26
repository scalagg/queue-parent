package com.solexgames.queue.commons.constants;

import com.google.gson.reflect.TypeToken;
import com.solexgames.queue.commons.model.impl.CachedQueuePlayer;
import lombok.experimental.UtilityClass;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * @author GrowlyX
 * @since 7/24/2021
 */

@UtilityClass
public class QueueGlobalConstants {

    public static final String JEDIS_KEY_PLAYER_CACHE = "queue_player_cache";
    public static final String JEDIS_KEY_QUEUE_CACHE = "queue_queued_cache";
    public static final String JEDIS_KEY_SETTING_CACHE = "queue_settings_cache";

    public static final Type CACHED_QUEUE_PLAYER_TYPE = new TypeToken<PriorityQueue<CachedQueuePlayer>>(){}.getType();
    public static final Type STRING_BOOLEAN_MAP_TYPE = new TypeToken<Map<String, Boolean>>(){}.getType();

}
