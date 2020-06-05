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

package fredboat.command.music.control;

import fredboat.audio.player.GuildPlayer;
import fredboat.audio.player.PlayerRegistry;
import fredboat.commandmeta.MessagingException;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.ICommandRestricted;
import fredboat.commandmeta.abs.IMusicCommand;
import fredboat.commandmeta.abs.JCommand;
import fredboat.definitions.PermissionLevel;
import fredboat.messaging.internal.Context;
import fredboat.shared.constant.BotConstants;

import javax.annotation.Nonnull;
import java.time.Duration;

import static fredboat.main.LauncherKt.getBotController;

public class VolumeCommand extends JCommand implements IMusicCommand, ICommandRestricted {

    public VolumeCommand(String name, String... aliases) {
        super(name, aliases);
    }

    @Override
    public void onInvoke(@Nonnull CommandContext context) {

        if (getBotController().getAppConfig().getDistribution().volumeSupported()) {

            GuildPlayer player = getBotController().getPlayerRegistry().getOrCreate(context.getGuild());
            try {
                float volume = Float.parseFloat(context.getArgs()[0]) / 100;
                volume = Math.max(0, Math.min(1.5f, volume));

                context.reply(context.i18nFormat("volumeSuccess",
                        Math.floor(player.getVolume() * 100), Math.floor(volume * 100)));

                player.setVolume(volume);
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException ex) {
                throw new MessagingException(context.i18nFormat("volumeSyntax",
                        100 * PlayerRegistry.DEFAULT_VOLUME, Math.floor(player.getVolume() * 100)));
            }
        } else {
            String out = context.i18n("volumeApology") + "\n<" + BotConstants.DOCS_DONATE_URL + ">";
            context.replyImageMono("https://fred.moe/1vD.png", out)
                    .subscribe(
                            (msg) -> context.getTextChannel().deleteMessage(msg.getMessageId())
                                            .delaySubscription(Duration.ofMinutes(2))
                                            .subscribe()
                    );
        }
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1} <0-150>\n#" + context.i18n("helpVolumeCommand");
    }

    @Nonnull
    @Override
    public PermissionLevel getMinimumPerms() {
        return PermissionLevel.DJ;
    }
}
