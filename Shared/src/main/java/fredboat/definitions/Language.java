/*
 * MIT License
 *
 * Copyright (c) 2017-2018 Frederik Ar. Mikkelsen
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

package fredboat.definitions;

import fredboat.util.IgnoreCaseStringList;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Created by napster on 21.03.18.
 */
public enum Language {
    AF_ZA("af", "ZA", "Afrikaans", "Afrikaans"),
    AR_SA("ar", "SA", "ﺔﻴﺐﺮﻌﻠﺍ", "Arabic"),
    BG_BG("bg", "BG", "български език", "Bulgarian"),
    CA_ES("ca", "ES", "Catalan", "Catalan"),
    CS_CZ("cs", "CZ", "Čeština", "Czech"),
    CY_GB("cy", "GB", "Cymraeg", "Welsh"),
    DA_DK("da", "DK", "Dansk", "Danish", "klaphat"),
    DE_DE("de", "DE", "Deutsch", "German", "bratwurst", "lederhosen"),
    EL_GR("el", "GR", "ελληνικά", "Greek"),
    EN_PT("en", "PT", "Pirate", "Pirate English", "arrr"),
    EN_TS("en", "TS", "Tsundere", "Tsundere English", "baka"),
    EN_US("en", "US", "English", "English"),
    ES_ES("es", "ES", "Español", "Spanish"),
    ET_EE("et", "EE", "Eesti", "Estonian"),
    FIL_PH("fil", "PH", "Filipino", "Filipino"),
    FI_FI("fi", "FI", "Suomi", "Finnish"),
    FR_FR("fr", "FR", "Français", "French", "baguette"),
    HE_IL("he", "IL", "עברית", "Hebrew"),
    HR_HR("hr", "HR", "Hrvatski", "Croatian"),
    HU_HU("hu", "HU", "magyar", "Hungarian"),
    ID_ID("id", "ID", "Bahasa Indonesia", "Indonesian"),
    IT_IT("it", "IT", "Italiano", "Italian"),
    JA_JP("ja", "JP", "日本語", "Japanese"),
    KO_KR("ko", "KR", "한국어", "Korean"),
    MS_MY("ms", "MY", "Bahasa Melayu", "Malay"),
    NL_NL("nl", "NL", "Nederlands", "Dutch"),
    NO_NO("no", "NO", "Norsk", "Norwegian"),
    PL_PL("pl", "PL", "Polski", "Polish"),
    PT_BR("pt", "BR", "Português brasileiro", "Brazilian Portuguese", "Brazilian", "huehuehue"),
    PT_PT("pt", "PT", "Português", "Portuguese"),
    RO_RO("ro", "RO", "Română", "Romanian"),
    RU_RU("ru", "RU", "Русский", "Russian", "cyka blyat", "cheeki breeki", "rush b"),
    SK_SK("sk", "SK", "Slovenský jazyk", "Slovak"),
    SR_SP("sr", "SP", "српски", "Serbian (Cyrillic)"),
    SV_SE("sv", "SE", "Svenska", "Swedish"),
    TH_TH("th", "TH", "ไทย", "Thai"),
    TR_TR("tr", "TR", "Türkçe", "Turkish"),
    UK_UA("uk", "UA", "українська мова", "Ukrainian"),
    VI_VN("vi", "VN", "Tiếng Việt", "Vietnamese"),
    ZH_CN("zh", "CN", "简体中文", "Chinese Simplified"),
    ZH_TW("zh", "TW", "繁體中文", "Chinese Traditional"),
    ;

    private final String code;
    private final Locale locale;
    private final String nativeName;
    private final String englishName;
    private final List<String> other; //other possible identifications / names for that lang (memes go here)

    Language(String language, String country, String nativeName, String englishName, String... other) {

        this.code = language + "_" + country;
        this.locale = new Locale(language, country);
        this.nativeName = nativeName;
        this.englishName = englishName;
        this.other = new IgnoreCaseStringList(Arrays.asList(other));
    }

    public String getCode() {
        return code;
    }

    public Locale getLocale() {
        return locale;
    }

    public String getNativeName() {
        return nativeName;
    }

    public String getEnglishName() {
        return englishName;
    }

    /**
     * This method tries to parse an input into a language that we recognize.
     * It will try to make use of the language code, the native language name, and the english name of a language to
     * match the input to a known language.
     *
     * @param input input to be parsed into a Language known to us (= defined in this enum)
     * @return the optional language identified from the input.
     */
    public static Optional<Language> parse(String input) {
        for (Language language : Language.values()) {
            if (language.name().equalsIgnoreCase(input)
                    || language.getCode().equalsIgnoreCase(input)
                    || language.getNativeName().equalsIgnoreCase(input)
                    || language.getEnglishName().equalsIgnoreCase(input)
                    || language.other.contains(input)) {
                return Optional.of(language);
            }
        }

        return Optional.empty();
    }
}
