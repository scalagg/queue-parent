package com.solexgames.queue.provider.impl;

import com.solexgames.core.CorePlugin;
import com.solexgames.lib.commons.redis.JedisSettings;
import com.solexgames.queue.provider.SettingsProvider;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;

/**
 * @author GrowlyX
 * @since 7/26/2021
 */

@RequiredArgsConstructor
public class DefaultSettingsProvider implements SettingsProvider {

    private final Configuration configuration;

    @Override
    public String getServerName() {
        return this.configuration.getString("server-id");
    }

    @Override
    public JedisSettings getJedisSettings() {
        return new JedisSettings(
                this.configuration.getString("redis.host"),
                this.configuration.getInt("redis.port"),
                this.configuration.getBoolean("redis.authentication.enabled"),
                this.configuration.getString("redis.authentication.password")
        );
    }

    @Override
    public boolean canJoin() {
        return Bukkit.hasWhitelist();
    }
}
