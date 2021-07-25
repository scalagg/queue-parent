package com.solexgames.queue.command;

import com.solexgames.lib.acf.BaseCommand;
import com.solexgames.lib.acf.annotation.CommandAlias;
import com.solexgames.lib.acf.annotation.CommandPermission;

/**
 * @author GrowlyX
 * @since 7/25/2021
 */

@CommandAlias("leavequeue|leaveq")
@CommandPermission("queue.command.settings")
public class LeaveQueueCommand extends BaseCommand {
}
