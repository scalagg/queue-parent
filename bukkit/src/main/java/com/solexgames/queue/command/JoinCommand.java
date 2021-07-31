package com.solexgames.queue.command;

import com.solexgames.lib.acf.BaseCommand;
import com.solexgames.lib.acf.annotation.CommandAlias;
import com.solexgames.lib.acf.annotation.Default;
import org.bukkit.entity.Player;

/**
 * @author GrowlyX
 * @since 7/30/2021
 */

@CommandAlias("join")
public class JoinCommand extends BaseCommand {

    @Default
    public void onDefault(Player player, String serve) {

    }
}
