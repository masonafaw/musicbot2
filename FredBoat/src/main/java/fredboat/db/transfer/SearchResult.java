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

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.io.MessageInput;
import com.sedmelluq.discord.lavaplayer.tools.io.MessageOutput;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist;
import fredboat.definitions.SearchProvider;
import org.apache.commons.lang3.SerializationUtils;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by napster on 20.03.18.
 * <p>
 * Transfer object for the {@link fredboat.db.entity.main.GuildConfig}
 */
//todo move ""business"" logic to the backend
public class SearchResult implements TransferObject<SearchResult.SearchResultId> {

    private SearchResultId searchResultId;
    private long timestamp;
    private byte[] serializedSearchResult;

    @Override
    public void setId(SearchResultId id) {
        this.searchResultId = id;
    }

    public SearchResult(AudioPlayerManager playerManager, SearchProvider provider, String searchTerm,
                        AudioPlaylist searchResult) {
        this.searchResultId = new SearchResultId(provider, searchTerm);
        this.timestamp = System.currentTimeMillis();
        this.serializedSearchResult = SerializationUtils.serialize(new SearchResult.SerializableAudioPlaylist(playerManager, searchResult));
    }

    @Override
    public SearchResultId getId() {
        return searchResultId;
    }

    public SearchProvider getProvider() {
        return searchResultId.getProvider();
    }

    public void setProvider(SearchProvider provider) {
        searchResultId.provider = provider.name();
    }

    public String getSearchTerm() {
        return searchResultId.searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchResultId.searchTerm = searchTerm;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public AudioPlaylist getSearchResult(AudioPlayerManager playerManager) {
        SerializableAudioPlaylist sap = SerializationUtils.deserialize(serializedSearchResult);
        return sap.decode(playerManager);
    }

    public void setSearchResult(AudioPlayerManager playerManager, AudioPlaylist searchResult) {
        this.serializedSearchResult = SerializationUtils.serialize(new SerializableAudioPlaylist(playerManager, searchResult));
    }

    /**
     * Composite primary key for SearchResults
     */
    public static class SearchResultId implements Serializable {

        private static final long serialVersionUID = 8969973651938173208L;

        private String provider;

        private String searchTerm;

        public SearchResultId(SearchProvider provider, String searchTerm) {
            this.provider = provider.name();
            this.searchTerm = searchTerm;
        }

        public SearchProvider getProvider() {
            return SearchProvider.valueOf(provider);
        }

        public void setProvider(SearchProvider provider) {
            this.provider = provider.name();
        }

        public String getSearchTerm() {
            return searchTerm;
        }

        public void setSearchTerm(String searchTerm) {
            this.searchTerm = searchTerm;
        }

        @Override
        public int hashCode() {
            return Objects.hash(provider, searchTerm);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SearchResultId)) return false;
            SearchResultId other = (SearchResultId) o;
            return provider.equals(other.provider) && searchTerm.equals(other.searchTerm);
        }

        @Override
        public String toString() {
            return "Search: Provider " + provider + " Term " + searchTerm;
        }
    }


    private static class SerializableAudioPlaylist implements Serializable {
        private static final long serialVersionUID = -6823555858689776338L;

        @Nullable
        private String name;
        @SuppressWarnings("NullableProblems") //triggered by the empty no params constructor
        private byte[][] tracks;
        @Nullable
        private byte[] selectedTrack;
        private boolean isSearchResult;

        //required for deserialization
        @SuppressWarnings("unused")
        SerializableAudioPlaylist() {
        }

        public SerializableAudioPlaylist(AudioPlayerManager playerManager, AudioPlaylist audioPlaylist) {
            this.name = audioPlaylist.getName();
            this.tracks = encodeTracks(playerManager, audioPlaylist.getTracks());
            this.selectedTrack = encodeTrack(playerManager, audioPlaylist.getSelectedTrack());
            this.isSearchResult = audioPlaylist.isSearchResult();
        }

        public AudioPlaylist decode(AudioPlayerManager playerManager) {
            return new BasicAudioPlaylist(name,
                    decodeTracks(playerManager, tracks),
                    decodeTrack(playerManager, selectedTrack),
                    isSearchResult);
        }

        private static byte[][] encodeTracks(AudioPlayerManager playerManager, List<AudioTrack> tracks) {
            byte[][] encoded = new byte[tracks.size()][];
            int skipped = 0;
            for (int i = 0; i < tracks.size(); i++) {
                encoded[i] = encodeTrack(playerManager, tracks.get(i));
                if (encoded[i] == null) {
                    skipped++;
                }
            }

            byte[][] result = new byte[tracks.size() - skipped][];
            int i = 0;
            for (byte[] encodedTrack : encoded) {
                if (encodedTrack != null) {
                    result[i] = encodedTrack;
                    i++;
                }
            }

            return result;
        }

        //may return null if the encoding fails or the input is null
        @Nullable
        private static byte[] encodeTrack(AudioPlayerManager playerManager, @Nullable AudioTrack track) {
            if (track == null) {
                return null;
            }
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                playerManager.encodeTrack(new MessageOutput(baos), track);
                return baos.toByteArray();
            } catch (IOException ignored) {
                return null;
            }
        }

        private static List<AudioTrack> decodeTracks(AudioPlayerManager playerManager, @Nullable byte[][] input) {
            List<AudioTrack> result = new ArrayList<>();
            if (input == null) return result;

            for (byte[] track : input) {
                AudioTrack decoded = decodeTrack(playerManager, track);
                if (decoded != null) {
                    result.add(decoded);
                }
            }
            return result;
        }

        //may return null if the decoding fails or the input is null
        @Nullable
        private static AudioTrack decodeTrack(AudioPlayerManager playerManager, @Nullable byte[] input) {
            if (input == null) return null;
            ByteArrayInputStream bais = new ByteArrayInputStream(input);
            try {
                return playerManager.decodeTrack(new MessageInput(bais)).decodedTrack;
            } catch (IOException e) {
                return null;
            }
        }
    }
}
