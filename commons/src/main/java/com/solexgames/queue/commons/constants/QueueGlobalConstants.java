package com.solexgames.queue.commons.constants;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import lombok.experimental.UtilityClass;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author GrowlyX
 * @since 7/24/2021
 * <p>
 * A collection of useful constants used on all platforms.
 */

@UtilityClass
public class QueueGlobalConstants {

    public static final String JEDIS_KEY_PLAYER_CACHE = "queue_player_cache";
    public static final String JEDIS_KEY_QUEUE_CACHE = "queue_queued_cache";
    public static final String JEDIS_KEY_SETTING_CACHE = "queue_settings_cache";
    public static final String JEDIS_KEY_SERVER_DATA_CACHE = "queue_server_data_cache";

    public static final Type PRIORITY_QUEUE_PLAYER_TYPE = new TypeToken<PriorityQueue<UUID>>(){}.getType();
    public static final Type STRING_BOOLEAN_MAP_TYPE = new TypeToken<Map<String, Boolean>>(){}.getType();

    public static final Gson GSON = new GsonBuilder()
            .serializeNulls()
            .create();

    public static final long FIFTEEN_SECONDS = TimeUnit.SECONDS.toMillis(15L);
    public static final long TWO_SECONDS = TimeUnit.SECONDS.toMillis(2L);

}
