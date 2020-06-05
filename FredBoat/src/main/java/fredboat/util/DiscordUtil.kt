/*
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
 *
 */

package fredboat.util

import fredboat.config.property.AppConfig
import fredboat.sentinel.Member
import fredboat.shared.constant.BotConstants
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

object DiscordUtil {

    fun getHighestRolePosition(member: Member): Mono<Int> {
        if (member.roles.isEmpty()) return Mono.just(-1) // @ everyone role

        return Mono.create { sink ->
            Flux.merge(member.roles.map { it.info })
                    .reduce { a, b ->
                        return@reduce if (a.position > b.position) a else b
                    }
                    .doOnError { sink.error(it) }
                    .subscribe { sink.success(it.position) }
        }
    }

    /**
     * @return true if this bot account is an "official" fredboat (music, patron, CE, etc).
     * This is useful to lock down features that we only need internally, like polling the docker hub for pull stats.
     */
    fun isOfficialBot(botId: Long): Boolean {
        return (botId == BotConstants.MUSIC_BOT_ID
                || botId == BotConstants.PATRON_BOT_ID
                || botId == BotConstants.CUTTING_EDGE_BOT_ID
                || botId == BotConstants.BETA_BOT_ID
                || botId == BotConstants.MAIN_BOT_ID)
    }

    //https://discordapp.com/developers/docs/topics/gateway#sharding
    fun getShardId(guildId: Long, appConfig: AppConfig) = ((guildId shr 22) % appConfig.shardCount).toInt()
}
