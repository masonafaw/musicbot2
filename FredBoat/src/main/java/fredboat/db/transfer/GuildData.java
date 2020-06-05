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

import javax.annotation.CheckReturnValue;

/**
 * Created by napster on 20.03.18.
 * <p>
 * Transfer object for the {@link fredboat.db.entity.main.GuildData}
 */
//todo move ""business"" logic to the backend
public class GuildData implements TransferObject<Long> {

    private long guildId;
    private long timestampHelloSent;

    @Override
    public void setId(Long guildId) {
        this.guildId = guildId;
    }

    @Override
    public Long getId() {
        return guildId;
    }

    public long getTimestampHelloSent() {
        return timestampHelloSent;
    }

    @CheckReturnValue
    public GuildData helloSent() {
        this.timestampHelloSent = System.currentTimeMillis();
        return this;
    }
}
