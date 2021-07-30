package com.solexgames.queue.provider.impl;

import com.solexgames.core.CorePlugin;
import com.solexgames.lib.commons.redis.JedisSettings;
import com.solexgames.queue.provider.SettingsProvider;

/**
 * @author GrowlyX
 * @since 7/26/2021
 */

public class ScandiumSettingsProvider implements SettingsProvider {

    @Override
    public String getServerName() {
        return CorePlugin.getInstance().getServerName();
    }

    @Override
    public JedisSettings getJedisSettings() {
        return CorePlugin.getInstance().getDefaultJedisSettings();
    }

    @Override
    public boolean canJoin() {
        return CorePlugin.getInstance().getServerSettings().isCanJoin();
    }
}
