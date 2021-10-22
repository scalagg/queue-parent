package gg.scala.melon.commons;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author GrowlyX
 * @since 10/10/2021
 */
@Getter
public abstract class PlayerCache<T> {

    private final Map<UUID, T> playerTypeMap = new HashMap<>();

    public void insert(UUID uuid, T profile) {
        this.playerTypeMap.put(uuid, profile);
    }

    public void remove(UUID uuid) {
        this.playerTypeMap.remove(uuid);
    }

    public T getByPlayer(Player player) {
        return this.playerTypeMap.getOrDefault(player.getUniqueId(), null);
    }

    public T getByUuid(UUID uuid) {
        return this.playerTypeMap.getOrDefault(uuid, null);
    }

    public T getByName(String name) {
        return this.playerTypeMap.getOrDefault(Bukkit.getPlayer(name).getUniqueId(), null);
    }
}
