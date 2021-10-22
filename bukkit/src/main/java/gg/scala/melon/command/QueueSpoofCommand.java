package gg.scala.melon.command;

import com.solexgames.lib.acf.BaseCommand;
import com.solexgames.lib.acf.CommandHelp;
import com.solexgames.lib.acf.annotation.*;

/**
 * @author GrowlyX
 * @since 7/29/2021
 */

@CommandAlias("queuespoof|qspoof")
@CommandPermission("queue.command.spoof")
public class QueueSpoofCommand extends BaseCommand {

    @Default
    @HelpCommand
    @Syntax("[page]")
    public void onHelp(CommandHelp help) {
        help.showHelp();
    }


}
