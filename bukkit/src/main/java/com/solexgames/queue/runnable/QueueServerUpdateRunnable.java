package com.solexgames.queue.runnable;

import com.solexgames.lib.commons.redis.JedisManager;
import com.solexgames.queue.QueueBukkit;
import com.solexgames.queue.commons.constants.QueueGlobalConstants;
import com.solexgames.queue.commons.model.server.ServerData;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.List;
import java.util.stream.Collectors;

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
            final List<String> whitelistedPlayers = Bukkit.getWhitelistedPlayers().stream()
                    .map(OfflinePlayer::getName).collect(Collectors.toList());

            this.serverData.setMaxPlayers(Bukkit.getMaxPlayers());
            this.serverData.setOnlinePlayers(Bukkit.getOnlinePlayers().size());
            this.serverData.setWhitelisted(QueueBukkit.getInstance().getSettingsProvider().canJoin() || Bukkit.hasWhitelist());
            this.serverData.setWhitelistedPlayers(whitelistedPlayers);
            this.serverData.setLastUpdate(System.currentTimeMillis());

            jedis.hset(
                    QueueGlobalConstants.JEDIS_KEY_SERVER_DATA_CACHE,
                    QueueBukkit.getInstance().getSettingsProvider().getServerName(),
                    QueueGlobalConstants.GSON.toJson(this.serverData)
            );
        });
    }
}
