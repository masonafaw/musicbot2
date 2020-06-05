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

package fredboat.db.transfer;

import fredboat.sentinel.Guild;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Created by napster on 20.03.18.
 * <p>
 * Transfer object for the {@link fredboat.db.entity.main.Prefix}
 */
//todo move ""business"" logic to the backend
public class Prefix implements TransferObject<Prefix.GuildBotId> {

    private GuildBotId id;
    @Nullable
    private String prefix;

    public Prefix(GuildBotId id, @Nullable String prefix) {
        this.id = id;
        this.prefix = prefix;
    }

    @Override
    public void setId(GuildBotId id) {
        this.id = id;
    }

    @Override
    public GuildBotId getId() {
        return this.id;
    }

    @Nullable
    public String getPrefix() {
        return this.prefix;
    }

    @CheckReturnValue
    public Prefix setPrefix(@Nullable String prefix) {
        this.prefix = prefix;
        return this;
    }

    public static class GuildBotId implements Serializable {
        private static final long serialVersionUID = -7996104189406039666L;
        private long guildId;
        private long botId;

        public GuildBotId(Guild guild, long botId) {
            this(guild.getId(), botId);
        }

        public GuildBotId(long guildId, long botId) {
            this.guildId = guildId;
            this.botId = botId;
        }

        public long getGuildId() {
            return guildId;
        }

        public void setGuildId(long guildId) {
            this.guildId = guildId;
        }

        public long getBotId() {
            return botId;
        }

        public void setBotId(long botId) {
            this.botId = botId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(guildId, botId);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof GuildBotId)) return false;
            GuildBotId other = (GuildBotId) o;
            return this.guildId == other.guildId && this.botId == other.botId;
        }


        @Override
        public String toString() {
            return GuildBotId.class.getSimpleName() + String.format("(G %s, B %s)", guildId, botId);
        }
    }
}
