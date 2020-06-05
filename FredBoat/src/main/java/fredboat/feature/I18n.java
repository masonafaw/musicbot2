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

package fredboat.feature;

import fredboat.db.DatabaseNotReadyException;
import fredboat.definitions.Language;
import fredboat.sentinel.Guild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import static fredboat.main.LauncherKt.getBotController;

public class I18n {

    private static final Logger log = LoggerFactory.getLogger(I18n.class);

    public static FredBoatLocale DEFAULT = new FredBoatLocale(Language.EN_US);
    public static final HashMap<String, FredBoatLocale> LANGS = new HashMap<>();

    public static void start() {
        for (Language language : Language.values()) {
            LANGS.put(language.getCode(), new FredBoatLocale(language));
        }
        log.info("Loaded " + LANGS.size() + " languages: " + LANGS);
    }

    @Nonnull
    public static ResourceBundle get(@Nullable Guild guild) {
        if (guild == null) return DEFAULT.getProps();
        return get(guild.getId());
    }

    @Nonnull
    public static ResourceBundle get(long guild) {
        return getLocale(guild).getProps();
    }

    @Nonnull
    public static FredBoatLocale getLocale(@Nonnull Guild guild) {
        return getLocale(guild.getId());
    }

    @Nonnull
    public static FredBoatLocale getLocale(long guild) {
        try {
            return LANGS.getOrDefault(getBotController().getGuildConfigService().fetchGuildConfig(guild).getLang(), DEFAULT);
        } catch (DatabaseNotReadyException e) {
            //don't log spam the full exceptions or logs
            return DEFAULT;
        } catch (Exception e) {
            log.error("Error when reading entity", e);
            return DEFAULT;
        }
    }

    public static void set(@Nonnull Guild guild, @Nonnull String lang) throws LanguageNotSupportedException {
        if (!LANGS.containsKey(lang))
            throw new LanguageNotSupportedException("Language not found");

        getBotController().getGuildConfigService().transformGuildConfig(guild.getId(), config -> config.setLang(lang));
    }

    public static class FredBoatLocale {

        private final Language language;
        private final ResourceBundle props;

        FredBoatLocale(Language language) throws MissingResourceException {
            this.language = language;
            props = ResourceBundle.getBundle("lang." + language.getCode(), language.getLocale());
        }

        public ResourceBundle getProps() {
            return props;
        }

        public String getCode() {
            return language.getCode();
        }

        public String getNativeName() {
            return language.getNativeName();
        }

        public String getEnglishName() {
            return language.getEnglishName();
        }

        @Override
        public String toString() {
            return "[" + getCode() + " " + getNativeName() + "]";
        }
    }

    public static class LanguageNotSupportedException extends Exception {
        public LanguageNotSupportedException(String message) {
            super(message);
        }
    }

}
