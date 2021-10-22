package gg.scala.melon.runnable;

import com.solexgames.lib.commons.redis.JedisManager;
import gg.scala.melon.QueueBukkit;
import gg.scala.melon.commons.constants.QueueGlobalConstants;
import gg.scala.melon.commons.model.server.ServerData;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.io.File;
import java.nio.file.Files;
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
            this.serverData.setWhitelisted(QueueBukkit.getInstance().getSettingsProvider().canJoin() && this.isWhitelistEnabled());
            this.serverData.setWhitelistedPlayers(whitelistedPlayers);
            this.serverData.setLastUpdate(System.currentTimeMillis());

            jedis.hset(
                    QueueGlobalConstants.JEDIS_KEY_SERVER_DATA_CACHE,
                    QueueBukkit.getInstance().getSettingsProvider().getServerName(),
                    QueueGlobalConstants.GSON.toJson(this.serverData)
            );
        });
    }

    /**
     * We're reading the server.properties file as
     * Bukkit#getWhitelist is a bit funky
     * <p>
     * @return true if the server's whitelisted
     */
    @SneakyThrows
    private boolean isWhitelistEnabled() {
        return new String(Files.readAllBytes(new File("server.properties").toPath()))
                .contains("white-list=true");
    }
}
