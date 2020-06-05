/*
 *
 * MIT License
 *
 * Copyright (c) 2017 Frederik Ar. Mikkelsen
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package fredboat.command.admin

import com.fredboat.sentinel.entities.coloredEmbed
import com.fredboat.sentinel.entities.field
import fredboat.commandmeta.abs.Command
import fredboat.commandmeta.abs.CommandContext
import fredboat.commandmeta.abs.ICommandRestricted
import fredboat.definitions.PermissionLevel
import fredboat.messaging.internal.Context
import fredboat.perms.NO_PERMISSIONS
import fredboat.util.extension.asCodeBlock
import fredboat.util.extension.escapeMarkdown

/**
 * Created by napster on 18.01.18.
 *
 * This command is for debugging and FredBoat support staff.
 */
class DiscordPermissionCommand(name: String, vararg aliases: String) : Command(name, *aliases), ICommandRestricted {

    override val minimumPerms: PermissionLevel
        get() = PermissionLevel.BOT_ADMIN

    override fun help(context: Context): String {
        return ("{0}{1} [channel id or mention] [user id or mention]\n#Show permissions of a user and any roles they "
                + "have for a channel and guild. If no user is provided, FredBoat shows permissions for itself. If no "
                + "channel is provided, permissions for the channel where the command is issued are shown.")
    }

    override suspend fun invoke(context: CommandContext) {
        var tc = context.textChannel

        val mentionedChannels = context.msg.mentionedChannels

        if (!mentionedChannels.isEmpty()) {
            tc = mentionedChannels[0]
        } else if (context.hasArguments()) {
            try {
                val channelId = context.args[0].toLong()
                // TODO: Make this work with channels outside this guild
                val textChannel = context.guild.getTextChannel(channelId)
                if (textChannel == null) {
                    context.reply(String.format("No text channel with id `%s` found.", channelId))
                    return
                }
                tc = textChannel
            } catch (ignored: NumberFormatException) {
            }

        }
        val guild = tc.guild


        var member = guild.selfMember
        if (!context.mentionedMembers.isEmpty()) {
            member = context.mentionedMembers[0]
        } else if (context.args.size > 1) {
            try {
                val userId = context.args[1].toLong()
                val m = guild.getMember(userId)
                if (m == null) {
                    context.reply("No member with id `%s` found in the specified guild.")
                    return
                }
                member = m
            } catch (ignored: NumberFormatException) {
            }

        }


        //we need the following things:
        // server permissions for all roles of the member, including the @everyone role
        // category overrides for roles + @everyone
        // category override for member
        // channel overrides for roles + @everyone
        // channel override for member

        val embed = coloredEmbed {
            title = "Full allowed/denied discord permissions"
            description = "Empty roles / permission overrides omitted."
            field {
                title = "User"
                body = (member.asMention
                        + "\n" + member.id
                        + "\n" + member.effectiveName.escapeMarkdown()
                        + "\n" + member.name.escapeMarkdown())
                inline = true
            }
            field {
                title = "Guild"
                body = guild.name.escapeMarkdown() + "\n" + guild.id
                inline = true
            }
            field {
                title = "Channel"
                body = tc.name + "\n" + tc.id
                inline = true
            }

            for (role in member.roles) {
                if (role.permissions == NO_PERMISSIONS) continue

                val roleField = StringBuilder()
                role.permissions.asList().forEach {
                    roleField.append("+ ").append(it.uiName).append("\n")
                }

                field {
                    title = "Level: Server, Role: ${role.name}"
                    body = roleField.toString().asCodeBlock("diff")
                }
            }
        }

        // TODO: Add handling for overrides and categories
        /*
        val category = tc.getParent()
        if (category == null) {
            eb.addField("No parent category", "", false)
        } else {
            for (role in roles) {
                formatPermissionOverride(eb, category!!.getPermissionOverride(role))
            }
            formatPermissionOverride(eb, category!!.getPermissionOverride(member))
        }

        for (role in roles) {
            formatPermissionOverride(eb, tc.getPermissionOverride(role))
        }
        formatPermissionOverride(eb, tc.getPermissionOverride(member))*/

        context.reply(embed)
    }

    /*
    //add formatted information about a permission override to the embed builder, if there is any
    private fun formatPermissionOverride(eb: EmbedBuilder, po: PermissionOverride?) {
        if (po == null) {
            return
        }
        if (po.allowed.isEmpty() && po.denied.isEmpty()) {
            return
        }

        var name = "Level: " + if (po.channel.type == ChannelType.CATEGORY) "Category" else "Channel"
        name += ", "
        val role = po.role
        if (role != null) {
            name += "Role: " + role.name
        } else {
            name += "Self Member"
        }

        eb.addField(name, permOverrideAsDiffMarkdown(po), false)
    }

    private fun permOverrideAsDiffMarkdown(override: PermissionOverride): String {
        val sb = StringBuilder()
        for (permission in override.allowed) {
            sb.append("+ ").append(permission.name).append("\n")
        }
        for (permission in override.denied) {
            sb.append("- ").append(permission.name).append("\n")
        }
        return TextUtils.asCodeBlock(sb.toString(), "diff")
    }*/
}
