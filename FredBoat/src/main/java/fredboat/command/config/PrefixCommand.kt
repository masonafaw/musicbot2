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

package fredboat.command.config

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import fredboat.commandmeta.abs.Command
import fredboat.commandmeta.abs.CommandContext
import fredboat.commandmeta.abs.IConfigCommand
import fredboat.db.transfer.Prefix
import fredboat.definitions.PermissionLevel
import fredboat.main.Launcher
import fredboat.messaging.internal.Context
import fredboat.perms.PermsUtil
import fredboat.sentinel.Guild
import fredboat.util.rest.CacheUtil
import io.prometheus.client.guava.cache.CacheMetricsCollector
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Created by napster on 19.10.17.
 */
class PrefixCommand(cacheMetrics: CacheMetricsCollector, name: String, vararg aliases: String) : Command(name, *aliases), IConfigCommand {

    init {
        cacheMetrics.addCache("customPrefixes", CUSTOM_PREFIXES)
    }

    companion object {
        val botId = Launcher.botController.sentinel.selfUser.id
        val CUSTOM_PREFIXES = CacheBuilder.newBuilder()
                //it is fine to check the db for updates occasionally, as we currently dont have any use case where we change
                //the value saved there through other means. in case we add such a thing (like a dashboard), consider lowering
                //the refresh value to have the changes reflect faster in the bot, or consider implementing a FredBoat wide
                //Listen/Notify system for changes to in memory cached values backed by the db
                .recordStats()
                .refreshAfterWrite(1, TimeUnit.MINUTES) //NOTE: never use refreshing without async reloading, because Guavas cache uses the thread calling it to do cleanup tasks (including refreshing)
                .expireAfterAccess(1, TimeUnit.MINUTES) //evict inactive guilds
                .concurrencyLevel(Launcher.botController.appConfig.shardCount)  //each shard has a thread (main JDA thread) accessing this cache many times
                .build(CacheLoader.asyncReloading(CacheLoader.from<Long, Optional<String>> {
                    guildId -> Launcher.botController.prefixService.getPrefix(Prefix.GuildBotId(
                        guildId!!,
                        botId
                ))
                },
                        Launcher.botController.executor))!!

        fun giefPrefix(guildId: Long) = CacheUtil.getUncheckedUnwrapped(CUSTOM_PREFIXES, guildId)
                .orElse(Launcher.botController.appConfig.prefix)

        fun giefPrefix(guild: Guild) = giefPrefix(guild.id)

        fun showPrefix(context: Context, prefix: String) {
            val p = if (prefix.isEmpty()) "No Prefix" else prefix
            context.reply(context.i18nFormat("prefixGuild", "``$p``")
                    + "\n" + context.i18n("prefixShowAgain"))
        }
    }

    override suspend fun invoke(context: CommandContext) {

        if (context.rawArgs.isEmpty()) {
            showPrefix(context, context.prefix)
            return
        }

        if (!PermsUtil.checkPermsWithFeedback(PermissionLevel.ADMIN, context)) {
            return
        }

        val newPrefix: String?
        if (context.rawArgs.equals("no_prefix", ignoreCase = true)) {
            newPrefix = "" //allow users to set an empty prefix with a special keyword
        } else if (context.rawArgs.equals("reset", ignoreCase = true)) {
            newPrefix = null
        } else {
            //considering this is an admin level command, we can allow users to do whatever they want with their guild
            // prefix, so no checks are necessary here
            newPrefix = context.rawArgs
        }

        Launcher.botController.prefixService.transformPrefix(context.guild, {
            prefixEntity -> prefixEntity.setPrefix(newPrefix)
        })

        //we could do a put instead of invalidate here and probably safe one lookup, but that undermines the database
        // as being the single source of truth for prefixes
        CUSTOM_PREFIXES.invalidate(context.guild.id)

        showPrefix(context, giefPrefix(context.guild))
    }

    override fun help(context: Context): String {
        return "{0}{1} <prefix> OR {0}{1} no_prefix OR {0}{1} reset\n#" + context.i18n("helpPrefixCommand")
    }
}
