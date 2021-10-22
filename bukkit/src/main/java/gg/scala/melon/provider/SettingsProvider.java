package gg.scala.melon.provider;

import com.solexgames.lib.commons.redis.JedisSettings;

/**
 * @author GrowlyX
 * @since 7/26/2021
 * <p>
 * To prevent people who use scandium from having to add redundant
 * values into their config when it's already in Scandium.
 */

public interface SettingsProvider {

    String getServerName();

    JedisSettings getJedisSettings();

    boolean canJoin();

}
