package com.envarcade.brennon.core.command.impl

import com.envarcade.brennon.common.util.TextUtil
import com.envarcade.brennon.core.Brennon
import com.envarcade.brennon.core.command.BrennonCommand
import com.envarcade.brennon.core.command.BrennonCommandSender

/**
 * /link — Generate a code to link your MC account to the web dashboard.
 */
class LinkCommand(
    private val brennon: Brennon
) : BrennonCommand(
    name = "link",
    permission = "brennon.command.link",
    usage = "/link",
    description = "Link your Minecraft account to the web dashboard"
) {
    override fun execute(sender: BrennonCommandSender, args: Array<String>) {
        if (!sender.isPlayer) {
            sender.sendMessage(TextUtil.error("Only players can link their account."))
            return
        }

        val code = brennon.playerAuthManager.generateLinkCode(sender.uuid!!, sender.name)
        sender.sendMessage(TextUtil.prefixed("Your web dashboard link code:"))
        sender.sendMessage(TextUtil.parse("  <gold><bold>$code</bold></gold>"))
        sender.sendMessage(TextUtil.parse("  <gray>Enter this code on the web dashboard to link your account."))
        sender.sendMessage(TextUtil.parse("  <gray>This code expires in <yellow>5 minutes</yellow>."))
    }
}
