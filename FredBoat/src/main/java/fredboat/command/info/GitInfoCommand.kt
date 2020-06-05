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
 */

package fredboat.command.info

import com.fredboat.sentinel.entities.embed
import com.fredboat.sentinel.entities.field
import com.fredboat.sentinel.entities.footer
import fredboat.command.`fun`.img.RandomImageCommand
import fredboat.commandmeta.abs.Command
import fredboat.commandmeta.abs.CommandContext
import fredboat.commandmeta.abs.IInfoCommand
import fredboat.messaging.internal.Context
import fredboat.shared.constant.BotConstants
import fredboat.util.GitRepoState
import java.awt.Color
import java.util.regex.Pattern

/**
 * Created by napster on 05.05.17.
 *
 * Display some git related information
 */
class GitInfoCommand(name: String, vararg aliases: String) : Command(name, *aliases), IInfoCommand {

    companion object {
        //https://regex101.com/r/wqfWBI/6/tests
        private val GITHUB_URL_PATTERN = Pattern.compile("^(git@|https?://)(.+)[:/](.+)/(.+)(\\.git)?$")
    }

    private val octocats = RandomImageCommand("https://imgur.com/a/sBkTj", "")

    // gitRepoState.remoteOriginUrl; FIXME unhardcode this. probably requires some gradle/groovy magic or a PR to the git info plugin were using
    private val githubCommitLink: String
        get() {
            var result = "Could not find or create a valid Github url."
            val gitRepoState = GitRepoState.getGitRepositoryState()
            if (gitRepoState != null) {
                val originUrl = BotConstants.GITHUB_URL

                val m = GITHUB_URL_PATTERN.matcher(originUrl)

                if (m.find()) {
                    val domain = m.group(2)
                    val user = m.group(3)
                    val repo = m.group(4)
                    val commitId = gitRepoState.commitId

                    result = "https://$domain/$user/$repo/commit/$commitId"
                }
            }
            return result
        }

    override suspend fun invoke(context: CommandContext) {
        val gitRepoState = GitRepoState.getGitRepositoryState()
        if (gitRepoState == null) {
            context.replyWithName("This build has does not contain any git meta information")
            return
        }

        val url = githubCommitLink

        val embed = embed {
            title = "Git info of this build"
            color = Color(240, 81, 51).rgb //git-scm color
            thumbnail = octocats.randomImageUrl

            field("Commit info", gitRepoState.commitMessageFull, false)
            field("Commit on Github", url, false)
            field("Commit timestamp", gitRepoState.commitTime, true)
            field("Branch", gitRepoState.branch, true)
            field("Committed by", gitRepoState.commitUserName, true)
            footer {
                text = "Built on"
                iconUrl = "http://i.imgur.com/RjWwxlg.png"
            }
        }

        context.reply(embed)
    }

    override fun help(context: Context): String {
        return "{0}{1}\n#Display some git meta information about the running FredBoat build."
    }
}
