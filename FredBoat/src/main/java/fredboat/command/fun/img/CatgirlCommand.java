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

package fredboat.command.fun.img;

import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.IFunCommand;
import fredboat.commandmeta.abs.JCommand;
import fredboat.main.BotController;
import fredboat.messaging.internal.Context;

import javax.annotation.Nonnull;
import java.io.IOException;

import static fredboat.main.LauncherKt.getBotController;

public class CatgirlCommand extends JCommand implements IFunCommand {

    private static final String BASE_URL = "https://nekos.life/api/neko";

    public CatgirlCommand(String name, String... aliases) {
        super(name, aliases);
    }

    @Override
    public void onInvoke(@Nonnull CommandContext context) {
        context.sendTyping();
        getBotController().getExecutor().submit(() -> postCatgirl(context));

        // TODO: Post lewd catgirls from the API in NSFW chat
    }

    private void postCatgirl(CommandContext context) {

        try {
            String nekoUrl = BotController.Companion.getHTTP().get(BASE_URL).asJson().getString("neko");
            context.replyImage(nekoUrl, "");
        } catch (IOException e) {
            context.reply(context.i18nFormat("catgirlFail", BASE_URL));
        }
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1}\n#Post a catgirl pic.";
    }
}
