package gg.scala.melon.command;

import net.evilblock.cubed.acf.BaseCommand;
import net.evilblock.cubed.acf.CommandHelp;
import net.evilblock.cubed.acf.annotation.*;

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
