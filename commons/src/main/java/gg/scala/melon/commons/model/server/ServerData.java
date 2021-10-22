package gg.scala.melon.commons.model.server;

import lombok.Data;

import java.util.List;

/**
 * @author GrowlyX
 * @since 7/26/2021
 * <p>
 * Stores basic server information
 */

@Data
public class ServerData {

    private int onlinePlayers;
    private int maxPlayers;

    private boolean whitelisted;

    private List<String> whitelistedPlayers;

    private long lastUpdate;

}
