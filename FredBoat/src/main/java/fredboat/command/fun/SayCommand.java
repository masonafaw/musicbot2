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
package fredboat.command.fun;

import fredboat.command.info.HelpCommand;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.IFunCommand;
import fredboat.commandmeta.abs.JCommand;
import fredboat.event.MessageEventHandler;
import fredboat.messaging.internal.Context;
import fredboat.util.TextUtils;

import javax.annotation.Nonnull;

/**
 * @author frederik
 */
public class SayCommand extends JCommand implements IFunCommand {

    public SayCommand(String name, String... aliases) {
        super(name, aliases);
    }

    @Override
    public void onInvoke(@Nonnull CommandContext context) {
        if (!context.hasArguments()) {
            HelpCommand.sendFormattedCommandHelp(context);
            return;
        }

        String out = TextUtils.escapeAndDefuse(context.getRawArgs());

        context.replyMono(TextUtils.ZERO_WIDTH_CHAR + out).subscribe(
                message -> MessageEventHandler.Companion.getMessagesToDeleteIfIdDeleted()
                        .put(context.getMsg().getId(), message.getMessageId())
        );

    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1} <text>\n#" + context.i18n("helpSayCommand");
    }
}
