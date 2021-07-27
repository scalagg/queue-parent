package com.solexgames.queue.commons.model.server;

import lombok.Data;

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

}
