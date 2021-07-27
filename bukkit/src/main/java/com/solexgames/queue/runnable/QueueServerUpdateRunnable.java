package com.solexgames.queue.runnable;

import com.solexgames.core.CorePlugin;
import com.solexgames.lib.commons.redis.JedisManager;
import com.solexgames.queue.commons.constants.QueueGlobalConstants;
import com.solexgames.queue.commons.model.server.ServerData;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;

/**
 * @author GrowlyX
 * @since 7/26/2021
 */

@RequiredArgsConstructor
public class QueueServerUpdateRunnable implements Runnable {

    private final JedisManager jedisManager;

    private final ServerData serverData = new ServerData();

    @Override
    public void run() {
        this.jedisManager.runCommand(jedis -> {
            this.serverData.setMaxPlayers(Bukkit.getMaxPlayers());
            this.serverData.setOnlinePlayers(Bukkit.getOnlinePlayers().size());
            this.serverData.setWhitelisted(Bukkit.hasWhitelist());

            jedis.hset(
                    QueueGlobalConstants.JEDIS_KEY_SERVER_DATA_CACHE,
                    CorePlugin.getInstance().getServerName(),
                    QueueGlobalConstants.GSON.toJson(this.serverData)
            );
        });
    }
}
