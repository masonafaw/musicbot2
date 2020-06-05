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

/**
 * Created by napster on 20.03.18.
 * <p>
 * Transfer object for the {@link fredboat.db.entity.main.BlacklistEntry}
 */
//todo move ""business"" logic to the backend
public class BlacklistEntry implements TransferObject<Long> {

    //id of the user or guild that this blacklist entry belongs to
    private long id;

    //blacklist level that the user or guild is on
    private int level = -1;

    //keeps track of how many times a user or guild reached the rate limit on the current blacklist level
    private int rateLimitReached;

    //when was the ratelimit hit the last time?
    private long rateLimitReachedTimestamp;

    //time when the id was blacklisted
    private long blacklistedTimestamp;

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    @Override
    public boolean equals(Object other) {
        return (other instanceof BlacklistEntry) && ((BlacklistEntry) other).id == this.id;
    }

    public int getLevel() {
        return level;
    }

    public void incLevel() {
        level++;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getRateLimitReached() {
        return rateLimitReached;
    }

    public void incRateLimitReached() {
        rateLimitReached++;
    }

    public void setRateLimitReached(int rateLimitReached) {
        this.rateLimitReached = rateLimitReached;
    }

    public long getRateLimitReachedTimestamp() {
        return rateLimitReachedTimestamp;
    }

    public void setRateLimitReachedTimestamp(long rateLimitReachedTimestamp) {
        this.rateLimitReachedTimestamp = rateLimitReachedTimestamp;
    }

    public long getBlacklistedTimestamp() {
        return blacklistedTimestamp;
    }

    public void setBlacklistedTimestamp(long blacklistedTimestamp) {
        this.blacklistedTimestamp = blacklistedTimestamp;
    }
}
